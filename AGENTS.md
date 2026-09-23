# Salary Calculator - Agent Working Rules & Standards

These rules define the mandatory behavioral constraints, development workflows, domain accuracy requirements, version tracking rules, gap analysis standards, execution ID tagging rules, and verification standards for AI agents operating within the **Salary Calculator** repository.

---

## 1. Documentation, Architecture & Rule Hygiene

- **Rule Conciseness & Deduplication (`@rules:agents_md_conciseness_and_hygiene`)**:
  - `AGENTS.md` MUST remain lean, dense, modular, and strictly focused on active, actionable agent rules and behavioral constraints.
  - Never duplicate instructions or rules across multiple sections. Overlapping directives must be merged into their canonical domain section.
  - Never use machine-specific absolute paths (e.g. `/home/d3fault/...`); always use clean repository-relative paths or standard workspace symbols.
  - Prune obsolete references immediately whenever architecture or workflows evolve.
- **Changelog Segregation Rule (`@rules:changelog_in_changelog_only`)**:
  - **ALL** version release notes, changelogs, and release histories MUST be maintained exclusively in [`CHANGELOG.md`](CHANGELOG.md).
  - **NEVER** embed release changelogs, version histories, or release notes inside `AGENTS.md`, `README.md`, or any other operational guide.
- **Living Documentation**:
  - Whenever you add a new screen, modify tax computation formulas, adjust Gradle dependencies, or alter navigation routes, you MUST update [`README.md`](README.md) and [`CHANGELOG.md`](CHANGELOG.md).
- **Dependency & Build Integrity**:
  - All library dependencies and plugins MUST be declared and managed centrally in the Gradle Version Catalog ([`gradle/libs.versions.toml`](gradle/libs.versions.toml)). Do not hardcode version strings inside `build.gradle.kts`.
  - Always maintain Unix (LF) line terminators on scripts such as `gradlew`.

---

## 2. Business Logic & Tax Engine Strictness (`@rules:tax_engine_strictness`)

- **Calculation Sequence Strictness**:
  When computing payslips or modifying [`TaxCalculator.kt`](app/src/main/java/com/example/salarycalculator/domain/TaxCalculator.kt), strictly follow this execution order:
  $$\text{Hours \& Overtime} \longrightarrow \text{Gross Pay} \longrightarrow \text{Pre-Tax Deductions / Sacrifice} \longrightarrow \text{Pension Relief} \longrightarrow \text{Tax-Free Allowances} \longrightarrow \text{Taxable Income} \longrightarrow \text{PAYE Tax} \longrightarrow \text{Class 1 NI} \longrightarrow \text{Student Loan} \longrightarrow \text{Post-Tax Deductions} \longrightarrow \text{Net Pay}$$

- **Mandatory Tax & NI Rules (UK Standard 2024/2025 & 2025/2026)**:
  - **Tax Code Parsing**: Standard codes (e.g., `1257L`) parse numeric values multiplied by 10 (£12,570/yr, £1,047.50/mo). If no valid tax code is provided, default to standard `1257L`.
  - **Income Tax Bands (UK Standard)**:
    - Basic Rate (20%): £0 to £37,700/yr (£3,141.67/mo) taxable income.
    - Higher Rate (40%): £37,700 to £125,140/yr (£3,141.67 to £10,428.33/mo) taxable income.
    - Additional Rate (45%): Taxable income exceeding £125,140/yr (£10,428.33/mo).
  - **Scottish 6-Tier Bands**: Starter 19%, Basic 20%, Intermediate 21%, Higher 42%, Advanced 45%, Top 48%.
  - **National Insurance (Class 1 Primary)**:
    - Below Primary Threshold (£1,048/mo / £12,576/yr): 0% NI.
    - Between Primary Threshold and Upper Earnings Limit (£4,189/mo / £50,268/yr): 8% main rate.
    - Above Upper Earnings Limit (> £4,189/mo): 2% additional rate.
  - **Statutory Reliefs**:
    - Marriage Allowance: £1,260 transferred personal allowance (£21/month tax reduction).
    - Blind Person's Allowance: £3,070 statutory tax-free personal allowance.
  - **Pay Schedule & Cutoff Engine**: Standard monthly pay schedule calculates Pay Day as the Last Friday of the month with Timesheet Cutoff Date as the preceding Sunday at 23:59 ($\text{Last Friday} - 5\text{ days}$). Shifts logged after cutoff date roll over into the subsequent month's payslip.
  - **Zero / Negative Bounds Protection**: Taxable pay and deductions must never result in negative tax amounts or negative net pay calculations. Always clamp minimums with `max(0.0, ...)`.

