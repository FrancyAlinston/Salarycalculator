# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

## Features

- **Payroll Pay Schedule & Cutoff Date Engine (`PayScheduleEngine.kt`):** Standard UK company payroll rule calculation with primary default for **Last Friday of the Month (with preceding Sunday timesheet cutoff)**. Exact calculation of monthly timesheet cutoff deadline ($5$ days prior to pay date at 23:59), cycle start dates, and post-cutoff rollover partitioning.
- **Pay Cycle & Cutoff Visualizer in Shift Calendar (`ShiftCalendarDialog.kt`):** Color-coded calendar badges for **Cutoff Day** (amber badge & border) and **Payday** (emerald badge & border), visual rollover indicator (`+Roll`) for post-cutoff shifts, payroll summary banner partitioning in-cycle hours vs rolled-over hours, and 1-tap "Apply Cutoff Hours" selector.
- **Pay Schedule Configuration in Settings (`PayScheduleSettingsDialog.kt` & `SettingsScreen.kt`):** Dedicated interactive configuration modal supporting selectable employer pay rules (*Last Friday of Month*, *Last Working Day*, *Fixed Day of Month with Lead-Time*, *Four-Weekly*, *Bi-Weekly*, *Calendar Month*) with live preview of upcoming pay & cutoff dates.
- **Automated Payday & Cutoff iCalendar Export with Alarms (`IcsCalendarExporter.kt`):** Full-year RFC 5545 `.ics` calendar generation for company paydays and timesheet cutoff deadlines with integrated `VALARM` notifications (cutoff reminder the night before, and morning payday alert with estimated net take-home pay).

