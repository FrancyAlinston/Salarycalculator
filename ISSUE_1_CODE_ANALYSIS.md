# Issue #1: Code Analysis & Implementation Guide

## Executive Summary
This document provides a comprehensive breakdown of Issue #1 (Shift Heatmap Bugs & Navigation Issues) with code-level analysis and implementation guidance. The app is a **Kotlin/Jetpack Compose Android application** for UK salary calculations with shift scheduling features.

---

## Architecture Overview

### Directory Structure
```
app/src/main/java/com/example/salarycalculator/
├── domain/              # Business logic & data management
│   ├── ShiftTracker.kt  # Shift record models
│   ├── PayScheduleEngine.kt  # Pay period calculations
│   ├── SalaryRepository.kt   # Data persistence layer
│   └── [40+ other domain files]
├── ui/
│   ├── calculator/      # Calculator & shift UI
│   │   ├── ShiftCalendarDialog.kt     # KEY FILE - Heatmap UI
│   │   ├── CalculatorScreen.kt        # Main calculator
│   │   └── [20+ other dialogs]
│   ├── settings/        # Settings UI
│   ├── history/         # History/ledger
│   └── main/            # Home screen
└── theme/               # Material Design theming
```

### Tech Stack
- **Language:** Kotlin 2.1.20
- **UI:** Jetpack Compose + Material 3
- **Data:** Jetpack DataStore Preferences
- **Navigation:** AndroidX Navigation 3
- **Build:** Gradle 9.0.1

---

## Issue #1: Root Cause Analysis

### 1.1 STATE PERSISTENCE BUG (Reset on Reopen)

**Current Implementation - ShiftCalendarDialog.kt (lines 59-97)**

The state is stored in a mutable map that initializes from parameters but fails to properly load persisted data:

```kotlin
val annualShifts = remember {
    mutableStateMapOf<Int, MutableMap<Int, Double>>().apply {
        for (m in 1..12) {
            put(m, mutableStateMapOf())
        }
        val currentMonthMap = get(selectedMonth)!!
        val initialCount = initialDaysWorked.toInt().coerceIn(0, 31)
        for (d in 1..initialCount) {
            currentMonthMap[d] = initialHoursPerDay
        }
    }
}

LaunchedEffect(Unit) {
    if (salaryRepository != null) {
        salaryRepository.getAnnualShiftSchedule().collect { jsonStr ->
            // Loading logic - but too late
        }
    }
}
```

**Root Causes:**
1. Timing Issue: LaunchedEffect runs AFTER initial compose
2. Initialization Order: annualShifts seeds from parameters, ignoring repository data
3. Missing Validation: Changes saved but not verified on reload
4. Repository Dependency: If salaryRepository is null, no persistence happens

**Key Files:**
- ShiftCalendarDialog.kt (lines 59-112)
- SalaryRepository.kt (getAnnualShiftSchedule/setAnnualShiftSchedule)

---

### 1.2 MISSING COLOR LEGEND & STATUS INDICATOR

**Current Implementation - ShiftCalendarDialog.kt (lines 403-409)**

Colors exist but lack explanation:
- Rose60 (Red) = 12h - Undefined
- Amber60 (Orange) = 10h - Overtime
- Emerald60 (Green) = 8h - Standard
- Teal60 (Teal) = 1-7h - Partial

**Root Causes:**
1. No Legend Component: Colors hardcoded without visible explanation
2. Red Shift Ambiguity: "12h" is undefined (Night Shift? Long Shift?)
3. No Inline Legend: No card explaining color meanings
4. Missing Status Feedback: Tap doesn't show active state

---

### 1.3 MONTH vs. YEAR NAVIGATION CONTROLS

**Current Implementation - ShiftCalendarDialog.kt (lines 227-250)**

```kotlin
IconButton(
    onClick = { selectedYear -= 1 },  // WRONG: Decrements year
) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Year")
}
Text(text = "Year $selectedYear", ...)  // Shows year, not month
IconButton(
    onClick = { selectedYear += 1 },  // WRONG: Increments year
) {
    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year")
}
```

**Root Causes:**
1. Wrong Variable: Controls modify selectedYear instead of selectedMonth
2. No Month Rollover: Doesn't handle Dec to Jan transitions
3. No Year Selector: User must click 12+ times to jump years
4. Confusing Label: Says "Year" but has month chips below

