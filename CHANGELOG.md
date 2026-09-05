# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [18.0] - 2026-09-05 (VersionCode: 26)
### Added
- **Overtime Tax-Efficiency & Marginal Return Optimizer (`OvertimeOptimizerEngine.kt` & `OvertimeTaxOptimizerDialog.kt`)**: Interactive simulation modal determining the exact marginal take-home cash return per hour of overtime worked (from 0 to 40+ hours) across selectable multipliers (`1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.5x`). Models extra gross earnings vs extra PAYE, NI, Student Loan, and Pension deductions, calculates retention percentage, and displays real-time efficiency ratings (High 65%+, Moderate 50%–65%, Low <50%) and 60% marginal tax trap warnings.
- **Multi-Employer Shift Color Coding & Dual Schedules (`EmployerProfile.kt`, `SalaryRepository.kt`, `ShiftCalendarDialog.kt`)**: Full support for multi-job workers. Added `colorHex` profile customization and persistent employer shift mapping (`getShiftEmployerAssignments`). In the Shift Heatmap, users can filter by employer, assign specific shifts to individual employers, view custom-colored day badges, and inspect separated earnings breakdowns per employer.
- **Comprehensive v18.0 Unit Test Suite (`OvertimeOptimizerTest.kt`)**: 100% automated test coverage validating marginal retention rates across basic rate (72%), higher rate (58%), marginal tax trap warnings, and employer color serialization.
- **Forgejo Actions CI/CD & Automated Dual-Git Sync (`.forgejo/workflows/release.yaml`)**: Added dedicated Forgejo Actions release workflow for automated APK building and release publishing, plus dual-remote Git synchronization support across GitHub and Forgejo repositories.

### Bugs Found & Fixed
- **Emulator Auto-Teardown Compliance**: Formally integrated mandatory emulator teardown rule (`@rules:emulator_auto_close_on_completion`) ensuring all emulator processes are killed upon test completion.

### What Needs to Be Fixed / Pending
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [17.2] - 2026-09-05 (VersionCode: 25)
### Added
- **Post-Cutoff Rollover Payroll Ingestion Engine (`PayScheduleEngine.kt`, `ShiftCalendarDialog.kt`, `CalculatorScreen.kt`)**: Modeled strict UK payroll rollover rule where shifts logged after the cutoff date in Month $M-1$ are automatically brought forward and included in Month $M$'s payslip calculation (`totalPaidDays`, `totalPaidHours`, `totalPaidOtHours`), while shifts after Month $M$'s cutoff roll into Month $M+1$.
- **Automated Rotational Shift Pattern Generator (`ShiftPatternGenerator.kt` & `ShiftPatternGeneratorDialog.kt`)**: Instant 1-tap generation of industrial rotational shift schedules across 1, 3, or 12 months with anchor date synchronization. Supported patterns:
  - **4-On 4-Off** (8-day repeating cycle)
  - **Continental 2-2-3** (28-day 2D / 2N / 3Off / 2D / 3N / 2Off rotation)
  - **Pitman 2-3-2** (14-day cycle with alternating 3-day weekends)
  - **3-Shift Rotating** (28-day Early / Late / Night rotation)
  - **Mon–Fri Standard** (5-On 2-Off working week)
- **Overtime Tax Bracket & 60% Marginal Tax Trap Alerts (`TaxCalculator.kt`, `SandboxCalculatorDialog.kt`, `ShiftCalendarDialog.kt`)**: Real-time tax threshold detection identifying when cumulative or overtime earnings push annual income into the 40% Higher Rate band (> £50,270) or the 60% Marginal Tax Trap (£100,000 – £125,140 Personal Allowance tapering zone). Features instant pension salary sacrifice recommendations to eliminate marginal penalty and preserve personal allowance.
- **Comprehensive v17.2 Unit Test Suite (`ShiftPatternAndTaxAlertTest.kt` & `PayScheduleEngineTest.kt`)**: 100% automated test coverage validating previous-month rollover addition, rotational pattern algorithms, and exact tax trap sacrifice math.

