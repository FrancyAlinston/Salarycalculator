# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### What Needs to Be Fixed / Upcoming
- [ ] End-of-year tax refund & rebate estimator for mid-year tax code corrections.
- [ ] Direct export of annual payroll summaries to HMRC self-assessment SA100 format.

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