- **Critical Code Annotations**:
  - `// CRITICAL: TAX_ENGINE` for core calculation formulas.
  - `// CRITICAL: DATASTORE_PERSISTENCE` for DataStore persistence routines.
  - `// EDGE_CASE:` for allowance overrides and edge boundaries.
  - `// RULE VIOLATION: NON_STANDARD_CODE` for non-standard tax code handling.

---

## 3. UI, Navigation & State Management Rules (`@rules:ui_and_state_management`)

- **Navigation Architecture**:
  - Use **AndroidX Navigation 3** with serializable `NavKey` definitions in [`NavigationKeys.kt`](app/src/main/java/com/example/salarycalculator/NavigationKeys.kt).
  - Manage navigation state via `rememberNavBackStack` and `NavDisplay` in [`Navigation.kt`](app/src/main/java/com/example/salarycalculator/Navigation.kt).
  - Support **Book-Style Foldable Dual-Screen Layout** (`maxWidth >= 720dp`) with Left Pane = Fullscreen Main Calculator and Right Pane = Fullscreen History/Settings companion workspace.
- **Jetpack Compose Guidelines**:
  - Always support **Edge-to-Edge** rendering (`enableEdgeToEdge()` in `MainActivity.kt`) and observe `Scaffold` inner padding across all screen composables.
  - Use Material 3 theming tokens ([`theme/Theme.kt`](app/src/main/java/com/example/salarycalculator/theme/Theme.kt)) and avoid hardcoded colors. Support dynamic colors on Android 12+.
  - State hoisting: Screens should collect state using `collectAsState()` or `collectAsStateWithLifecycle()` from repository flows or ViewModels.
- **Data Persistence**:
  - Always persist user settings (custom tax code, default hourly rate, theme mode, employer profiles, statutory reliefs, email memory) using Jetpack DataStore Preferences via [`SalaryRepository.kt`](app/src/main/java/com/example/salarycalculator/domain/SalaryRepository.kt).

---

## 4. Version Control, Build & Dual-Platform Release Pipeline

- **Automated Commit & Push on Every Change (`@rules:auto_git_sync`)**:
  - On any completed change, feature, bugfix, or update, automatically stage all changes (`git add .`), create a semantic commit (`git commit -m "..."`), and push to all active branches (`alpha`, `stable`, `Beta`) on both remotes (`origin` and `github`).
- **Version Tracking & Changelog Integrity (`@rules:version_changelog_tracking`)**:
  - Increment `versionCode` and update `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts) whenever user-facing changes, schema updates, or calculation adjustments occur.
  - Every version change MUST have a structured entry in [`CHANGELOG.md`](CHANGELOG.md) with 4 categorized sections:
    1. **Version Header**: `[VersionName] - YYYY-MM-DD (VersionCode: N)`
    2. **Added / Changed**: Detailed explanation of new features and architectural changes.
    3. **Bugs Found & Fixed**: Bugs, glitches, deprecations, or build issues fixed.
    4. **What Needs to Be Fixed / Pending**: Unresolved issues or upcoming features scheduled.
- **Local Versioned APK Archive Directory (`@rules:local_versioned_apks_folder`)**:
  - The repository maintains an untracked root folder named `APKs/`.
  - On every build or version release, automatically copy newly generated binaries to `APKs/` named with their semantic version:
    - Production / Release APK: `APKs/Salarycalculator-v<versionName>.apk`
    - Development / Debug APK: `APKs/Salarycalculator-v<versionName>-debug.apk`
- **Mandatory Dual-Platform Release on GitHub & Forgejo (`@rules:dual_platform_release`)**:
  - On **EVERY** version release, publish, tag, and synchronize the release to **BOTH** GitHub and Forgejo:
    - **Git Synchronization**: `git push origin alpha Beta stable --tags && git push github alpha Beta stable --tags`
    - **Forgejo API Release Sync**: Execute `python scripts/sync_forgejo_releases.py` to create release pages and upload both versioned APK assets directly to Forgejo (`https://forgejo.449100.xyz`).
    - **Storage Quota Resilience**: If ephemeral CI upload steps (`actions/upload-artifact`) fail due to runner storage limits, attach binaries directly to the Release (`softprops/action-gh-release`), which utilizes independent, uncapped release asset storage.
  - In installed environments, the app launcher display name MUST remain strictly `Salary Calculator`.

