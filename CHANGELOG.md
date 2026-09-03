# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### What Needs to Be Fixed / Upcoming
- [ ] Direct Google Drive REST API integration for automated sync.
- [ ] Exportable shift calendar with visual hourly punch heatmap.

---

## [5.0] - 2026-09-03 (VersionCode: 11)
### Added
- **Full Ledger JSON Backup & Restore (`LedgerBackupManager.kt` & `BackupRestoreDialog.kt`)**: 1-tap complete backup export and restore of all monthly salary records, employer profiles, custom deductions, and settings via Android Sharesheet and JSON data import.
- **Direct Shift Timesheet Stopwatch & Punch Clock (`ShiftTracker.kt` & `ShiftStopwatchCard.kt`)**: Real-time punch-in / punch-out stopwatch that records shift start/end timestamps and automatically transfers accumulated days and hours into the live salary calculator.
- **Biometric & Device PIN Privacy App Lock (`BiometricPrompt` in `MainActivity.kt`)**: Hardware-backed fingerprint, face unlock, and device credential security protecting confidential payroll and historical records.
- **High Income Child Benefit Charge (HICBC) Calculator (`ChildBenefitCalculator.kt` & `ChildBenefitDialog.kt`)**: Interactive modal calculating statutory 2024/2025 child benefit entitlement, £60,000–£80,000 taper clawback percentage (1% per £200), and HMRC tax charge.
- **Comprehensive Multi-Rate Full Battery Testing Suite**: Added exhaustive automated tests asserting arithmetic invariants ($Gross = Net + Deductions$) across varying wage points (£1,000 to £12,000) and Scottish/UK tax brackets.

### Bugs Found & Fixed
- **Unresolved Reference on Scope Variables**: Hoisted backup dialog state and biometric preferences to top-level composable scopes to prevent recreation cycles and compilation errors.

### What Needs to Be Fixed / Pending
- Direct Google Drive REST sync.

---

## [4.0] - 2026-09-03 (VersionCode: 10)
### Added
- **Official Annual P60 End-of-Year Certificate (`P60Generator.kt` & `P60Dialog.kt`)**: Generates an authentic HMRC-styled A4 vector PDF P60 certificate aggregating all recorded pay periods across the tax year with total pay, total PAYE income tax, Class 1 Primary NI, workplace pension, and student loan totals.
- **Multiple Job & Employer Profiles (`EmployerProfile.kt` & `ProfileManagerDialog.kt`)**: Full support for managing multiple concurrent employments (Primary Employment, Second Job, Freelance Shift) with independent hourly rates, tax codes (e.g. `1257L` vs `BR`), pension rates, tax regions, and student loans.
- **1-Tap Profile Switcher**: Quick-switch active employment profile directly from the Live Calculator header chip or Settings screen.
- **Marriage Allowance Statutory Relief (`TaxCalculator.kt`)**: Configurable toggle transferring £1,260 personal allowance from spouse, reducing income tax by up to £252/year (£21/month).
- **Blind Person's Allowance (`TaxCalculator.kt`)**: Statutory £3,070/year additional tax-free personal allowance.
- **Custom Recurring Deductions (`CustomDeduction.kt`)**: Support for user-defined pre-tax and post-tax deduction line items (Trade Union dues, Professional Subscriptions, Healthcare).
- **Tax Year Framework Selector**: Seamlessly switch between current 2024/2025 and upcoming 2025/2026 statutory HMRC tax thresholds.

### Bugs Found & Fixed
- **Header Chip Text Overflow on Dual-Screen Devices**: Wrapped Calculator header chips in a horizontal scrolling row to eliminate letter clipping and prevent vertical line wrapping on wide and compact displays.

### What Needs to Be Fixed / Pending
- Cloud backup & synchronization to Google Drive (resolved in v5.0).

---