- **Self-Employed Payments on Account & Balancing Charge Calculator (`SelfEmployedTaxEngine.kt` & `SelfEmployedTaxDialog.kt`):** HMRC statutory calculations including £1,000 Trading Allowance, Class 4 NI (6% main band / 2% upper band), combined PAYE + Self-Employment assessment, and automatic Payments on Account calculation (50% due 31 January, 50% due 31 July) with first-year cash outlay projections.
- **Gift Aid Tax Relief & Higher-Rate Band Extension Optimizer (`GiftAidOptimizer.kt` & `GiftAidDialog.kt`):** Charitable donations with 25% HMRC basic rate top-up, Higher Rate (40%) and Additional Rate (45%) personal tax reclaim calculations, and basic rate tax band expansion from £37,700.
- **Capital Gains Tax (CGT) Annual Exemption (£3,000) & Asset Disposal Planner (`CapitalGainsTaxEngine.kt` & `CapitalGainsDialog.kt`):** 2024/25 & 2025/26 £3,000 statutory CGT annual exempt amounts, basic rate (10% standard / 18% residential property) and higher rate (20% standard / 24% residential property) tax computations, and taxable income band absorption.
- **Direct PDF/CSV Email Dispatch Intent (`EmailExporter.kt`):** 1-tap email dispatch pre-filling subject, body summary, and attaching monthly PDF payslips, P60 certificates, or CSV reports via Android `FileProvider`.
- **Astronomical Solar Sunset/Sunrise Dynamic Dark Mode (`SolarThemeScheduler.kt`, `Theme.kt`, `SettingsScreen.kt`):** Astronomical sunrise and sunset calculation for UK coordinates to smoothly auto-toggle Material 3 Dark/Light mode at twilight without location permissions.
- **Pension Annual Allowance (£60,000) & Tapering Optimizer (`PensionAllowanceOptimizer.kt` & `PensionAllowanceDialog.kt`):** Statutory UK pension allowance calculations covering £60k annual limit, high-earner tapering (£260k–£360k), Money Purchase Annual Allowance (MPAA £10k), 3-year carry-forward additions, and marginal rate tax charge estimations.
- **Month-over-Month Payslip Variance Heatmap & Diff (`PayslipVarianceDialog.kt`):** Chronological multi-period comparison highlighting month-on-month percentage and pound deltas for gross pay, net pay, PAYE tax, NI, pension, and overtime hours.
- **Student Loan Repayment Horizon & Early Payoff Calculator (`StudentLoanPayoffEngine.kt` & `StudentLoanPayoffDialog.kt`):** Mathematical payoff horizon modeling across Plan 1, Plan 2, Plan 4, and Postgraduate, statutory interest rates, 30-year statutory write-off dates, and voluntary overpayment interest savings.
- **Offline Background Cloud Sync Retry Queue (`SyncQueueManager.kt`):** Persistent queue with automated exponential backoff retry via `WorkManager` for WebDAV, Nextcloud, and private cloud storage uploads.
- **Automated Scheduled ZIP Tax Bundle Archiving (`ScheduledBackupWorker.kt`):** Periodic background worker automatically compiling and archiving encrypted Annual Tax Pack ZIP bundles and payroll records.
- **Android 13+ Material You Monochrome Adaptive Icon:** Dynamic wallpaper-matching themed icon layer (`ic_launcher_monochrome.xml`) conforming to Android 13+ Monet design specifications.
- **ML-Powered Salary & Year-End Tax Forecast Engine (`SalaryForecastEngine.kt` & `SalaryForecastDialog.kt`):** Ordinary Least Squares (OLS) multi-month linear regression and seasonal time-series projection modeling annual gross earnings, R² confidence scoring, trend velocity (+/- £/mo), projected full-year PAYE tax liability vs actual deductions, and automatic detection of HMRC year-end tax rebates or underpayment shortfalls.
- **Selective Biometric Privacy Lock on History Ledger:** Dedicated biometric fingerprint/face authentication gate protecting confidential salary history cards, SA100 returns, and annual tax pack exports while keeping the main calculator instantly accessible.
- **Customizable Material 3 Color Theme Palettes:** 4 curated Material 3 themes (**Ocean Sapphire**, **Emerald Green**, **Midnight Violet**, **Sunset Amber**) with real-time UI switching and DataStore persistence.
- **Interactive Payslip OCR Correction & Re-Calculation Editor:** Visual field-level verification with 1-tap statutory re-calculation helpers before saving scanned physical or PDF payslips into the history ledger.
- **Company Director Dividend vs Salary Optimizer (`DirectorDividendOptimizer.kt` & `DirectorDividendDialog.kt`):** Interactive corporate and personal tax planning engine modeling Corporation Tax (19%–25% + Marginal relief), personal dividend tax bands (8.75%–39.35%), and 4-way comparison matrix displaying annual tax savings and net cash retention.
- **Cloud Drive Direct Export Engine (`CloudDriveExporter.kt` & `CloudDriveExportDialog.kt`):** Upload Annual Tax Pack ZIP bundles, official P60 certificates, HMRC SA100 returns, and CSV payroll files directly to WebDAV, Nextcloud, ownCloud, and REST cloud storage endpoints.
- **Multi-Currency History Ledger Toggle:** Interactive currency selector chips (`GBP £`, `EUR €`, `USD $`) converting all historical records and cumulative statistics in real-time.
- **Expanded Overtime & Standard 1.0x Rate Multipliers:** Full support for standard single rate (`1.0x`), `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.25x`, `2.5x`, and `3.0x` across Weekday, Weekend, and Bank Holiday overtime.
- **Dynamic HMRC Statutory Tax Rate Config (`HmrcRateSyncManager.kt` & `HmrcRateSyncDialog.kt`):** Live remote JSON synchronization engine supporting dynamic statutory personal allowance updates, tax bands, NI thresholds, and UK National Living Wage rates (£12.21 21+, £10.00 18-20, £7.55 Apprentice).
- **Live Foreign Exchange Cloud Sync Engine (`LiveFxSyncEngine.kt`):** Automated real-time synchronization of open exchange rates for GBP $\rightarrow$ EUR and GBP $\rightarrow$ USD with offline fallback caches.
- **Annual Tax Pack One-Click ZIP Bundle (`TaxPackZipExporter.kt` & `TaxPackExportDialog.kt`):** 1-tap package compiler bundling P60 PDF, HMRC SA100 return PDF, 12-month shift `.ics` calendar, `.csv` payroll ledger, and printable shift calendar poster into a single `.zip` archive.
- **12-Month Shift Year-at-a-Glance Printable PDF Poster (`AnnualShiftPdfGenerator.kt`):** Vector A4 printable 12-month calendar poster with color-coded day grids (Worked, Overtime, Off), monthly worked days and overtime volume, and annual gross payroll summaries.
- **Company Car Benefit-in-Kind (BiK) & Fuel Tax Calculator:** Interactive calculation engine modeling UK HMRC BiK percentages (2% Pure EV, 2%–14% PHEV, 15%–37% ICE, +4% diesel surcharge), £27,800 private fuel benefit statutory charge, and exact net monthly take-home reduction across 20% Basic, 40% Higher, and 45% Additional tax bands.
- **Statutory Sick Pay (SSP) & Parental Leave (SMP / SPP) Modeling:** Detailed simulation of UK statutory wage entitlements including SSP (£116.75/wk with 3-day waiting rule), SMP (6 weeks at 90% AWE + 33 weeks standard £184.03/wk), SPP, and net take-home comparisons against regular working pay.
- **Mortgage & Loan Borrowing Capacity Estimator:** Lender borrowing power modeling using 4.0x–5.0x salary multiples, cash deposit sizing, existing debts, stress-tested monthly loan amortization formula ($M = P \frac{r(1+r)^n}{(1+r)^n - 1}$), LTV %, and net disposable affordability health rating.
- **Bank Statement CSV Payroll Reconciliation Engine:** Automated import, parsing, and reconciliation of bank statement CSV deposits against recorded payslips with automatic credit identification, exact net match assertion, and variance detection.
- **Annual 12-Month Shift & Overtime Heatmap Planner:** Full-year multi-month schedule management across all 12 months with persistent month-by-month working days and OT customization, annual aggregate summary metrics, quick-fill presets, and 12-month RFC 5545 `.ics` iCalendar export.
- **Multi-Year Statutory Tax Comparison Matrix:** Interactive comparative analysis engine and custom Canvas bar chart comparing net take-home, PAYE tax, and Class 1 NI across 2023/2024 (12% NI rate), 2024/2025 (8% NI rate cut), and 2025/2026 statutory regimes with annual savings badges.
- **HMRC Self-Assessment (SA100 / SA102) Formatter & PDF Exporter:** Automatic mapping of payroll records and live calculations to official HMRC employment return box numbers (Boxes 1–7) with 1-tap A4 vector PDF generation and system sharing via Android `FileProvider`.
- **Mid-Year Tax Code Refund & Rebate Estimator:** Interactive cumulative PAYE refund model calculating one-off payslip refunds and monthly take-home increases when transitioning from emergency tax codes (`BR`, `0T`, `D0`) to standard allowances (`1257L`, `1383M`).
- **Multi-Tier Weekend & Bank Holiday Overtime Rates:** Dedicated configuration chips and persistence for Weekday (`1.0x`–`1.5x`), Weekend (`1.5x`–`2.25x`), and Bank Holiday (`2.0x`–`3.0x`) overtime multipliers.
- **Scheduled Background Cloud Auto-Sync:** Automated 24-hour background ledger backup sync to your self-hosted private domain via Android `WorkManager` with network and battery constraints.
- **Export Shift Calendar to iCalendar (.ics):** 1-tap export of logged monthly shift calendars and overtime shifts to RFC 5545 compliant `.ics` files importable directly into Google Calendar, Apple Calendar, and Outlook.
- **60% Marginal Tax Trap & Pension Sacrifice Visualizer:** Dynamic visual tool analyzing personal allowance tapering between £100,000 and £125,140 with interactive pension salary sacrifice remedy modeling.
- **Custom Foreign Exchange Rates Engine:** Configurable EUR (€) and USD ($) conversion rates with 1-tap presets and live take-home pay conversion.
- **Biometric Auto-Lock Delay Customization:** Configurable auto-lock delay (Immediate, 1 min, 5 min, 15 min) for biometric fingerprint / face authentication.
- **Private Domain / Custom Cloud Save & Sync:** Connect to your self-hosted server, private domain (e.g. Nextcloud, WebDAV, or custom REST API) with optional Bearer Token / API Key auth for 1-tap backup push & restore.
- **Interactive Shift Calendar & Overtime Heatmap:** Visual 30-day interactive calendar modal with color-coded shift heatmaps (Day Off, 8h Regular, 10h Overtime) and 1-tap calculator sync.
- **UK Tax Code Allowance Explainer:** Interactive education modal breaking down personal allowance calculation rules for standard UK (`1257L`), secondary job (`BR`, `0T`, `D0`, `D1`), and Marriage allowance (`M`, `N`) codes.
- **Bonus & Commission Variable Earnings Engine:** Dedicated bonus and commission input fields in the Live Calculator calculating variable gross and marginal PAYE/NI tax rates.
- **Real-Time Multi-Currency Converter:** Live estimated take-home conversions in EUR (€) and USD ($) rendered directly in the net pay hero card.
- **Full Ledger JSON Backup & Restore:** 1-tap backup export and restore of all recorded payslips, employer profiles, custom deductions, and settings via Android Sharesheet and JSON data import.
- **Direct Shift Timesheet Stopwatch & Punch Clock:** Real-time punch-in / punch-out stopwatch that records shift timestamps and automatically transfers accumulated days and hours into the salary calculator.
- **Biometric & Device PIN Privacy App Lock:** Hardware-backed fingerprint, face unlock, and device credential security protecting confidential financial records.
- **High Income Child Benefit Charge (HICBC) Calculator:** Interactive modal calculating statutory 2024/2025 child benefit entitlement, £60,000–£80,000 taper clawback percentage (1% per £200), and HMRC tax charges.
- **Official Annual P60 Certificate Generator:** 1-tap generation of HMRC-styled A4 vector PDF P60 End-of-Year certificates aggregating all recorded months in a tax year.
- **Multiple Job & Employer Profiles:** Manage separate primary, secondary, and freelance employment profiles with dedicated tax codes, hourly rates, and pension schemes.
- **1-Tap Profile Switcher:** Fast switching between employment profiles directly from the Calculator header or Preferences.
- **Marriage Allowance Statutory Relief:** Transferred £1,260 personal allowance between spouses, saving up to £252/year in PAYE tax.
- **Blind Person's Allowance:** Statutory £3,070/year additional tax-free personal allowance.
- **Custom Recurring Deductions:** Support for pre-tax and post-tax custom deductions (Trade Union dues, Professional Subscriptions, Healthcare).
- **Tax Year Framework Selector:** Support for both 2024/2025 and 2025/2026 statutory HMRC tax thresholds.
- **Direct Vector PDF Payslip Export:** 1-tap generation of official A4 payslips with company/employee tables, statutory tax breakdowns, and FileProvider sharing.
- **CSV Timesheet & Ledger Exporter:** Export complete monthly history into spreadsheet `.csv` files for Excel and Google Sheets.
- **Side-by-Side Month Diff & Variance Tool:** Compare any two historical months with color-coded delta indicators and percentage variance.
- **Interactive Earnings Trend Canvas Chart:** Custom Canvas bar visualizer in History displaying Gross vs Net earnings across the timeline.
- **Salary Sacrifice Schemes:** Pre-tax relief calculation for Cycle to Work and Electric Vehicle (EV) salary sacrifice arrangements.
- **Book-Style Foldable Dual-Screen Layout:** Responsive architecture optimized for unfolded book-style foldables (Galaxy Z Fold, Pixel Fold) and tablets.
- **Monthly Salary History & Earnings Ledger:** Save calculated monthly payslips with custom notes, view cumulative total take-home / tax paid, and review itemized past payslips in a dedicated History screen.
- **UK & Scottish PAYE Tax Engines (2024/2025):** Full support for England/Wales/NI (20%, 40%, 45%) and Scottish 6-tier system (Starter 19%, Basic 20%, Intermediate 21%, Higher 42%, Advanced 45%, Top 48%).
- **Workplace Auto-Enrolment Pension:** Configurable employee contribution (default 5%) with **Net Pay Arrangement** upfront tax relief and 3% employer contribution tracking.
- **UK Student Loan Repayments:** Full calculation for Plan 1, Plan 2, Plan 4 (Scotland), and Postgraduate loan thresholds.
- **Pay Frequency Switcher & Matrix:** Instant conversion across **Monthly**, **Weekly**, **Annual**, and **Hourly** views with full multi-period comparison tables.
- **Overtime Multipliers & Schedule Presets:** Configurable overtime rates (`1.0x`, `1.5x`, `2.0x`) and 1-tap schedule chips (20d, 21.7d, 16d).
- **Tax Code Parsing:** Dynamically calculates personal allowances from standard UK codes (`1257L`) and non-standard codes (`BR`, `0T`, `D0`, `D1`).
- **Deep Slate Dark & Light Themes:** Material 3 theme switcher (System Default, Light, Dark) with edge-to-edge transparent system bars.
- **Material You Adaptive App Icon:** Custom vector icon featuring British Pound (£) currency branding and Android 13+ monochrome wallpaper theming.
- **Micro-Animations & Visual Distribution:** Spring-animated multi-segment breakdown bar, numerical slide-fade transitions, and expandable rows.
- **In-App Release Notes & Share Sheet:** 1-tap payslip export via native Android share sheet and in-app Changelog modal.
- **Persistent User Settings:** Saves tax code, region, pension rate, student loan plan, hourly rate, and theme mode via Jetpack DataStore Preferences.

---

## Installation

1. Go to the **Releases** section on this GitHub page.
2. Download either:
   - **Stable Version (Recommended)**: `Salarycalculator.apk` (Production signed standalone build).
   - **Debug Version**: `Salarycalculator-debug.apk` (Development build).
3. Transfer the APK to your Android device.
4. Tap the APK file to install (make sure you allow "Install from Unknown Sources" if prompted).

---

## Development & Building

### Build Stable (Release) and Debug APKs
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Stable Release APK
./gradlew assembleRelease
```
The APKs are generated at:
- Stable APK: `app/build/outputs/apk/release/Salarycalculator-release.apk` (or `Salarycalculator.apk`)
- Debug APK: `app/build/outputs/apk/debug/Salarycalculator-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```

---

## Tech Stack

- **Language:** Kotlin 2.1.20 (JVM Toolchain 17)
- **UI Toolkit:** Jetpack Compose (BOM 2026.03.01) + Material 3
- **Navigation:** AndroidX Navigation 3 (`1.0.1`)
- **Data Persistence:** Jetpack DataStore Preferences (`1.1.1`)
- **Build System:** Gradle 9.0.1 with Android Gradle Plugin (`9.0.1`) & Version Catalogs (`libs.versions.toml`)