---

## 5. Testing, Verification & Cleanup Standards (`@rules:mandatory_testing_and_cleanup`)

- **Mandatory Emulator & Live UI Verification (`@rules:mandatory_emulator_verification`)**:
  - When changes are made, install and launch the build on the Android emulator (`adb install -r ...` and `adb shell am start ...`) when an emulator is available.
  - Inspect the live running app for glitches, alignment issues, layout overflow, truncated text, touch target clipping, and runtime logcat exceptions across portrait and landscape/foldable orientations.
- **Mandatory Emulator Teardown on Completion (`@rules:emulator_auto_close_on_completion`)**:
  - Once live UI inspection is completed, immediately close the emulator (`adb emu kill` or kill process) and never leave it running in the background.
- **Mandatory Screenshot & Temporary Media Cleanup (`@rules:Always delete the screenshots taken from the Project dir after user`)**:
  - Whenever screenshots or media captures are taken during inspection, delete and clean up all generated media files from the repository and workspace root immediately after use.
- **Tax Engine & Build Verification**:
  - Always verify that all unit tests and builds pass before committing releases: `./gradlew test assembleDebug`.
  - Periodically run full-battery verification across varying tax codes, Scottish tiers, student loans, overtime multipliers, and statutory allowances to assert zero arithmetic drift and penny accuracy.

---

## 6. Gap Analysis & Task Completion Reporting Standard (`@rules:gap_analysis_and_opportunity_reporting`)

On **EVERY** completed task or change, generate a structured **End-of-Task (EOT)** report adhering to the following schema:

1. **Task Summary**: Concise summary of what was requested and technical actions performed.
2. **Issues**: Blockers, bugs, or inconsistencies discovered during execution.
3. **Improvements**: Architectural, performance, or UX enhancements introduced.
4. **Concerns**: Potential edge cases, tax legislation ambiguities, or platform constraints.
5. **Optimizations**: Code refactoring, memory, or state-efficiency gains.
6. **Alerts**: Breaking changes, required manual configurations, or signing notes.
7. **Comprehensive Gap Analysis & Roadmap**:
   - **Areas That Needed Work**: Technical debt, missing test coverage, code structure improvements, performance bottlenecks, or limitations identified.
   - **New Features & Improvements That Can Be Implemented**: High-value opportunities and upcoming feature suggestions.
   - **Unworked / Pending Areas**: Features, screens, domain modules, or integrations not yet touched or in baseline state.

---

## 7. Action & Execution ID Tagging Standard (`@rules:task_execution_id_tagging`)

To enable seamless user triggering and 1-message delegation:

- **Master Execution ID**: Every report MUST define a top-level **Action ID / Execution ID** (e.g., `EOT-EXEC-V24.9`, `EOT-EXEC-V25.0`) at the beginning and in the Roadmap section.
- **Feature Action IDs**: Every recommended feature, unworked area, and improvement item in the report MUST be assigned a unique sub-ID (e.g., `[ACTION: FEAT-901]`, `[ACTION: FIX-901]`, `[ACTION: PEND-901]`).
- **1-Message Trigger Support**: When the user references any Execution ID or Feature Action ID (e.g., *"implement EOT-EXEC-V25.0"* or *"work on FEAT-901"*), parse the corresponding scope, construct the implementation plan, and execute all listed items without requiring manual re-prompting.
