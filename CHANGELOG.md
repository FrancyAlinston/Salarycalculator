# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### What Needs to Be Fixed / Upcoming
- [ ] Direct PDF vector export with custom company/employee header formatting.
- [ ] CSV timesheet log file generation and saving.
- [ ] Side-by-side historical pay period comparison diff tool.

---

## [2.1] - 2026-09-03 (VersionCode: 6)
### Added
- **Persistent Monthly Salary History**: Full snapshot persistence of monthly payslips (Days, Hours, Overtime, Wage, Pension, Tax, NI, Student Loans, Net Take-Home, and Custom Notes) using Jetpack DataStore Preferences and `kotlinx.serialization`.
- **Dedicated History Screen (`HistoryScreen.kt`)**: Added 3rd navigation destination featuring cumulative earnings statistics (Total Take-Home, Avg Monthly Net, Total Gross, Total Tax, Total NI) and a chronological list of monthly records.
- **Expandable Payslip Cards with Mini Distribution Bars**: Visual breakdown bars and expandable itemized deductions for every historical salary record.
- **Save Record Flow on Calculator**: 1-tap "Save Record" action on `CalculatorScreen.kt` with a month/year suggestion chip picker and custom note input.
- **Record Management & Sharing**: Per-record deletion, clear-all action, and 1-tap sharing of historical payslip summaries via Android Sharesheet.
- **Unit Test for Serialization**: Added `monthlySalaryRecord_serialization_isLossless` in [`TaxCalculatorTest.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/test/java/com/example/salarycalculator/domain/TaxCalculatorTest.kt).

### Bugs Found & Fixed
- **Multi-Period Layout Overflow on 320px Screens**: Optimized font scaling and padding in `PeriodColumn` and `PayslipRow` to eliminate unwanted number wrapping on compact displays.
- **Bottom Navigation Bar Overlap**: Adjusted bottom padding on CalculatorScreen so action buttons scroll completely into view.

### What Needs to Be Fixed / Pending
- Direct PDF vector generation (currently supports native text-based payslip sharing).

---

## [2.0] - 2026-09-02 (VersionCode: 5)
### Added
- **Workplace Auto-Enrolment Pension**: Configurable employee contribution (0% to 15%, default 5%) with upfront **Net Pay Arrangement** tax relief reducing taxable gross; 3% statutory employer contribution calculated and displayed.
- **Scottish 6-Tier Income Tax Engine**: Complete support for Scotland's 2024/2025 tax system (Starter 19%, Basic 20%, Intermediate 21%, Higher 42%, Advanced 45%, Top 48%) toggled via Tax Region preference.
- **UK Student Loan Repayments**: Deductions for Plan 1 (threshold £24,990 @ 9%), Plan 2 (threshold £27,295 @ 9%), Plan 4 Scottish (threshold £31,395 @ 9%), and Postgraduate (threshold £21,000 @ 6%).
- **Pay Frequency Switcher**: Dynamic view switching between **Monthly**, **Weekly**, **Annual**, and **Hourly** take-home representations.
- **Overtime Multiplier Selector**: Support for `1.0x` (Standard), `1.5x` (Time-and-a-Half), and `2.0x` (Double Time) overtime calculation multipliers.
- **Multi-Period Comparison Table**: Comprehensive 4-column summary grid comparing Gross and Net earnings across Hourly, Weekly, Monthly, and Annual frequencies.
- **In-App Changelog & Release Notes Dialog**: Interactive modal in Settings rendering version 2.0 feature highlights.
- **Native Android Share Sheet**: 1-tap "Share Payslip Summary" action generating formatted plaintext payslip summaries.
- **Comprehensive Unit Testing Suite**: New test cases in [`TaxCalculatorTest.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/test/java/com/example/salarycalculator/domain/TaxCalculatorTest.kt) asserting Scottish 6-band rates, Student Loan thresholds, Pension tax relief, and multi-period conversions.

### Bugs Found & Fixed
- **Segmented Button Text Wrap**: Resolved text wrapping on 320px screens for `Monthly`/`Weekly` frequency buttons by optimizing icon slots and typography scaling.
- **Chip Horizontal Overflow**: Wrapped Tax Region, Student Loan, and Overtime Multiplier chip rows with horizontal scrolling to prevent layout clipping on compact displays.

### What Needs to Be Fixed / Pending
- Monthly history storage (resolved in v2.1).

---

