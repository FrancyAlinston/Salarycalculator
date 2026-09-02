# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

## Features

- **UK PAYE Tax Engine (2024/2025):** Computes tax bands (Basic 20%, Higher 40%, Additional 45%) and Class 1 Primary National Insurance (8% and 2% rates).
- **Tax Code Parsing:** Dynamically calculates personal allowances from standard UK tax codes (e.g., `1257L` $\rightarrow$ £12,570/yr) and non-standard codes (`BR`, `0T`, `D0`, `D1`).
- **Hourly & Overtime Pay Breakdown:** Calculates gross pay from days worked, standard daily hours, and overtime hours.
- **Deep Slate Dark & Light Themes:** Dedicated Material 3 theme switcher (System Default, Light, Dark) with edge-to-edge transparent system bars.
- **Material You Adaptive App Icon:** Custom Material 3 vector icon featuring British Pound (£) currency styling, rich gradient depth, and Android 13+ dynamic monochrome wallpaper theming.
- **Micro-Animations & Visual Breakdown:** Animated proportional progress bar (Take-Home %, PAYE Tax %, NI %), smooth number slide-fades, and expandable rows.
- **Quick-Select Presets:** One-tap presets for standard UK working schedules (20d, 21.7d, 16d) and National Living Wage benchmark rates.
- **Persistent User Settings:** Saves default hourly rate, tax code, and theme mode preferences using Jetpack DataStore Preferences.
- **Modern Jetpack Compose UI:** AndroidX Navigation 3 with responsive layout hierarchy and zero-stutter calculation memoization.

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