## [3.0] - 2026-09-03 (VersionCode: 9)
### Added
- **Native Vector PDF Payslip Generator (`PdfPayslipGenerator.kt`)**: Generates official A4 payslip documents with company/employee header, schedule, tax & pension details, payments breakdown, deductions, and bold take-home hero boxes.
- **Android FileProvider Integration**: Configured secure file sharing in `AndroidManifest.xml` and `provider_paths.xml` for seamless PDF and CSV sharing via Android Sharesheet.
- **CSV Spreadsheet Exporter (`CsvSalaryExporter.kt`)**: 1-tap export of the entire salary history ledger into `.csv` files for Excel and Google Sheets.
- **Side-by-Side Month Diff & Comparison Tool (`MonthDiffDialog.kt`)**: Interactive comparison dialog calculating exact numerical and percentage variances (Gross, Tax, NI, Pension, Net) between any two saved months.
- **Interactive Salary Analytics Chart (`SalaryTrendChart.kt`)**: Custom Compose Canvas rendering monthly Gross vs Take-Home bars along with a dashed average pay reference line.
- **Salary Sacrifice Schemes**: Pre-tax deductions for Cycle to Work and Electric Vehicle (EV) schemes reducing taxable gross before Income Tax, NI, and pension calculations.
- **Direct PDF Export Actions**: Added 1-tap "PDF" generation buttons directly on the Live Calculator screen and on every saved monthly record in History.

### Bugs Found & Fixed
- **Missing File Sharing Permissions**: Resolved Android URI sharing crashes by integrating `androidx.core.content.FileProvider`.

### What Needs to Be Fixed / Pending
- Annual P60 tax document generator (resolved in v4.0).

---

## [2.3] - 2026-09-03 (VersionCode: 8)
### Added
- **Book-Style Foldable Dual-Screen Layout**: Implemented native dual-pane layout architecture (`maxWidth >= 720dp`) designed for unfolded book-style foldables (Galaxy Z Fold, Pixel Fold) and tablets. Left pane renders fullscreen Main Calculator; right pane renders fullscreen companion workspace (Salary History / Settings) with top segmented switcher.
- **Physical Hinge Divider**: Added subtle Material 3 vertical fold divider mirroring the phone's physical crease.
- **Adaptive Single/Dual Screen Switching**: Seamlessly adapts between single-screen bottom navigation on folded cover screens and dual-screen workspace when unfolded.
- **Automated GitHub Release Tagging**: Updated `.github/workflows/release.yml` with semantic version tag triggers (`v*`) and automatic release notes generation.

### Bugs Found & Fixed
- **Large Screen Real Estate Underutilization**: Replaced stretched single-column layouts on wide foldables with side-by-side productive dual-screen layout.

### What Needs to Be Fixed / Pending
- Direct PDF vector generation (resolved in v3.0).

---

## [2.2] - 2026-09-03 (VersionCode: 7)
### Added
- **Adaptive Dual-Pane Layout for Foldables & Tablets**: Automatic transformation into a 2-column wide layout (`maxWidth >= 600dp`) separating input controls and real-time calculation output panes.
- **Adaptive Centering on Large Phones**: Added `Modifier.widthIn(max = ...)` container bounds across all screens (`CalculatorScreen`, `HistoryScreen`, `SettingsScreen`) for balanced visual composition.
- **Zero-Wrap Currency Formatting**: Fixed decimal truncation and currency wrapping (`+£61.01`, `£2,034 grs`) with `maxLines = 1` and flexible-weight labels.
- **Redesigned Cumulative History Header**: Clean horizontal column separation between Total Take-Home and Average Monthly Net figures in `HistoryScreen.kt`, preventing text collisions.

### Bugs Found & Fixed
- **History Header Text Overlap**: Resolved vertical label collision in `HistoryScreen` where average net pay overlapped total take-home numbers.
- **Currency Splitting in Payslip Rows**: Prevented currency symbols and decimal fractions from wrapping onto multiple lines across compact displays.

### What Needs to Be Fixed / Pending
- Direct PDF vector generation (resolved in v3.0).

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
- Direct PDF vector generation (resolved in v3.0).

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
