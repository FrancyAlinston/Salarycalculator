# Changelog

All notable changes to the **Salary Calculator** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### What Needs to Be Fixed / Upcoming
- [ ] Auto-enrolment pension contribution calculations (default 5% employee / 3% employer relief).
- [ ] Student loan repayment plan deduction options (Plan 1, Plan 2, Plan 4, Postgraduate).
- [ ] Scottish Income Tax Bands support (Starter, Basic, Intermediate, Higher, Advanced, Top).
- [ ] PDF and CSV payslip export and sharing.
- [ ] Yearly vs. monthly payslip comparison toggle.

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
- Add unit tests for `ThemeMode` preference serialization.

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