**Expected Behavior:**
- Arrows navigate month-by-month (< September 2026 >)
- At Dec: Next → Jan of following year
- At Jan: Prev → Dec of previous year
- Separate year selector for quick jumps

---

### 1.4 MONTHLY SALARY CALCULATION & ZERO-STATE

**Current Implementation - ShiftCalendarDialog.kt (lines 140-157)**

Calculations exist but lack payroll cutoff integration:

```kotlin
val monthDaysWorked = currentMonthMap.values.count { it > 0 }
val monthTotalHours = currentMonthMap.values.sum()
val monthOvertimeHours = currentMonthMap.values.sumOf { maxOf(0.0, it - 8.0) }
val annualEstimatedGross = annualTotalHours * hourlyRate
```

**Issues:**
1. No Payroll Cutoff Calculation: Annual/monthly estimates don't respect cutoff rules
2. Missing Zero-State: If month has 0 shifts, might show cached values
3. No Validation: Empty months not explicitly marked
4. No £0.00 Logic: Should display £0.00 when no shifts marked

**Related File:**
- PayScheduleEngine.kt has calculatePayPeriod() for cutoff/payday logic

---

## Implementation Phases

### Phase 1: Fix State Persistence (CRITICAL)
**Priority: HIGH | Effort: 4 hours**

**Files:** ShiftCalendarDialog.kt, SalaryRepository.kt

**Steps:**
1. Load repository data BEFORE composing initial state
2. Fix initialization order in remember block
3. Add error logging for debugging
4. Test reload after dialog close/reopen
5. Verify JSON serialization/deserialization

### Phase 2: Add Color Legend & Month Navigation (IMPORTANT)
**Priority: MEDIUM | Effort: 3 hours**

**Files:** ShiftCalendarDialog.kt

**Steps:**
1. Add legend card above calendar showing all colors
2. Fix month arrows (use selectedMonth, not selectedYear)
3. Add month rollover logic (Dec → Jan, Jan → Dec)
4. Update label to show "September 2026" not "Year 2026"
5. Optional: Add year picker for quick jumps

### Phase 3: Fix Salary Calculation Zero-State (IMPORTANT)
**Priority: MEDIUM | Effort: 2 hours**

**Files:** ShiftCalendarDialog.kt

**Steps:**
1. Display £0.00 explicitly if no shifts in month
2. Use PayScheduleEngine for payroll cutoff validation
3. Add "No shifts this month" messaging
4. Test edge cases (Feb, 30/31-day months)

### Phase 4: Home Screen Sync (IMPORTANT)
**Priority: MEDIUM | Effort: 3 hours**

**Files:** ShiftCalendarDialog.kt, CalculatorScreen.kt

**Steps:**
1. Enhance onApply callback to sync heatmap → home
2. Update Days Worked & Estimated Net on dialog apply
3. Load current month's data on app launch
4. Add navigation: home screen → heatmap dialog

### Phase 5: Settings Integration (NICE-TO-HAVE)
**Priority: LOW | Effort: 2 hours**

**Files:** SettingsScreen.kt, SalaryRepository.kt

**Steps:**
1. Add "Default Working Hours" setting (1-12h)
2. Persist to DataStore
3. Use in heatmap initialization
4. Add validation

### Phase 6: Sandbox Calculator (NICE-TO-HAVE)
**Priority: LOW | Effort: 5 hours**

**Files:** New SandboxCalculatorDialog.kt

**Steps:**
1. Create isolated "What-If" calculator UI
2. Separate OT and Bank Holiday multipliers
3. Independent from saved heatmap data
4. Display as "What-if" scenario only

---

## Testing Checklist

- [ ] Mark days → Close → Reopen → Data persists
- [ ] Month arrows step 1-12 sequentially
- [ ] Dec Next → Jan (next year)
- [ ] Color legend visible and accurate
- [ ] No shifts = displays £0.00
- [ ] Home screen updates after heatmap apply
- [ ] App restart loads current month data

---

## Next Steps on Beta Branch

1. Confirm you're on Beta branch (capital B)
2. Create feature branch: fix/state-persistence
3. Start with Phase 1 (state persistence)
4. Add unit tests for each phase
5. Test on Android device/emulator
6. Create pull requests linked to Issue #1
