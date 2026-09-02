# Salary Calculator - Agent Working Rules & Standards

These rules define the mandatory behavioral constraints, development workflows, domain accuracy requirements, and reporting standards for AI agents operating within the **Salary Calculator** repository.

---

## 1. Documentation & Architecture Compliance

- **Living Documentation**:
  - Whenever you add a new screen, modify tax computation formulas, adjust Gradle dependencies, or alter navigation routes, you MUST update [`README.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/README.md) and [`AGENTS.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/AGENTS.md).
  - Never leave documentation or version catalogs out of sync with actual codebase implementations.
- **Dependency & Build Integrity**:
  - All library dependencies and plugins MUST be declared and managed centrally in the Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///home/d3fault/Documents/Projects/Salarycalculator/gradle/libs.versions.toml)). Do not hardcode version strings inside `build.gradle.kts`.
  - Always maintain Unix (LF) line terminators on scripts such as [`gradlew`](file:///home/d3fault/Documents/Projects/Salarycalculator/gradlew).

---

## 2. Business Logic & Tax Engine Strictness

- **Calculation Sequence Strictness**:
  When computing payslips or modifying [`TaxCalculator.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/TaxCalculator.kt), you must strictly follow this execution order:
  $$\text{Hours \& Overtime} \longrightarrow \text{Gross Pay} \longrightarrow \text{Tax-Free Allowance Parsing} \longrightarrow \text{Taxable Income} \longrightarrow \text{PAYE Income Tax Bands} \longrightarrow \text{Class 1 National Insurance} \longrightarrow \text{Net Pay}$$

- **Mandatory Tax & NI Rules (UK Standard 2024/2025)**:
  - **Tax Code Parsing**: Standard codes (e.g., `1257L`) parse numeric values multiplied by 10 (e.g., £12,570/yr, £1,047.50/mo). If no valid tax code is provided, default to standard `1257L`.
  - **Income Tax Bands**:
    - Basic Rate (20%): £0 to £37,700/yr (£3,141.67/mo) taxable income.
    - Higher Rate (40%): £37,700 to £125,140/yr (£3,141.67 to £10,428.33/mo) taxable income.
    - Additional Rate (45%): Taxable income exceeding £125,140/yr (£10,428.33/mo).
  - **National Insurance (Class 1 Primary)**:
    - Below Primary Threshold (£1,048/mo / £12,576/yr): 0% NI.
    - Between Primary Threshold and Upper Earnings Limit (£4,189/mo / £50,268/yr): 8% main rate.
    - Above Upper Earnings Limit (> £4,189/mo): 2% additional rate.
  - **Zero / Negative Bounds Protection**: Taxable pay and deductions must never result in negative tax amounts or negative net pay calculations. Always clamp minimums with `max(0.0, ...)`.

- **Critical Code Annotations**:
  - Mark core tax computation logic with `// CRITICAL: TAX_ENGINE`
  - Mark DataStore persistence operations with `// CRITICAL: DATASTORE_PERSISTENCE`
  - Mark edge-case allowances or override handlers with `// EDGE_CASE:`
  - Document any non-standard tax code handling with `// RULE VIOLATION: NON_STANDARD_CODE`

---

## 3. UI, Navigation & State Management Rules

- **Navigation Architecture**:
  - Use **AndroidX Navigation 3** with serializable `NavKey` definitions in [`NavigationKeys.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/NavigationKeys.kt).
  - Manage navigation state via `rememberNavBackStack` and `NavDisplay` within [`Navigation.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/Navigation.kt).
- **Jetpack Compose Guidelines**:
  - Always support **Edge-to-Edge** rendering (`enableEdgeToEdge()` in `MainActivity.kt`) and observe `Scaffold` inner padding across all screen composables.
  - Use Material 3 theming tokens ([`theme/Theme.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/theme/Theme.kt)) and avoid hardcoded colors. Support dynamic colors on Android 12+.
  - State hoisting: Screens should collect state using `collectAsState()` or `collectAsStateWithLifecycle()` from repository flows or ViewModels.
- **Data Persistence**:
  - Always persist user settings (custom tax code, default hourly rate) using Jetpack DataStore Preferences via [`SalaryRepository.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/SalaryRepository.kt).

---

## 4. Version Control, Build & Release Automation

- **Automated Commit & Push on Every Change (`@rules:auto_git_sync`)**:
  - Whenever any new change, feature, bugfix, or update is detected/completed, the agent MUST automatically stage all changes (`git add .`), create a semantic and descriptive commit (`git commit -m "..."`), and push immediately to GitHub (`git push origin <branch>`).
- **Version Bump Compliance**:
  - When introducing user-facing features, schema modifications, or calculation updates, increment `versionCode` and update `versionName` in [`app/build.gradle.kts`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/build.gradle.kts).
- **Keystore & Signing Integrity**:
  - Maintain signing config referencing [`app/debug.keystore`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/debug.keystore) to ensure compatibility with automated release workflows.
- **Automated CI/CD**:
  - Releases are automatically generated via GitHub Actions ([`.github/workflows/release.yml`](file:///home/d3fault/Documents/Projects/Salarycalculator/.github/workflows/release.yml)) on push to `main`.
  - Artifact path rule: APK must always be output to `app/build/outputs/apk/debug/Salarycalculator-debug.apk`.

---

## 5. Testing & Verification Standards

- **Unit Testing**:
  - Every calculation rule change in [`TaxCalculator.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/TaxCalculator.kt) MUST be validated by unit tests in `app/src/test/`.
  - Ensure tests cover basic allowance, higher rate thresholds, additional rate thresholds, overtime calculations, and zero/negative income bounds.
- **UI & Instrumented Testing**:
  - Instrumented Compose tests in `app/src/androidTest/` must reflect valid screen signatures and current Navigation3 destinations.

---

## 6. Task Completion Reporting Standard (EOT Report)

When completing any development task or feature request, you MUST generate a structured **End-of-Task (EOT)** report adhering to the following categorized schema:

1. **Task Summary**: Concise summary of what was requested and the technical actions performed.
2. **Issues**: Any blockers, bugs, or inconsistencies discovered during execution.
3. **Improvements**: Architectural, performance, or UX enhancements introduced.
4. **Concerns**: Potential edge cases, tax legislation ambiguities, or platform constraints.
5. **Optimizations**: Code refactoring, memory, or state-efficiency gains.
6. **Alerts**: Breaking changes, required manual configurations, or signing notes.
7. **Future Roadmap**:
   - **Code**: Architectural refinements, additional unit tests.
   - **UI**: Visual improvements, animations, payslip breakdown visualizations.
   - **Features**: Student loan deductions, pension contributions (auto-enrolment), Scottish tax bands, export to PDF/CSV.
