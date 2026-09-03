# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### What Needs to Be Fixed / Upcoming
- [ ] Real-time HMRC live tax rate updates via remote JSON configuration.
- [ ] Direct export of annual tax packs to cloud drives (Google Drive / OneDrive / Nextcloud).

---

## [9.0] - 2026-09-03 (VersionCode: 15)
### Added
- **Company Car Benefit-in-Kind (BiK) & Fuel Tax Calculator (`CompanyCarCalculator.kt` & `CompanyCarDialog.kt`)**: Interactive calculation engine modeling UK HMRC BiK percentages (2% Pure EV, 2%–14% PHEV based on zero-emission electric range, 15%–37% ICE based on CO2 g/km, +4% diesel surcharge), private fuel benefit statutory charge (£27,800 baseline), and exact net monthly take-home reduction across 20% Basic, 40% Higher, and 45% Additional tax bands.
- **Statutory Sick Pay (SSP) & Parental Leave (SMP / SPP) Modeling (`StatutoryLeaveCalculator.kt` & `StatutoryLeaveDialog.kt`)**: Detailed simulation of UK statutory wage entitlements including SSP (£116.75/wk with 3-day waiting rule), SMP (6 weeks at 90% AWE followed by 33 weeks at standard statutory rate £184.03/wk), SPP (1–2 weeks paternity pay), estimated net take-home pay, and comparison against regular working income.
- **Mortgage & Loan Borrowing Capacity Estimator (`MortgageBorrowingCalculator.kt` & `MortgageBorrowingDialog.kt`)**: Lender borrowing power modeling using 4.0x–5.0x salary multiples, cash deposit sizing, existing monthly debt commitments, stress-tested monthly loan amortization formula ($M = P \frac{r(1+r)^n}{(1+r)^n - 1}$), Loan-to-Value (LTV) %, and net disposable affordability health rating (Excellent, Moderate, Stretched, High Risk).
- **Bank Statement CSV Payroll Reconciliation Engine (`BankReconciliationEngine.kt` & `BankReconciliationDialog.kt`)**: Automated import, parsing, and reconciliation of bank statement CSV deposits against recorded payslips with automatic credit identification, exact net match assertion, variance detection, and discrepancy reporting.
- **Annual 12-Month Shift & Overtime Heatmap Planner (`ShiftCalendarDialog.kt` & `SalaryRepository.kt`)**: Full-year multi-month schedule management across all 12 months with persistent month-by-month working days and OT customization, annual aggregate summary metrics (total days, hours, overtime, and estimated annual gross), quick-fill presets ("Mon-Fri 8h", "4-on 4-off", "Copy to All 12 Months"), and full 12-month RFC 5545 `.ics` iCalendar calendar export.
- **Comprehensive v9.0 Verification Battery (`TaxCalculatorTest.kt`)**: Added automated unit test coverage verifying EV and PHEV BiK percentages, SSP 3-day waiting period deductions, SMP higher-rate calculation periods, mortgage amortization formulas, and bank CSV statement reconciliation matching.

### Bugs Found & Fixed
- **Deprecated Divider Migration**: Migrated deprecated Material 3 `Divider` components to `HorizontalDivider` across all dialogs.
- **Type-Safe Method Signatures**: Standardized `TaxCalculator.calculateTax` invocations and strengthened month integer parsing safety in `ShiftCalendarDialog`.

### What Needs to Be Fixed / Pending
- Real-time HMRC live tax rate updates via remote JSON configuration.
- Direct export of annual tax packs to cloud storage endpoints.

---

## [8.0] - 2026-09-03 (VersionCode: 14)
### Added
- **Multi-Year Statutory Tax Comparison Matrix (`TaxYearComparison.kt` & `TaxComparisonDialog.kt`)**: Interactive comparative analysis engine and custom Canvas bar chart comparing take-home pay, PAYE income tax, and Class 1 National Insurance across 2023/2024 (12% NI rate), 2024/2025 (8% NI rate cut), and 2025/2026 statutory regimes with instant annual savings badges.
- **HMRC Self-Assessment (SA100 / SA102) Formatter & PDF Exporter (`Sa100Generator.kt` & `Sa100Dialog.kt`)**: Automatic mapping of payroll records and live calculations to official HMRC employment return box numbers (Boxes 1–7) with 1-tap A4 vector PDF generation and system sharing via Android `FileProvider`.
- **Mid-Year Tax Code Refund & Rebate Estimator (`TaxRefundEstimator.kt` & `TaxRefundEstimatorDialog.kt`)**: Interactive cumulative PAYE refund model calculating one-off payslip refunds and monthly take-home increases when transitioning from emergency tax codes (`BR`, `0T`, `D0`) to standard allowances (`1257L`, `1383M`).
- **Multi-Tier Weekend & Bank Holiday Overtime Rates (`SalaryRepository.kt` & `SettingsScreen.kt`)**: Dedicated configuration chips and persistence for Weekday (`1.0x`–`1.5x`), Weekend (`1.5x`–`2.25x`), and Bank Holiday (`2.0x`–`3.0x`) overtime multipliers.
- **Dual Stable & Debug GitHub Release Packaging (`release.yml`)**: Automated GitHub Actions CI workflow assembling both debug and release APKs, guaranteeing stable releases are packaged strictly as `Salarycalculator.apk` and debug builds as `Salarycalculator-debug.apk`.
- **Comprehensive v8.0 Tax Test Battery (`TaxCalculatorTest.kt`)**: Automated unit test coverage verifying 2023–2025 NI savings calculations, emergency code refund formulas, and SA100 return box distributions.