### Bugs Found & Fixed
- **Cutoff Rollover Month-to-Month Omission**: Fixed payroll calculation omitting hours worked after the previous month's cutoff date by integrating multi-month shift retrieval and rollover aggregation in `PayScheduleEngine.calculateShiftPayrollSplit`.

### What Needs to Be Fixed / Pending
- Multi-Employer Shift Color Coding and overlay scheduling in calendar view.
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [17.1] - 2026-09-05 (VersionCode: 24)
### Added
- **Isolated "What-If" Scenario Sandbox Calculator (`SandboxCalculatorDialog.kt`)**: Decoupled variable earnings and scenario modeling from the live Shift Heatmap into a dedicated sandbox environment. Supports standard working days/hours, independent Overtime multiplier pills (`1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.5x`), dedicated Bank Holiday multiplier pills (`1.5x`, `2.0x`, `2.5x`, `3.0x`), variable bonus/commission inputs, quick scenario presets (+2 Weekend OT, +1 Bank Holiday, +£500 Bonus), and real-time net take-home pay comparison vs baseline actual payslip.
- **Base Hours per Working Day Configuration in Settings (`SettingsScreen.kt` & `SalaryRepository.kt`)**: Configurable standard base hours per working day (default `8.0h`, with presets `7.5h`, `8.0h`, `10.0h`, `12.0h`) persisted in DataStore (`default_hours_per_day`), automatically consumed across the home screen, Shift Heatmap, and schedule presets.
- **Sequential Month-by-Month Heatmap Navigation (`ShiftCalendarDialog.kt`)**: Re-engineered primary `<` and `>` arrow navigation to step sequentially through individual months with automatic year rollover (e.g., December 2026 $\rightarrow$ January 2027), accompanied by a dedicated year selector dropdown/chips.
- **Visual Shift Heatmap Color Legend (`ShiftCalendarDialog.kt`)**: Added prominent color legend bar defining 🟢 Standard 8h Shift (`Emerald60`), 🟠 Overtime 10h Shift (`Amber60`), 🔴 Long/Night 12h Duty (`Rose60`), 🔵 Part-Time <8h Shift (`Teal60`), and ⚪ Day Off.
- **Live Home Screen Sync with Active Month Heatmap Schedule (`CalculatorScreen.kt`)**: On app launch and upon calendar changes, the Home screen automatically syncs with the current calendar month's saved shift schedule (`getMonthShiftSchedule`), reflecting accurate days worked, average hours, and overtime.
- **Comprehensive v17.1 Unit Test Suite (`ShiftPersistenceAndSandboxTest.kt`)**: 100% automated test coverage validating multi-year JSON persistence, sequential month rollover, strict zero-state £0.00 earnings calculation, and sandbox take-home variance modeling.

