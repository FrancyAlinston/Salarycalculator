# Salary Calculator - Agent Working Rules & Standards

These rules define the mandatory behavioral constraints, development workflows, domain accuracy requirements, version tracking rules, gap analysis standards, execution ID tagging rules, and verification standards for AI agents operating within the **Salary Calculator** repository.

---

## 1. Documentation & Architecture Compliance

- **Living Documentation**:
  - Whenever you add a new screen, modify tax computation formulas, adjust Gradle dependencies, or alter navigation routes, you MUST update [`README.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/README.md), [`CHANGELOG.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/CHANGELOG.md), and [`AGENTS.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/AGENTS.md).
  - Never leave documentation, version catalogs, or changelogs out of sync with actual codebase implementations.
- **Dependency & Build Integrity**:
  - All library dependencies and plugins MUST be declared and managed centrally in the Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///home/d3fault/Documents/Projects/Salarycalculator/gradle/libs.versions.toml)). Do not hardcode version strings inside `build.gradle.kts`.
  - Always maintain Unix (LF) line terminators on scripts such as [`gradlew`](file:///home/d3fault/Documents/Projects/Salarycalculator/gradlew).

---

## 2. Business Logic & Tax Engine Strictness

- **Calculation Sequence Strictness**:
  When computing payslips or modifying [`TaxCalculator.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/TaxCalculator.kt), you must strictly follow this execution order:
  $$\text{Hours \& Overtime} \longrightarrow \text{Gross Pay} \longrightarrow \text{Pre-Tax Deductions / Sacrifice} \longrightarrow \text{Pension Relief} \longrightarrow \text{Tax-Free Allowances} \longrightarrow \text{Taxable Income} \longrightarrow \text{PAYE Tax} \longrightarrow \text{Class 1 NI} \longrightarrow \text{Student Loan} \longrightarrow \text{Post-Tax Deductions} \longrightarrow \text{Net Pay}$$

- **Mandatory Tax & NI Rules (UK Standard 2024/2025 & 2025/2026)**:
  - **Tax Code Parsing**: Standard codes (e.g., `1257L`) parse numeric values multiplied by 10 (e.g., £12,570/yr, £1,047.50/mo). If no valid tax code is provided, default to standard `1257L`.
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
  - Mark core tax computation logic with `// CRITICAL: TAX_ENGINE`
  - Mark DataStore persistence operations with `// CRITICAL: DATASTORE_PERSISTENCE`
  - Mark edge-case allowances or override handlers with `// EDGE_CASE:`
  - Document any non-standard tax code handling with `// RULE VIOLATION: NON_STANDARD_CODE`

---

## 3. UI, Navigation & State Management Rules

- **Navigation Architecture**:
  - Use **AndroidX Navigation 3** with serializable `NavKey` definitions in [`NavigationKeys.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/NavigationKeys.kt).
  - Manage navigation state via `rememberNavBackStack` and `NavDisplay` within [`Navigation.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/Navigation.kt).
  - Support **Book-Style Foldable Dual-Screen Layout** (`maxWidth >= 720dp`) with Left Pane = Fullscreen Main Calculator and Right Pane = Fullscreen History/Settings companion workspace.
