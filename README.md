# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

## Features

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