### Bugs Found & Fixed
- **Deprecated Icon Migration**: Updated `Assignment`, `TrendingUp`, and `CompareArrows` icons to use `Icons.AutoMirrored.Filled.*`.
- **Responsive Layout for Tool Chips**: Placed the `Schedule Presets & Tools` header and horizontal chip bar into dedicated rows to prevent horizontal scrolling truncation in foldable and compact orientations.
- **Type Signature Unification**: Resolved `TaxReport` to `SalaryReport` references across SA100 generation dialogs.

### What Needs to Be Fixed / Pending
- Direct bank statement CSV import & automatic payslip matching.
- Real-time HMRC live tax rate updates via remote JSON configuration.

---

## [7.0] - 2026-09-03 (VersionCode: 13)
### Added
- **Scheduled Background Cloud Auto-Sync (`CloudSyncWorker.kt` & Android `WorkManager`)**: Background periodic 24-hour backup push to custom private domain endpoints with network connection and battery-not-low constraints.
- **Export Shift Calendar to iCalendar (.ics) (`IcsCalendarExporter.kt`)**: 1-tap export of logged monthly shift calendars and overtime shifts to RFC 5545 compliant `.ics` calendar files importable directly into Google Calendar, Apple Calendar, and Microsoft Outlook.
- **60% Marginal Tax Trap & Pension Sacrifice Visualizer (`MarginalTaxTrapDialog.kt`)**: Dynamic visual tool analyzing personal allowance tapering between £100,000 and £125,140 with interactive pension salary sacrifice remedy modeling.
- **Custom Foreign Exchange Rates Engine (`CurrencySettingsDialog.kt`)**: Configurable EUR (€) and USD ($) conversion rates with 1-tap "Market High" and "Default" preset buttons.
- **Biometric Auto-Lock Delay Customization (`MainActivity.kt` & `SettingsScreen.kt`)**: Configurable auto-lock delay (Immediate, 1 min, 5 min, 15 min) allowing seamless app switching without triggering immediate re-authentication.
- **Comprehensive v7.0 Verification Battery (`TaxCalculatorTest.kt`)**: Added automated test coverage for RFC 5545 `.ics` formatting, £100k+ personal allowance tapering mathematics, and custom FX conversion calculations.

### Bugs Found & Fixed
- **Unresolved Navigation & Icon Imports**: Fixed missing `LocalContext`, `Share` icon, and `clickable` imports in `ShiftCalendarDialog` and `CalculatorScreen`.
- **TaxYear Enum Reference**: Corrected outdated `TaxYear.CURRENT_2024_2025` reference to `TaxYear.YEAR_2024_2025`.

### What Needs to Be Fixed / Pending
- Advanced multi-year tax comparison chart across 2023/2024, 2024/2025, and 2025/2026 statutory regimes.

---

## [6.0] - 2026-09-03 (VersionCode: 12)
### Added
- **Private Domain / Custom Cloud Save & Sync (`CustomCloudSyncManager.kt` & `CustomCloudSyncDialog.kt`)**: Connect to your self-hosted server, private domain (e.g. Nextcloud, WebDAV, or custom REST endpoint) with optional Bearer Token / API Key auth, featuring 1-tap "Test Connection", "Push Ledger to Cloud", and "Pull & Restore".
- **Interactive Shift Calendar & Overtime Heatmap (`ShiftCalendarDialog.kt`)**: Visual 30-day interactive calendar modal with color-coded shift heatmaps (Day Off, 8h Regular, 10h Overtime), summary of total days and hours worked, and 1-tap "Apply to Calculator".
- **UK Tax Code Allowance Explainer Modal (`TaxCodeExplainerDialog.kt`)**: Interactive education dialog breaking down personal allowance calculation rules for standard UK (`1257L`), secondary job (`BR`, `0T`, `D0`, `D1`), Marriage allowance (`M`, `N`), and Scottish (`S`) tax codes.
- **Bonus & Commission Variable Earnings Engine (`TaxCalculator.kt`)**: Dedicated bonus and commission input fields in the Live Calculator calculating variable gross and marginal PAYE/NI tax rates.
- **Real-Time Multi-Currency Converter (`CurrencyConverter.kt`)**: Live estimated take-home conversions in EUR (€) and USD ($) rendered directly below the main GBP net pay header.
- **NVIDIA & Host GPU Emulator Acceleration**: Verified `-gpu host` hardware acceleration support for Android emulators on Linux.
- **Automated Verification Battery**: Added unit test cases for bonus/commission tax computations, lossless JSON cloud bundle serialization, and currency conversions.