- **Jetpack Compose Guidelines**:
  - Always support **Edge-to-Edge** rendering (`enableEdgeToEdge()` in `MainActivity.kt`) and observe `Scaffold` inner padding across all screen composables.
  - Use Material 3 theming tokens ([`theme/Theme.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/theme/Theme.kt)) and avoid hardcoded colors. Support dynamic colors on Android 12+.
  - State hoisting: Screens should collect state using `collectAsState()` or `collectAsStateWithLifecycle()` from repository flows or ViewModels.
- **Data Persistence**:
  - Always persist user settings (custom tax code, default hourly rate, theme mode, employer profiles, statutory reliefs) using Jetpack DataStore Preferences via [`SalaryRepository.kt`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/src/main/java/com/example/salarycalculator/domain/SalaryRepository.kt).

---

## 4. Version Control, Build, Release & Tracking Automation

- **Automated Commit & Push on Every Change (`@rules:auto_git_sync`)**:
  - Whenever any new change, feature, bugfix, or update is detected/completed, the agent MUST automatically stage all changes (`git add .`), create a semantic and descriptive commit (`git commit -m "..."`), and push immediately to both GitHub and Forgejo (`git push origin <branch>` with dual push URLs configured or pushing to both remotes).
- **Version Tracking & Changelog Integrity (`@rules:version_changelog_tracking`)**:
  - For EVERY version change, the agent MUST maintain a structured, up-to-date entry in [`CHANGELOG.md`](file:///home/d3fault/Documents/Projects/Salarycalculator/CHANGELOG.md).
  - Every version log MUST categorically document:
    1. **Version Header**: `[VersionName] - YYYY-MM-DD (VersionCode: N)`
    2. **Added / Changed**: Detailed explanation of all new features and UI/architectural modifications.
    3. **Bugs Found & Fixed**: Comprehensive list of bugs, glitches, deprecations, or build issues identified and fixed.
    4. **What Needs to Be Fixed / Pending**: Unresolved issues, upcoming statutory additions, or improvements scheduled for subsequent versions.
  - Never bump version codes in `app/build.gradle.kts` without adding a corresponding entry to `CHANGELOG.md`.
- **Version Bump Compliance**:
  - When introducing user-facing features, schema modifications, or calculation updates, increment `versionCode` and update `versionName` in [`app/build.gradle.kts`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/build.gradle.kts).
- **Keystore & Signing Integrity**:
  - Maintain signing config referencing [`app/debug.keystore`](file:///home/d3fault/Documents/Projects/Salarycalculator/app/debug.keystore) to ensure compatibility with automated release workflows.
- **GitHub & Forgejo Actions Storage Limits & Release Backtracking (`@rules:github_upload_storage_limits_management`)**:
  - On every change or build push for release, the agent MUST consider GitHub and Forgejo Actions upload/artifact storage quotas.
  - Never allow intermediate workflow artifact upload steps (`actions/upload-artifact`) to block or fail release publishing (`softprops/action-gh-release`), because Releases utilize independent, uncapped release asset storage.
  - If a storage quota or recalculation window (6-12 hours) prevents immediate artifact staging, the agent MUST bypass ephemeral workflow artifact uploads and attach production and debug APKs directly to the Release.
  - The agent MUST backtrack across past versions (from the last successful release up to the current active version) to verify all intervening releases are properly tagged, built, and published.
- **Local Versioned APK Archive Directory (`@rules:local_versioned_apks_folder`)**:
  - The repository maintains a local folder named `APKs` at the workspace root.
  - This folder MUST be included in `.gitignore` and used strictly for storing built `.apk` binaries across all app versions.
  - On every build or version release, the agent MUST automatically copy the newly generated release and debug APKs into `APKs/` named with their semantic version: e.g. `Salarycalculator-v<versionName>.apk` and `Salarycalculator-v<versionName>-debug.apk`.
  - Only `.apk` files with version names are permitted in this folder.
- **Mandatory Dual-Platform Release on Both Git and Forgejo (`@rules:Always release on both git and forgejo`)**:
  - On **EVERY** version release or build update, the agent MUST publish, tag, and synchronize the release to **BOTH** GitHub and Forgejo.
  - **Git Remotes Synchronization**: Always push all commits and semantic tags to both primary Forgejo (`origin`) and secondary GitHub (`github`):
    `git push origin main --tags && git push github main --tags`
  - **Release Assets & Pages Parity**: Verify that release entries exist on both GitHub Releases and Forgejo Releases with corresponding changelog notes and both binaries attached:
    - **Stable / Production APK**: `Salarycalculator.apk` (packaged from `app/build/outputs/apk/release/`).
    - **Debug / Development APK**: `Salarycalculator-debug.apk` (packaged from `app/build/outputs/apk/debug/`).
  - **Automated Forgejo API Release Sync**: Execute `python3 scripts/sync_forgejo_releases.py` on every release to ensure immediate API publishing of release pages and APK assets directly to Forgejo (`https://forgejo.449100.xyz`), guaranteeing 100% release parity regardless of runner availability.
- **Automated CI/CD**:
  - Releases are automatically generated via GitHub Actions ([`.github/workflows/release.yml`](file:///home/d3fault/Documents/Projects/Salarycalculator/.github/workflows/release.yml)) and Forgejo Actions ([`.forgejo/workflows/release.yaml`](file:///home/d3fault/Documents/Projects/Salarycalculator/.forgejo/workflows/release.yaml)) on push to `main` with semantic tags `v*`.
  - In installed environments, the app launcher display name MUST remain strictly the project name (`Salary Calculator`).

---

## 5. Testing & Verification Standards (`@rules:mandatory_post_implementation_testing_and_full_verification`)

- **Mandatory Emulator & Live UI Verification (`@rules:mandatory_emulator_verification_and_ui_inspection`)**:
  - Whenever changes are made, the agent MUST install and launch the build on the Android emulator (`adb install -r ...` and `adb shell am start ...`).
  - The agent MUST actively inspect the live running app for glitches, alignment issues, layout overflow, truncated text, touch target clipping, and runtime logcat exceptions.
  - Verify that screens render cleanly in both portrait and landscape/foldable modes with edge-to-edge system insets properly observed.
- **Mandatory Emulator Teardown on Completion (`@rules:emulator_auto_close_on_completion`)**:
  - Once live UI inspection and emulator testing are completed, the agent MUST immediately close the emulator (`adb emu kill` or kill the emulator process) and never leave it running in the background.
- **Post-Implementation Functional Testing**:
  - Upon finishing any new implementation, bugfix, or refactoring, the agent MUST always test that the changes work exactly as intended across all screen orientations and states.
- **Periodic Full-Battery Tax Engine Verification**:
  - The agent MUST periodically run a comprehensive test suite across varying income brackets, wage amounts, overtime multipliers (`1.0x`, `1.5x`, `2.0x`), pension rates (`0%` to `15%`), salary sacrifice schemes, student loan plans (Plans 1, 2, 4, Postgraduate), and statutory allowances (Standard, Marriage Allowance, Blind Person's Allowance) to assert zero arithmetic drift and exact penny accuracy.
- **Automated Unit & Build Verification**:
  - Always verify with `./gradlew test assembleDebug` before committing releases.

---

## 6. Gap Analysis & Task Completion Reporting Standard (`@rules:gap_analysis_and_opportunity_reporting`)

On **EVERY** change or task completed, the agent MUST generate a structured **End-of-Task (EOT)** report adhering to the following categorized schema:

1. **Task Summary**: Concise summary of what was requested and the technical actions performed.
2. **Issues**: Any blockers, bugs, or inconsistencies discovered during execution.
3. **Improvements**: Architectural, performance, or UX enhancements introduced.
4. **Concerns**: Potential edge cases, tax legislation ambiguities, or platform constraints.
5. **Optimizations**: Code refactoring, memory, or state-efficiency gains.
6. **Alerts**: Breaking changes, required manual configurations, or signing notes.
7. **Comprehensive Gap Analysis & Roadmap**:
   - **Areas That Needed Work**: Technical debt, missing test coverage, code structure improvements, performance bottlenecks, or limitations identified in the existing codebase.
   - **New Features & Improvements That Can Be Implemented**: Concrete, high-value opportunities and upcoming feature suggestions.
   - **Unworked / Pending Areas**: Features, screens, domain modules, or integrations that have not yet been touched or remain in a baseline state.

---

## 7. Action & Execution ID Tagging Standard (`@rules:task_execution_id_tagging`)

To enable seamless user triggering and 1-message delegation, the agent MUST adhere to the following tagging standard on **EVERY** End-of-Task report:

- **Master Execution ID**: Every report MUST define a top-level **Action ID / Execution ID** (e.g., `EOT-EXEC-V4.1`, `EOT-EXEC-V5.0`) at the beginning and in the Roadmap section.
- **Feature Action IDs**: Every recommended feature, unworked area, and improvement item in the report MUST be assigned a unique sub-ID (e.g., `[ACTION: FEAT-501]`, `[ACTION: FEAT-502]`, `[ACTION: FIX-501]`).
- **1-Message Trigger Support**: When the user references any Execution ID or Feature Action ID (e.g., *"implement EOT-EXEC-V5.0"* or *"work on FEAT-501"*), the agent MUST immediately parse the corresponding scope, construct the implementation plan, and execute all listed items without requiring manual re-prompting.
