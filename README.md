# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

## Features

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
2. Download the latest `Salarycalculator-debug.apk` file.
3. Transfer the APK to your Android device.
4. Tap the APK file to install (make sure you allow "Install from Unknown Sources" if prompted).

---

## Development & Building

### Build Debug APK
```bash
./gradlew assembleDebug
```
The APK is generated at:
```
app/build/outputs/apk/debug/Salarycalculator-debug.apk
```

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