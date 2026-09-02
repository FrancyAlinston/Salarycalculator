# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

## Features

- **UK PAYE Tax Engine (2024/2025):** Computes tax bands (Basic 20%, Higher 40%, Additional 45%) and Class 1 Primary National Insurance (8% and 2% rates).
- **Tax Code Parsing:** Dynamically calculates personal allowances from standard UK tax codes (e.g., `1257L` $\rightarrow$ £12,570/yr) and custom codes.
- **Hourly & Overtime Pay Breakdown:** Calculates gross pay from days worked, standard daily hours, and overtime hours.
- **Persistent User Settings:** Saves default hourly rate and tax code preferences using Jetpack DataStore Preferences.
- **Modern Jetpack Compose UI:** Material 3 design system with dynamic theming and AndroidX Navigation 3.

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