## [1.3] - 2026-09-02 (VersionCode: 4)
### Added
- **Material 3 Adaptive App Icon**: Custom high-resolution vector icon featuring a financial calculator card, digital display with British Pound (`£`) currency symbol, and emerald action key ([`ic_launcher_foreground.xml`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/res/drawable/ic_launcher_foreground.xml)).
- **Deep Indigo Gradient Background**: Layered ambient gradient background with subtle geometry for launcher icons ([`ic_launcher_background.xml`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/res/drawable/ic_launcher_background.xml)).
- **Material You Dynamic Theming**: Added monochrome adaptive icon vector ([`ic_launcher_monochrome.xml`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/res/drawable/ic_launcher_monochrome.xml)) supporting Android 13+ wallpaper-based color tinting.

### Bugs Found & Fixed
- Replaced the default Android robot template icon with brand-consistent adaptive assets across all screen densities.

### What Needs to Be Fixed / Pending
- Add notification status bar drawables (`ic_stat_salary`) for background export alerts.

---

## [1.2] - 2026-09-02 (VersionCode: 3)
### Added
- **Deep Slate Dark Theme & Clean Slate Light Theme**: Curated high-contrast HSL color palettes in [`Color.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/theme/Color.kt) and [`Theme.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/theme/Theme.kt).
- **Theme Mode Selection & Persistence**: Added `ThemeMode` enum (`SYSTEM`, `LIGHT`, `DARK`) persisted in Jetpack DataStore Preferences via [`SalaryRepository.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/SalaryRepository.kt).
- **Settings Theme Switcher**: Material 3 `SingleChoiceSegmentedButtonRow` for instant switching between System Default, Light, and Dark modes in [`SettingsScreen.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/ui/settings/SettingsScreen.kt).
- **Spring-Animated Distribution Bar**: Multi-segment progress bar visually displaying the proportional breakdown of Take-Home Pay (Emerald), PAYE Tax (Rose), and National Insurance (Amber) with `animateFloatAsState`.
- **Take-Home Hero Card**: Glassmorphic estimated net pay card with animated numerical slide-fade transitions (`AnimatedContent`) and `% Take-Home` badge.
- **Quick-Select Presets**: One-tap chips for standard schedules (`Full Month 20d`, `UK Avg 21.7d`, `4-Day Week 16d`) and UK wage benchmarks (`£12.21`, `£12.60`, `£13.85`).

### Bugs Found & Fixed
- **UI Lag & Frame Drops**: Eliminated UI stutter during typing by memoizing all string conversions, parsing, and tax calculations with `remember(...)` and `derivedStateOf`.
- **Narrow Screen Text Wrapping**: Fixed Net Take-Home figure wrapping onto multiple lines on 320px screens using flexible weight constraints and responsive typography sizing.
- **Deprecated Color APIs**: Replaced deprecated `statusBarColor`/`navigationBarColor` window calls with modern `WindowCompat.getInsetsController` edge-to-edge system bar tinting.

### What Needs to Be Fixed / Pending
- Scottish tax tiers and Student Loans (resolved in v2.0).

---

## [1.1] - 2026-09-02 (VersionCode: 2)
### Added
- **Tax Engine Strictness & Annotations**: Added `// CRITICAL: TAX_ENGINE` annotations, non-standard tax code handling (`BR`, `0T`, `D0`, `D1`), and zero/negative bounds clamping with `max(0.0, ...)`.
- **Unit Test Suite**: Added [`TaxCalculatorTest.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/test/java/com/example/salarycalculator/domain/TaxCalculatorTest.kt) covering allowance parsing, 20% basic rate, 40% higher rate, 45% additional rate, Class 1 Primary NI thresholds, and zero income edge cases.
- **Automated Git Sync Rule**: Established `@rules:auto_git_sync` in [`AGENTS.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/AGENTS.md).

### Bugs Found & Fixed
- **Template Test Failures**: Fixed broken Android template tests in `MainScreenViewModelTest.kt` and `MainScreenTest.kt`.
- **Line Endings**: Sanitized CRLF line terminators on `gradlew` script to LF Unix format.

### What Needs to Be Fixed / Pending
- App lacked explicit dark theme option and modern UI styling (resolved in v1.2).
- Default Android robot icon used (resolved in v1.3).

---

## [1.0] - 2026-09-02 (VersionCode: 1)
### Added
- Initial project architecture with Kotlin 2.1.20 and Jetpack Compose BOM 2026.03.01.
- AndroidX Navigation 3 structure with Calculator and Settings keys.
- Basic UK PAYE and National Insurance computation.
- Jetpack DataStore preference storage for tax code and default hourly rate.