### Bugs Found & Fixed
- **Shift Heatmap State Persistence Bug (Issue #1.1)**: Eliminated unconditional default shift overwriting on dialog reopen by replacing destructive memory seeding with non-destructive multi-year JSON persistence keyed by `"YYYY-MM"`.
- **Month vs Year Navigation Inversion (Issue #1.3)**: Corrected top navigation arrows to cycle months sequentially rather than skipping entire years.
- **Heatmap Zero-State False Earnings Bug (Issue #1.4)**: Enforced strict £0.00 gross and net take-home calculation when a viewed month contains zero marked shifts.

### What Needs to Be Fixed / Pending
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [17.0] - 2026-09-05 (VersionCode: 23)
### Added
- **Payroll Pay Schedule & Cutoff Date Engine (`PayScheduleEngine.kt`)**: Modeled standard UK company pay schedule rules with primary default for **Last Friday of the Month (with preceding Sunday timesheet cutoff)**. Exact calculation of monthly cutoff deadline ($5$ days prior to pay date at 23:59), cycle start dates, and post-cutoff rollover partitioning.
- **Pay Cycle & Cutoff Visualizer in Shift Calendar (`ShiftCalendarDialog.kt`)**: Color-coded calendar badges for **Cutoff Day** (amber badge & border) and **Payday** (emerald badge & border), visual rollover indicator (`+Roll`) for post-cutoff shifts, payroll summary banner partitioning in-cycle hours vs rolled-over hours, and 1-tap "Apply Cutoff Hours" selector.
- **Pay Schedule Configuration in Settings (`PayScheduleSettingsDialog.kt` & `SettingsScreen.kt`)**: Dedicated interactive configuration modal supporting selectable employer pay rules (*Last Friday of Month*, *Last Working Day*, *Fixed Day of Month with Lead-Time*, *Four-Weekly*, *Bi-Weekly*, *Calendar Month*) with live preview of upcoming pay & cutoff dates.
- **Automated Payday & Cutoff iCalendar Export with Alarms (`IcsCalendarExporter.kt`)**: Full-year RFC 5545 `.ics` calendar generation for company paydays and timesheet cutoff deadlines with integrated `VALARM` notifications (cutoff reminder the night before, and morning payday alert with estimated net take-home pay).
- **Comprehensive v17.0 Unit Test Suite (`PayScheduleEngineTest.kt`)**: 100% automated test coverage validating 12-month Last Friday & Sunday cutoff schedules for 2025/2026, shift splitting accuracy, and `.ics` RFC 5545 format compliance.

### Bugs Found & Fixed
- **Adaptive Launcher Icon Zoom & Mask Clipping**: Re-engineered adaptive foreground layer across all density buckets (`mipmap-mdpi` through `mipmap-xxxhdpi`) to strictly fit within the Android 62% (67dp) safe zone, eliminating launcher edge clipping and over-zooming on installed home screens.
- **Material 3 AutoMirrored Icon Compatibility**: Replaced deprecated `Icons.Filled.TrendingUp` references in `CapitalGainsDialog.kt` with `Icons.AutoMirrored.Filled.TrendingUp`.
- **iCalendar Exporter DateFormatSymbols Import**: Fixed missing `java.text.DateFormatSymbols` import in `IcsCalendarExporter.kt`.

### What Needs to Be Fixed / Pending
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [16.0] - 2026-09-03 (VersionCode: 22)
### Added
- **Self-Employed Payments on Account & Balancing Charge Calculator (`SelfEmployedTaxEngine.kt` & `SelfEmployedTaxDialog.kt`)**: Implemented HMRC statutory Self-Assessment calculations including £1,000 Trading Allowance, Class 4 NI (6% main band / 2% upper band), combined PAYE + Self-Employment assessment, and automatic Payments on Account calculation (50% due 31 January, 50% due 31 July) with first-year cash outlay projections.
- **Gift Aid Tax Relief & Higher-Rate Band Extension Optimizer (`GiftAidOptimizer.kt` & `GiftAidDialog.kt`)**: Modeled charitable donations with 25% HMRC basic rate top-up, Higher Rate (40%) and Additional Rate (45%) personal tax reclaim calculations (saving up to 25% of gross donation), and basic rate tax band expansion from £37,700.
- **Capital Gains Tax (CGT) Annual Exemption (£3,000) & Asset Disposal Planner (`CapitalGainsTaxEngine.kt` & `CapitalGainsDialog.kt`)**: Modeled 2024/25 & 2025/26 £3,000 statutory CGT annual exempt amounts, basic rate (10% standard / 18% residential property) and higher rate (20% standard / 24% residential property) tax computations, and taxable income band absorption.
- **Direct PDF/CSV Email Dispatch Intent (`EmailExporter.kt`)**: Added 1-tap email composer button in action toolbar pre-filling subject, body summary, and attaching monthly PDF payslips, P60 certificates, or CSV reports via Android `FileProvider`.
- **Astronomical Solar Sunset/Sunrise Dynamic Dark Mode (`SolarThemeScheduler.kt`, `Theme.kt`, `SettingsScreen.kt`)**: Integrated astronomical sunrise and sunset calculation for UK coordinates to smoothly auto-toggle Material 3 Dark/Light mode at twilight without requiring location permissions.
- **Comprehensive v16.0 Unit Test Suite (`SelfEmployedAndCgtTest.kt`)**: Automated test coverage for self-employed profits, Class 4 NI, Payments on Account triggers, Gift Aid relief, CGT calculations, and Solar scheduler decimal hours (100% pass rate).

### Bugs Found & Fixed
- **Actions Storage Quota Resilience**: Removed ephemeral `actions/upload-artifact` blocker from release workflow to guarantee releases attach APK assets directly to GitHub Releases without hitting the 500MB Actions storage limit.

### What Needs to Be Fixed / Pending
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [15.0] - 2026-09-03 (VersionCode: 21)
### Added
- **Pension Annual Allowance (£60,000) & Tapered Allowance Optimizer (`PensionAllowanceOptimizer.kt` & `PensionAllowanceDialog.kt`)**: Implemented statutory UK pension allowance calculations covering £60k standard annual allowance, high-earner tapered allowance (£260k–£360k adjusted income tapering £1 for every £2 down to £10k floor), Money Purchase Annual Allowance (MPAA £10k) restriction, 3-year unused carry-forward calculations (2021/22–2023/24), and marginal rate tax charge estimations on excess contributions.
- **Offline Background Cloud Sync Retry Queue (`SyncQueueManager.kt` & `SyncQueueWorker.kt`)**: Built persistent JSON/SQLite sync request queue with automatic exponential backoff retry via `WorkManager` when device connects to unmetered network.
- **Month-over-Month Payslip Variance Heatmap & Diff (`PayslipVarianceDialog.kt`)**: Added interactive multi-period comparison matrix calculating chronological month-on-month percentage and pound deltas for gross pay, net pay, PAYE tax, NI, pension, and overtime hours with color-coded trend badges.
- **Student Loan Repayment Horizon & Early Payoff Calculator (`StudentLoanPayoffEngine.kt` & `StudentLoanPayoffDialog.kt`)**: Full mathematical simulation for Plan 1, Plan 2, Plan 4, and Postgraduate repayment thresholds, statutory interest rates (RPI + variable up to 7.7%), 30-year statutory write-off dates, and voluntary overpayment interest savings.
- **Automated Scheduled ZIP Tax Bundle Archiving (`ScheduledBackupWorker.kt`)**: Periodic background worker automatically compiling and archiving encrypted Annual Tax Pack ZIP bundles and payroll records on a weekly (7d) or monthly (30d) cadence.
- **Comprehensive v15.0 Unit Test Suite (`PensionAndStudentLoanTest.kt`)**: Automated unit test coverage for pension tapering formulas, MPAA triggers, carry-forward addition, student loan write-offs, and early payoff interest savings.

### Bugs Found & Fixed
- **Compose Icon Deprecation Cleanup**: Fully migrated all `TrendingUp`, `TrendingDown`, and `CompareArrows` icons to `Icons.AutoMirrored.Filled.*` across all calculator and history dialogs.

### What Needs to Be Fixed / Pending
- Dynamic live exchange rate streaming for crypto/fiat pairs.

---

## [14.0] - 2026-09-03 (VersionCode: 20)
### Added
- **Android 13+ Material You Monochrome Themed Adaptive Icon (`ic_launcher_monochrome.xml`, `ic_launcher.xml`, `ic_launcher_round.xml`)**: Added dedicated vector monochrome silhouette of the calculator and £ money bag allowing Android 13+ devices to dynamically tint launcher icons to match user system wallpaper palettes.
- **ML-Powered Salary & Year-End Tax Forecast Engine (`SalaryForecastEngine.kt` & `SalaryForecastDialog.kt`)**: Ordinary Least Squares (OLS) multi-month linear regression and seasonal time-series projection modeling annual gross earnings, R² goodness-of-fit confidence scoring, trend velocity (+/- £/mo), projected full-year PAYE tax liability vs actual deductions, and automatic detection of HMRC year-end tax rebates or underpayment shortfalls.
- **Selective Biometric Privacy Lock on History Ledger (`SalaryRepository.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`)**: Added granular biometric security options enabling users to specifically protect confidential historical payslips and annual tax packs behind biometric authentication while keeping the live calculator immediately accessible.
- **Customizable Material 3 Color Theme Palettes (`Theme.kt`, `Color.kt`, `SettingsScreen.kt`)**: Introduced 4 curated Material 3 themes with live preview and DataStore persistence: **Ocean Sapphire** (Indigo/Teal), **Emerald Green** (Emerald/Mint), **Midnight Violet** (Violet/Amethyst), and **Sunset Amber** (Amber/Gold).
- **Interactive Payslip OCR Correction & Re-Calculation Editor (`PayslipImportDialog.kt`)**: Added field-level editing and 1-tap "Re-calculate with HMRC Formula" helper in the OCR scanner dialog allowing users to verify, adjust, and recalculate extracted numbers before committing to the salary history ledger.
- **Comprehensive v14.0 Test Suite (`SalaryForecastEngineTest.kt`)**: Added automated regression unit test coverage for baseline forecasts, linear slope trajectories, R² confidence bounds, and 60% marginal tax trap detection.

### Bugs Found & Fixed
- **CI/CD Release Workflow Repair (`release.yml`)**: Fixed fatal build failure caused by attempting to copy non-existent `app-debug.apk` when `base.archivesName` is set to `Salarycalculator`. Added dynamic APK path detection, artifact extraction into `release-artifacts/`, automatic semantic version tagging (`v${versionName}`), and GitHub Actions artifact uploads (`actions/upload-artifact@v4`).
- **Launcher Icon Formatting**: Cleaned up legacy WebP mipmap remnants and isolated the calculator body with pure alpha transparency.
- **Adaptive Safe Zone Layout**: Enforced 72dp safe zone centering for all adaptive foreground layers to eliminate circular launcher edge clipping.

### What Needs to Be Fixed / Pending
- Background auto-retry queue for offline cloud sync failures.
- Pension Annual Allowance (£60k) threshold alert banner.

---

## [11.0] - 2026-09-03 (VersionCode: 17)
### Added
- **Payslip Image & PDF OCR Scanner with Statutory Discrepancy Diagnostics (`PayslipParserEngine.kt`, `PayslipOcrAnalyzer.kt`, `PayslipImportDialog.kt`)**: On-device machine learning text extraction using Google ML Kit Latin recognizer and Android `PdfRenderer` for direct import of physical payslip photos and PDF digital payslips. Includes regex and heuristic field parsing (gross pay, net pay, PAYE tax, NI, pension, student loan, tax code, pay period, and employer name), verification analysis asserting exact statutory alignment against HMRC formulas, and 1-tap "Save to History Ledger" and "Apply to Live Calculator".
- **Company Director Dividend vs Salary Tax Optimizer (`DirectorDividendOptimizer.kt` & `DirectorDividendDialog.kt`)**: Interactive tax planning engine for Ltd company owner-directors modeling Corporation Tax (19% small profits rate $\le$ £50k, 25% main rate $\ge$ £250k, and marginal relief between £50k–£250k), personal dividend tax rates (8.75% Basic, 33.75% Higher, 39.35% Additional), £500 statutory dividend allowance, and 4-way comparative scenarios (£12,570 optimal salary + dividends, £9,100 primary NI threshold + dividends, 100% PAYE salary, and 100% dividends) displaying exact net cash in pocket and annual tax savings.
- **Cloud Drive Direct Export Engine (`CloudDriveExporter.kt` & `CloudDriveExportDialog.kt`)**: Direct network upload module supporting WebDAV, Nextcloud, ownCloud, Bearer Token REST, and Basic Auth endpoints for uploading Annual Tax Pack ZIP archives, official P60 PDFs, HMRC SA100 return PDFs, and CSV payroll files with persistent server credentials.
- **Multi-Currency History Ledger Toggle (`HistoryScreen.kt`)**: Real-time currency selector chips (`GBP £`, `EUR €`, `USD $`) on the salary history overview and individual record cards, dynamically converting gross pay, tax, NI, pension, deductions, and take-home amounts using live synced or custom exchange rates.
- **Comprehensive v11.0 Unit Test Suite (`TaxCalculatorTest.kt`)**: Automated test coverage for payslip OCR regex extraction, emergency tax code detection, Corporation Tax bracket modeling, and Director Dividend optimization comparisons.

### Bugs Found & Fixed
- **Method Signature & Property Mapping**: Standardized `MonthlySalaryRecord` note constructor parameter and resolved ML Kit `Tasks.await` coroutine integration.
- **Dynamic Calculation Interop**: Cleaned parameter passing to `TaxCalculator.calculateTax` inside `DirectorDividendOptimizer` and `PayslipParserEngine`.

### What Needs to Be Fixed / Pending
- Advanced ML-based salary forecasting and year-end tax liability projections.
- Biometric fingerprint/face unlock for confidential payroll history.

---

## [10.0] - 2026-09-03 (VersionCode: 16)
### Added
- **Expanded Overtime & Standard 1.0x Rates (`SettingsScreen.kt` & `CalculatorScreen.kt`)**: Added standard single rate (`1.0x`), `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.25x`, `2.5x`, and `3.0x` multipliers for Weekday, Weekend, and Bank Holiday overtime calculations across both Preferences and the Live Calculator.
- **Dynamic HMRC Statutory Tax Rate Config (`HmrcRateSyncManager.kt` & `HmrcRateSyncDialog.kt`)**: Remote JSON synchronization engine allowing real-time updates for Personal Allowances, basic/higher tax bands, Class 1 NI primary thresholds, Scottish tax rates, and UK National Living Wage rates (£12.21 21+, £10.00 18-20, £7.55 Apprentice) with 1-tap "Restore Statutory Baseline".
- **Live Foreign Exchange Cloud Sync Engine (`LiveFxSyncEngine.kt` & `CurrencySettingsDialog.kt`)**: Automated real-time synchronization of open currency exchange rates for GBP $\rightarrow$ EUR and GBP $\rightarrow$ USD with offline fallback caches and instant "Sync Live FX" action button.
- **Annual Tax Pack One-Click ZIP Bundle (`TaxPackZipExporter.kt` & `TaxPackExportDialog.kt`)**: 1-tap compiler bundling all statutory tax records into a single `.zip` archive containing the official P60 PDF certificate, HMRC SA100 return PDF, 12-month shift `.ics` calendar, raw `.csv` payroll ledger, printable annual shift poster PDF, and audit summary `README_Tax_Pack.txt`.
- **12-Month Shift Year-at-a-Glance Printable PDF Poster (`AnnualShiftPdfGenerator.kt` & `ShiftCalendarDialog.kt`)**: Vector A4 printable 12-month calendar poster with color-coded day grids (Worked, Overtime, Off), monthly worked days and overtime volume, and annual gross payroll summaries.
- **Comprehensive v10.0 Test Battery (`TaxCalculatorTest.kt`)**: Automated unit test coverage for dynamic HMRC JSON serialization/deserialization, live FX fallback resiliency, and expanded 1.0x–3.0x overtime multiplier calculations.

### Bugs Found & Fixed
- **AutoMirrored Notes Icon**: Migrated deprecated `Icons.Default.Notes` to `Icons.AutoMirrored.Filled.Notes`.
- **Clean Method Signatures**: Standardized `P60Generator.generateP60Pdf` and `Sa100Generator.generatePdf` invocations in `TaxPackZipExporter.kt`.

### What Needs to Be Fixed / Pending
- Direct export of annual tax packs to cloud storage endpoints.
- OCR camera scanner for printed paper payslips.

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