### Bugs Found & Fixed
- **Optimized Dialog Layouts for Wide & Foldable Devices**: Refactored `ShiftCalendarDialog` to use chunked row layouts to prevent measurement conflicts inside `AlertDialog`.
- **Deprecated Icon Migration**: Upgraded `HelpOutline` to `AutoMirrored.Filled.Help` to maintain clean zero-warning build output.

### What Needs to Be Fixed / Pending
- Background periodic sync via Android `WorkManager`.

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
- Custom cloud domain sync (resolved in v6.0).

---

## [4.0] - 2026-09-03 (VersionCode: 10)
### Added
- **Official Annual P60 End-of-Year Certificate (`P60Generator.kt` & `P60Dialog.kt`)**: Generates an authentic HMRC-styled A4 vector PDF P60 certificate aggregating all recorded pay periods across the tax year.
- **Multiple Job & Employer Profiles (`EmployerProfile.kt` & `ProfileManagerDialog.kt`)**: Full support for managing multiple concurrent employments with independent hourly rates, tax codes, pension rates, tax regions, and student loans.
- **1-Tap Profile Switcher**: Quick-switch active employment profile directly from the Live Calculator header chip or Settings screen.
- **Marriage Allowance Statutory Relief (`TaxCalculator.kt`)**: Configurable toggle transferring £1,260 personal allowance from spouse.
- **Blind Person's Allowance (`TaxCalculator.kt`)**: Statutory £3,070/year additional tax-free personal allowance.
- **Custom Recurring Deductions (`CustomDeduction.kt`)**: Support for user-defined pre-tax and post-tax deduction line items.
- **Tax Year Framework Selector**: Seamlessly switch between current 2024/2025 and upcoming 2025/2026 statutory HMRC tax thresholds.

---

## [3.0] - 2026-09-03 (VersionCode: 9)
### Added
- **Native Vector PDF Payslip Generator (`PdfPayslipGenerator.kt`)**: Generates official A4 payslip documents with company/employee header, statutory breakdowns, and FileProvider sharing.
- **CSV Spreadsheet Exporter (`CsvSalaryExporter.kt`)**: 1-tap export of the entire salary history ledger into `.csv` files.
- **Side-by-Side Month Diff & Comparison Tool (`MonthDiffDialog.kt`)**: Interactive comparison dialog calculating exact numerical and percentage variances.
- **Interactive Salary Analytics Chart (`SalaryTrendChart.kt`)**: Custom Compose Canvas rendering monthly Gross vs Take-Home bars.
- **Salary Sacrifice Schemes**: Pre-tax deductions for Cycle to Work and Electric Vehicle (EV) schemes.

---

## [2.3] - 2026-09-03 (VersionCode: 8)
### Added
- **Book-Style Foldable Dual-Screen Layout**: Implemented native dual-pane layout architecture (`maxWidth >= 720dp`) for foldables and tablets. Left pane = fullscreen Calculator; right pane = fullscreen companion workspace (History / Settings).

---

## [2.2] - 2026-09-03 (VersionCode: 7)
### Added
- **Adaptive Dual-Pane Layout for Foldables & Tablets**: Automatic transformation into a 2-column wide layout (`maxWidth >= 600dp`).
- **Zero-Wrap Currency Formatting**: Fixed decimal truncation and currency wrapping.

---

## [2.1] - 2026-09-03 (VersionCode: 6)
### Added
- **Persistent Monthly Salary History**: Full snapshot persistence of monthly payslips using Jetpack DataStore Preferences.
- **Dedicated History Screen (`HistoryScreen.kt`)**: Added 3rd navigation destination featuring cumulative earnings statistics.

---

## [2.0] - 2026-09-02 (VersionCode: 5)
### Added
- **Workplace Auto-Enrolment Pension**: Configurable employee contribution with **Net Pay Arrangement** upfront tax relief.
- **Scottish 6-Tier Income Tax Engine**: Complete support for Scotland's 2024/2025 tax system.
- **UK Student Loan Repayments**: Deductions for Plan 1, Plan 2, Plan 4, and Postgraduate loans.
