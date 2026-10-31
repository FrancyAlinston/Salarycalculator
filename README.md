# Salary Calculator 💰

A clean, modern Android application for calculating UK net salary, PAYE income tax, and Class 1 National Insurance based on gross income, tax code allowances, hourly rates, and overtime. Built with Kotlin and Jetpack Compose.

---

- **Casio Classic Black Digital Watch Face, Step Counter, Weather & November Calendar Fix (`v27.7`) (`WearCasioWatchFaceService.kt`, `CasioDigitalCanvasRenderer.kt`, `WearTelemetryManager.kt`, `ShiftHeatmapCard.kt`):**
  - **Casio Classic F-91W / G-Shock Black Digital Watch Face & Tile:** Pure black (`#000000`) Super AMOLED digital watch face with beveled 7-segment LCD numerals (`HH:MM:SS`), Cobalt Blue & Gold Casio bezel framing, live **Step Counter** (`Sensor.TYPE_STEP_COUNTER` + 8-segment LCD progress bar), live **Weather** (`?C` + condition + High/Low), **`NIGHT DUTY` / `OFF DUTY`** readout, and a 7-day (`M T W T F S S`) weekly shift matrix.
  - **438x438 Circular Watch UI Optimization (`FEAT-931`, `FEAT-932`, `PEND-926`):** Optimized `WearHeatmapScreen.kt` so all 5 weeks of the month, the month header, and the Today Duty + Steps/Weather pill fit on the Samsung Galaxy Watch 8 Classic screen at once, with live shift progress arc and Ambient AOD support.
  - **November 30-Day Calendar Rollover Fix (`FIX-930`):** Fixed `java.util.Calendar` initialization on the 31st of a month so 30-day months (November, April, June, September) and February always render their exact day count (`30` days for November).

- **Samsung Galaxy Watch 8 Classic Quick Tile & Watch Face Complication (`FEAT-927`, `FEAT-928`) (`WearShiftTileService.kt`, `WearShiftComplicationService.kt`, `WearHeatmapScreen.kt`):**
  - **480x480 Super AMOLED Circular Duty Tile (`FEAT-927`):** Luxury circular chronograph tile with radial Emerald/Teal aura, 60-tick bezel ring, Crescent Moon & Stars emblem, bold **`Night Duty`** / **`Off Duty`** status readout (with no net pay exposed), shift timing subtitle (`20:00 ? 08:00 ? 12h Shift`), and a 7-day `M T W T F S S` mini weekly rota bar.
  - **Multi-Slot Watch Face Complication (`FEAT-928`):** Supports `SHORT_TEXT`, `LONG_TEXT`, `RANGED_VALUE`, and `MONOCHROMATIC_IMAGE` slots showing **`Night Duty`** on shift days and **`Off Duty`** on rest days with 1-tap launch into the Watch Heatmap.
  - **Today Duty Hero Banner on Watch Heatmap:** Glowing top duty status card (`?? Night Duty` / `? Off Duty`) with instant Tile + Complication auto-refresh on every shift toggle.

- **Samsung Galaxy Watch 8 Classic Edition (:wear) (FEAT-925) (WearHeatmapScreen.kt, WearSalaryDetailScreen.kt, WearSalaryRepository.kt):**
  - **Physical Rotating Bezel & Circular 480x480 Super AMOLED Optimization:** Native onRotaryScrollEvent handling for the Samsung Galaxy Watch 8 Classic physical rotating bezel (supporting both Smooth Scroll and Bezel Month Dial modes with haptic feedback) and a circular-safe 172dp 7-column Heatmap card that prevents outer-edge bezel clipping.
  - **12-Month Quick Grid Selector & Payroll Cutoff Rollover Engine:** Tap the month header on either screen to open a 3x4 JAN–DEC picker; calculates exact UK Care Worker payslips using the Last Friday Pay Day & Preceding Sunday Cutoff rollover engine (plus 1-tap toggle between Payslip Cutoff and Full Calendar Month modes).
  - **Pre-Seeded 2026 Rota + Direct UGOS NAS Wi-Fi & Wearable Bluetooth Sync:** Pre-seeded with the 2026 shift schedule, £12.82/hr rate, Annual Leave (A/L), and Sick Leave (SSP), with direct Wi-Fi sync to http://192.168.8.100:8085/api/backup and unified pplicationId Bluetooth Wearable Data Layer sync.

- **Wear OS Smartwatch Companion App (:wear) (FEAT-920) (WearHeatmapScreen.kt, WearSalaryDetailScreen.kt, WearDataSyncService.kt):**
  - **Standalone & Companion Wear OS Experience:** Native Compose for Wear OS application optimized for Samsung Galaxy Watch and Google Pixel Watch round/square displays with pure black AMOLED battery savings.
  - **Heatmap as Main First View:** Opens directly into the shift heatmap with month switcher (< / >), monthly shift count and total hours summary, high-contrast touch day tiles (Night Duty 12h, Overtime, Off, Today ring), and 1-tap shift logging.
  - **Select Month Salary Breakdown:** Dedicated wearable salary screen allowing users to select any month (Jan - Dec) and view Take-Home Net Pay, Gross Earnings, Total Hours Worked, Shift Count, Deductions (PAYE Tax, Class 1 NI, Pension), and Pay Day/Cutoff dates.
  - **On-Wrist Hourly Rate Steppers & Google Play Services Sync:** Adjust hourly rate directly on the watch, with automatic background synchronization via Google Play Services Wearable Data Layer (/salary_sync/shifts).

- **Bidirectional Dynamic Month Synchronization & Hero Month Switcher (`FEAT-959`) (`web/index.html`, `web/js/app.js`, `web/js/heatmap.js`):**
  - **100% Guaranteed Month Synchronization:** Interactive month switcher in the Hero Net Pay card (`◀ [📅 Month Year] ▶ [Today]`) and the Shift Heatmap calendar are bidirectionally locked. Changing months anywhere immediately updates the entire suite. Defaults strictly to current calendar month on launch.

- **Full Shift, Overtime, Annual Leave & Sick Pay Settings Suite (`FEAT-958`) (`web/index.html`, `web/js/app.js`, `web/js/store.js`):**
  - **100% Android APK Settings Parity:** Configurable standard shift duration (12.0h, 7.5h, 8.0h, 10.0h), weekday/weekend/bank-holiday overtime multipliers (1.0x–3.0x), weekly overtime thresholds (37.5h, 40.0h, 48.0h), Annual Leave (A/L) daily hours & custom rates, and Statutory Sick Pay (SSP) daily rates & waiting days.
  - **Instant Bidirectional Cloud Persistence:** Real-time synchronization to local storage and NAS SQLite `/api/backup`.

- **Active Month Visual Badges on Calculator & Audit Suite (`FEAT-957`) (`web/index.html`, `web/js/app.js`):**
  - **Hero Take-Home Pay Month Badge:** Displays prominent cyan glass badge (`#res-target-month-badge`, e.g., `📅 August 2026`) in the Hero Net Pay card and monthly frequency subtext (`/ August`) showing exactly which roster month is actively evaluated.
  - **Dynamic Payslip & Configurator Month Headers:** Payslip Audit breakdown card and Hours Configurator headers reflect the active target month in real-time as users navigate the calendar.

- **Dynamic Month-Driven Roster & Payroll Recalculation Engine (`FEAT-956`) (`web/js/heatmap.js`, `web/js/app.js`):**
  - **Dynamic Heatmap Month-Linked Estimation:** Navigating across months in the Shift Heatmap calendar (clicking ◀ Prev, Next ▶, Today, or month jumps) immediately recalculates the calculator's estimated net pay, gross monthly income, tax & NI deductions, and standard/overtime hours to match that selected month's shifts and duty hours.
  - **Context-Aware History Ledger Recording:** History ledgers saved from the calculator automatically record the month label and timestamp corresponding to the active heatmap month.
  - **Live UI Input & Label Synchronization:** Updates the interactive hours inputs and readout badges in real-time as shift duty counts change.

- **Zero-Latency Roster & Payroll Data Seeding (`FEAT-955`) (`web/js/store.js`, `web/js/heatmap.js`, `web/js/app.js`, `web/index.html`):**
  - **Embedded Baseline 2026 Shift Schedule & Payslip History:** Embedded the full 2026 duty roster (Feb–Sep) and August 2026 payslip directly in the client store, ensuring immediate, zero-latency visual rendering of all duty shifts, annual leave, and history ledgers.
  - **Dual-Container Heatmap Display:** Embedded the interactive Roster Heatmap directly inside the main Calculator screen below earnings configurators and in the dedicated Shift Heatmap tab.
  - **Silent Background Sync:** Removed obstructive pop-up confirmation modals in favor of seamless background auto-import from NAS `/api/backup`.

- **Bidirectional Real-Time Auto-Sync Engine (`FEAT-952`) (`web/js/app.js`, `web/js/store.js`, `web/js/heatmap.js`, `AutoBackupObserver.kt`, `CustomCloudSyncManager.kt`):**
  - **100% Web & Mobile Feature Parity:** On login, the web suite automatically synchronizes and renders all data from the mobile APK backup: full 7-column shift heatmap calendar across all 2026 months (Feb through Sep with Emerald duty shifts, Blue A/L annual leave, Amber OT, Purple SSP), monthly payroll history records in "History & P60", custom employer profiles, and statutory rates (£12.82/hr).
  - **Instant Bidirectional Synchronization:** Any shift cycle, input modification, or record save on the web immediately syncs to `/api/backup` on the NAS SQLite server. Android APK's `AutoBackupObserver` automatically syncs local mutations back to NAS.
  - **10-Second Background Polling:** Web client polls `/api/backup` every 10 seconds to update changes made from the mobile phone in real-time without manual page reloads.

- **Mandatory Google Authentication Gatekeeper on Web (`FEAT-951`) (`web/index.html`, `web/js/auth.js`, `web/js/app.js`):**
  - **Zero Data Exposure on First Load:** Gated the entire web application behind a full-screen mandatory Google authentication gatekeeper matching the mobile app's security architecture (`FEAT-934`). All salary calculations, confidential tax codes, duty shift rosters, and navigation bars remain completely hidden until verified.
  - **Instant Multi-Device Cloud & NAS Restore:** Automatically queries Google Drive AppData and local NAS SQLite storage (`/api/backup`) upon login to offer 1-tap instant restoration of shift rosters and payroll logs.
  - **1-Tap Lockout & Sign Out:** Dedicated lock button in the top bar to instantly clear active sessions and return to the secure gatekeeper screen.

- **UGOS Docker Management App & NAS Deployment (`FEAT-947`) (`Dockerfile`, `docker-compose.yml`, `server.py`):**
  - **UGREEN NAS UGOS Docker App Integration:** Pre-configured `docker-compose.yml` and `Dockerfile` matching the container patterns established in the IQVIA project for 1-click container management on UGREEN NAS.
  - **Zero-Latency Container Runtime:** Lightweight Python 3.11 container (`salarycalculator-web`) running at port `8085:8080`, with active container healthchecks (`GET /api/status`), SPA route fallbacks, and persistent storage bindings (`./data:/app/data` and `./web:/app/web:ro`).
  - **Instant Local Deployment:** Run via `docker compose up -d` or import directly in the UGOS Docker management app.

- **Cloudflare Pages & PWA Edge Optimization (`FEAT-946`) (`web/_headers`, `web/_redirects`, `web/manifest.json`, `web/sw.js`, `wrangler.toml`, `scripts/deploy_cloudflare.sh`):**
  - **Cloudflare Edge Performance & Security:** Optimized with `_headers` configuring HSTS, Content Security Policy for Google Identity Services, and immutable asset caching (`Cache-Control: public, max-age=31536000, immutable`).
  - **Standalone PWA & Offline Service Worker:** Installable as an offline-first Progressive Web App across Windows, macOS, Linux, Android Chrome, and iOS Safari.
  - **1-Click Cloudflare Deployment:** Direct Wrangler configuration (`wrangler.toml`) and deployment script (`scripts/deploy_cloudflare.sh`).

- **Cross-Platform Modern Web Application (`FEAT-945`) (`web/index.html`, `web/js/app.js`, `web/js/taxEngine.js`, `web/js/store.js`, `web/js/heatmap.js`, `web/css/styles.css`):**
  - **Identical Modern UI & Google Sign-In:** Complete web companion matching the Material 3 & glassmorphic dark theme of the Android app, with Google Account login, profile header chip, and instant cloud restore prompt (`FEAT-937`).
  - **Dynamic Screen & Foldable Dual-Pane:** Responsive desktop dual-pane layout, mobile bottom tab bar, smooth view transitions, and theme customizers (Dark Glass, Light, Solar ☀️, Ocean, Emerald, Violet, Amber).
  - **Exact UK Tax Engine & 7-Column Shift Roster:** 100% mathematical parity for UK PAYE, Scottish 6-tier rates, Class 1 NI, Student Loans, Overtime, Annual Leave, Statutory Sick Pay (`SSP`), and 1-tap shift cycling.
  - **100% Android APK Interoperability:** Export and import JSON backup bundles seamlessly across web and mobile.

- **Wear OS Companion & Rich Shift Notification Integration (`FEAT-941` / `PEND-901`) (`WearShiftNotificationManager.kt`, `WearShiftActionReceiver.kt`, `DutyShiftReminderWorker.kt`, `SettingsScreen.kt`):**
  - **Wear OS Extensible Notifications:** Direct pre-shift countdown reminders pushed to connected Wear OS smart watches (Pixel Watch, Samsung Galaxy Watch) and Android heads-up alerts.
  - **1-Tap Watch Quick Actions:** Perform shift updates right from your wrist with actionable notification buttons:
    - `"Log +2h OT"`: Adds overtime directly into today's shift roster.
    - `"Mark Attended"`: Records standard duty shift completion.
    - `"Snooze 15m"`: Postpones reminder alerts.
  - **Configurable Pre-Shift Lead Times:** Set reminder alerts (15m, 30m, 45m, 60m) and preview alerts via the 1-tap **"Send Test Notification to Watch"** button in Settings.

- **Instant Cloud Restore Prompt on First Login (`FEAT-937`) (`GoogleAuthGateScreen.kt`, `GoogleDriveSyncEngine.kt`, `LedgerBackupManager.kt`):**
  - **Automated Remote Backup Detection:** Immediately scans Google Drive AppData folder upon Google authorization for previous backups across all user devices.
  - **1-Tap Cloud Restore:** Allows restoring full payroll history, multi-year shift rosters, statutory rates, and employer profiles before entering the main app.
  - **Start Fresh Option:** Gives users the choice to start anew while safely keeping remote backups intact.

- **Google Account Profile Header Chip & Direct Cloud Sync (`FEAT-935`, `FEAT-936`) (`CalculatorScreen.kt`, `SettingsScreen.kt`, `MainActivity.kt`):**
  - **Top Bar Profile Chip:** Sleek account badge in `AppHeaderSection` showing user avatar, name, and live green connection status dot with direct 1-tap launcher for `GoogleDriveSyncDialog`.
  - **Biometric + Google Hybrid Security Flow:** Fast local biometric unlock paired with encrypted background Google account cloud synchronization.

- **Mandatory Google Authentication Gatekeeper Screen (`FEAT-934`) (`GoogleAuthGateScreen.kt`, `MainActivity.kt`, `SalaryRepository.kt`):**
  - **Full-Screen Security Gate:** Requires mandatory Google Sign-In upon first opening the application before revealing any payroll data, confidential shift rosters, or settings.
  - **Encrypted Cloud Continuity:** Automatically hooks user account credentials (`google_account_email`, `google_account_name`) into private Google Drive AppData synchronization and multi-device calendar features.
  - **Seamless Zero-Friction Auto-Auth:** Automatically recognizes existing authorized device Google accounts on initial launch for instantaneous access.

- **Interactive Android Home Screen Shift Heatmap Widget (`FEAT-926`, `FEAT-927`, `FEAT-930`) (`ShiftHeatmapWidgetProvider.kt`, `ShiftCycleEngine.kt`, `ShiftHeatmapWidgetManager.kt`, `widget_shift_heatmap.xml`):**
  - **Live Full-Size Home Screen Roster Widget:** Direct on-screen 7-column shift heatmap displaying current month scheduled shifts, overtime, holiday days, and sick days, utilizing 100% of the user-configured widget canvas size.
  - **1-Tap Interactive Tap-to-Log Quick Shift Pill:** Tappable "Today" status pill in the widget header enables direct 1-tap shift cycling (`Off` $\to$ `Standard Shift` $\to$ `Overtime` $\to$ `Annual Leave` $\to$ `Sick Pay SSP` $\to$ `Off`) without opening the app, accompanied by instantaneous toast notifications and automatic gross salary recalculation.
  - **Dynamic High-DPI Heatmap Canvas & Today Highlight Ring:** Pixel-perfect rendering of Emerald (Standard Shifts), Amber (Overtime), Blue (Annual Leave A/L), Purple (Statutory Sick Pay SSP), and Off days with real-time Pay Day and Cutoff indicators, Cyan outline ring for Today's date, and dynamic scaling upon resizing.
  - **Adaptive Transparent & Glassmorphic Theming:** Supports translucent dark glass (`#E6151A22`) and 100% fully transparent (`#00000000`) modes to seamlessly blend with any device wallpaper.
  - **Real-Time Automatic Sync:** Instantly refreshes widget display whenever shifts are updated, imported via OCR, or edited in the main app.

- **Commute & Traffic Pre-Alarm Buffer Engine (`FEAT-925`) & Vibration Presets (`FEAT-923`) (`DutyAlarmManager.kt`, `MonthCalendarSyncDialog.kt`):**
  - **Dynamic Traffic Lead Buffer:** Configurable commute duration (15m, 30m, 45m, 60m) and traffic delay buffer (0m, 10m, 15m, 30m) to calculate exact departure times ($\text{Shift Start} - \text{Commute} - \text{Traffic Buffer}$) with 1-tap smart alarm generation.
  - **Alarm Vibration & Sound Customization:** Full alarm vibration toggling and custom label dispatching directly to the Android system clock app.

- **Google Calendar Category Sync & Shift Importer (`FEAT-921`) (`GoogleCalendarSyncEngine.kt`, `MonthCalendarSyncDialog.kt`, `ShiftHeatmapCard.kt`):**
  - **Account & Category Selector:** Queries and lists all device Google Calendars (`Personal`, `Work`, `Shifts`, `francyalinston@gmail.com`) allowing users to choose the target category for shift events.
  - **Custom Shift Timing & Filters:** Configurable Shift Start Time presets (`07:00 Care Day`, `19:00 Care Night`, `08:00 Standard`, `09:00 Office`), custom reminder minutes (15m, 30m, 1h), and selective shift type filters (Duty, Overtime, Annual Leave, Statutory Sick Pay).
  - **Duplicate Prevention:** Automatically clears prior Salary Calculator tagged events (`[SalaryCalculator]`) before inserting updated monthly rosters.

- **Auto Duty Days Phone Alarm Setter (`FEAT-922`) (`DutyAlarmManager.kt`, `MonthCalendarSyncDialog.kt`):**
  - **1-Tap Whole-Month Duty Alarm Automation:** Auto-configures multiple system alarms across every scheduled working duty day in the selected month in a single action.
  - **Strict Annual Leave Holiday Exclusion:** Automatically excludes Annual Leave (`A/L`) days and off days from alarms so users are not disturbed on holidays.
  - **Multiple Configurable Alarms per Duty Day:** Set multiple distinct alarm times (e.g. `05:30 Wake Up & Prepare`, `06:00 Depart / Commute`, `06:30 Arrive & Handover`) with custom labels and toggle switches saved to DataStore.

- **Statutory Sick Pay (`SSP`) Purple Shift Marking & Waiting Days Payroll Engine (`FEAT-915`) (`ShiftHeatmapCard.kt`, `PayScheduleEngine.kt`, `TaxCalculator.kt`, `SettingsScreen.kt`):**
  - **Interactive Purple (`#A855F7`) SSP Badging:** 7-column interactive shift heatmap featuring dedicated Purple `SSP` sick leave badges with a quick 5-state tap cycle (`0h Off` $\to$ `12h Std` $\to$ `12h OT` $\to$ `A/L` $\to$ `SSP` $\to$ `0h Off`) and Purple legend indicator.
  - **Statutory UK Waiting Days & Qualifying Sickness Engine:** Implements HMRC statutory rules where the first 3 consecutive qualifying days in a sickness sequence are unpaid waiting days, and subsequent days earn statutory pay (default £23.35/day, configurable in Settings).
  - **Cutoff-Aware SSP Payroll Splitting:** Splits SSP into in-cycle and rollover sick pay based on monthly payroll cutoffs, displaying real-time paid SSP days and total statutory sick pay in the pay cycle banner and adding it to gross pay, PAYE tax, NI, and net take-home calculations.
  - **Annual Printable PDF & Calendar Sync:** Renders Purple `SSP` days on printable 12-month PDF calendars and exports sick leave events to RFC 5545 `.ics` calendars.

- **OCR Duty Rota Auto-Detection of Annual Leave (`A/L`) & Sick Leave (`SSP`) (`FEAT-916`) (`DutyRotaOcrEngine.kt`, `DutyRotaImportDialog.kt`):**
  - **Automated Leave & Sickness Classification:** On-device ML Kit OCR automatically detects `"A/L"`, `"AL"`, `"LEAVE"`, `"HOL"`, and `"HOLIDAY"` as Annual Leave, and `"SSP"`, `"SICK"`, `"S"`, `"S/L"`, and `"SL"` as Statutory Sick Pay.
  - **Seamless Rota-to-Heatmap Mapping:** One-tap application directly encodes Blue `A/L` (`-999.0`) and Purple `SSP` (`-888.0`) into the monthly shift schedule, updating staff preview badges, summaries, and monthly salary calculations instantly.

- **Blue Annual Leave (`A/L`) Shift Marking & Dynamic Weekly Overtime Threshold Engine (`FEAT-913`) (`ShiftHeatmapCard.kt`, `PayScheduleEngine.kt`, `TaxCalculator.kt`, `SettingsScreen.kt`):**
  - **Interactive Blue (`#1E88E5`) Annual Leave Badging:** Interactive 7-column shift heatmap featuring dedicated Blue `A/L` holiday badges with a fast tap cycle (`0h Off` $\to$ `12h Std` $\to$ `12h OT` $\to$ `A/L` $\to$ `SSP` $\to$ `0h Off`) and Blue legend indicator.
  - **Configurable Annual Leave Rate & Shift Duration:** Customize Annual Leave Hourly Rate (£/hr) (defaulting to basic rate) and Holiday Shift Hours (7.5h standard, 8.0h, 12.0h long day) in Settings, calculating statutory holiday pay directly into Gross Pay, Tax, NI, and Take-Home earnings.
  - **Dynamic Weekly Overtime Threshold Trigger:** Automatically calculates cumulative regular hours in a Monday–Sunday calendar week and converts hours worked beyond the threshold (e.g., 37.5h or 40.0h) into paid Overtime.
  - **Annual PDF & Calendar Synchronization:** Exports Blue `A/L` holidays in high-resolution 12-month printable vector PDFs and RFC 5545 `.ics` calendar events.

- **Universal Multi-Currency Conversion & Live Periodic FX Sync (`MultiCurrencyConverterEngine.kt`, `CurrencySettingsDialog.kt`, `LiveFxWorker.kt`, `CalculatorScreen.kt`):**
  - **Universal Global Currency Registry:** Converts UK GBP (£) earnings into any user-selected primary and secondary currencies supporting **all 160+ world currencies** (EUR, USD, INR, PHP, CAD, AUD, JPY, CHF, SGD, NZD, AED, PLN, ZAR, NGN, KES, BRL, etc.) and major crypto assets (BTC, ETH, SOL, XRP, ADA, DOGE).
  - **Automated WorkManager Background Sync (`LiveFxWorker.kt`):** Fetches live foreign exchange rates from `open.er-api.com` and crypto pricing every 6 hours with offline graceful caching.
  - **Dynamic Home Screen Hero Net Pay Badges:** Renders real-time converted take-home salary badges for both primary and secondary currencies directly inside the Hero Net Pay card.
  - **Custom Rate Overrides & Quick Selectors:** Interactive currency settings dialog with quick currency chips, search, manual rate overrides, and 1-tap live rate refresh.

- **Comprehensive Full-App Backup & Restore System (`LedgerBackupManager.kt`, `BackupRestoreDialog.kt`, `SalaryRepository.kt`):**
  - **Full-State Backup Bundle (v2):** Captures complete application state—all monthly salary records, employer profiles, custom pre/post-tax deductions, pay schedule rules, UI theme settings, biometric locks, currency preferences, and the *entire* multi-year Shift Heatmap schedule (`annualShiftScheduleJson`). Restoring returns the user *exactly* to how they left the app.
  - **Legacy Backup Backward Compatibility:** Resilient multi-tier JSON parser seamlessly imports and restores older JSON backups from previous app versions without schema errors or data loss.
  - **System File Picker Integration:** Direct `.json` file picker launcher (`ActivityResultContracts.GetContent()`) with instant backup preview and 1-tap full restore.

- **Google Account & Drive AppData Cloud Synchronization (`GoogleDriveSyncEngine.kt`, `GoogleDriveSyncDialog.kt`, `AutoBackupObserver.kt`, `SettingsScreen.kt`):**
  - **Google Identity Integration:** Connect your Google account directly from Settings $\rightarrow$ Data Backup & Restore using standard Google Sign-In with private `drive.appdata` permissions.
  - **Private AppData Cloud Storage:** Backups (`SalaryCalculator_Latest.json`) are stored securely in your hidden Google Drive AppData folder, keeping regular Drive folders clean while syncing effortlessly across multiple Android phones and tablets.
  - **Automatic Continuous Sync & 1-Tap Restore:** Seamlessly updates the Drive backup whenever you make changes and enables 1-tap full ledger/shift restore on any device.

- **Real-Time Continuous Syncthing Auto-Backup & Phone-to-Phone Sync (`AutoBackupObserver.kt`, `SyncthingSyncEngine.kt`, `SyncthingSyncDialog.kt`, `SettingsScreen.kt`):**
  - **Instant State Observer:** Continuously observes application DataStore preferences in real time with 500ms debouncing, immediately serializing full snapshots to `Documents/SalaryCalculator/SalaryCalculator_Latest.json` and timestamped archives on any user edit (shifts, rates, deductions, tax codes, employer profiles).
  - **Multi-Phone Synchronization:** Allows continuous synchronization across multiple Android phones and private NAS clusters (`syncthing.449100.xyz`) without requiring manual exports.
  - **Syncthing REST API Connectivity:** Full REST integration supporting self-hosted instances with `X-API-Key` authentication, real-time connectivity ping tests, and remote folder scan triggers (`/rest/db/scan?folder=...`).
  - **1-Tap Syncthing Restore & Snapshot Pruning:** Automatically maintains the 15 most recent timestamped backups and provides 1-tap ledger restoration from synced files with live JSON schema validation.

- **Cutoff-Window-Aware Payslip Auditor & 1-Tap Payroll Dispute Emailer (`PayslipParserEngine.kt`, `PayslipAuditEngine.kt`, `PayslipAuditDialog.kt`, `ShiftHeatmapCard.kt`):**
  - **Automated Payslip OCR Import:** Scans payslips from camera captures, gallery photos, and multi-page PDFs with on-device ML Kit OCR to extract Employee Name, Payroll Ref, NI Number, Process Date, Tax Period, Tax Code, Basic Paid Hours (units), Hourly Rate, Bank Holiday uplift hours & rate, Gross, PAYE, NI, and Net Pay.
  - **Payroll Cutoff Window Awareness:** Strictly audits shifts within the employer's pay cycle window (previous month cutoff + 1 day to current month cutoff date, e.g. `24 Aug 2026 to 20 Sep 2026` for September 2026 payslip). Automatically brings forward previous month's post-cutoff shifts into qualifying in-cycle hours while isolating current month's post-cutoff shifts into upcoming October rollovers.
  - **Timesheet Heatmap Cross-Referencing:** Cross-references paid basic hours against qualifying in-cycle shifts in the monthly Shift Heatmap (e.g. 16 in-cycle shifts / 192.0h worked vs 15 shifts / 179.90h paid), detecting underpayments, missing shifts (−1 shift), missing hours (−12.10h), gross shortfall (+£154.16), and net take-home deficit (+£109.73 net at 72% cash retention).
  - **1-Tap Formal Dispute Email Generator:** Automatically generates a courteous, fully itemized dispute email specifying exact pay cycle window dates, employee reference, hours comparison, an itemized numbered list of in-cycle dates worked from the heatmap, and a post-cutoff rollover shift footnote for instant dispatch to employer payroll departments via default mail apps or clipboard.

- **Shift Swap & Coverage Forecaster (`ShiftSwapEngine.kt`, `ShiftSwapForecasterDialog.kt`, `ShiftHeatmapCard.kt`):**
  - **Statutory Net Take-Home Simulation:** Evaluates before-and-after gross earnings, PAYE Income Tax (20%/40%), Class 1 NI (8%), and net cash-in-hand yield when care workers swap shifts, pick up overtime cover (+12h @ 1.5x), or give away shifts.
  - **Roster Colleague Integration:** Automatically pulls colleagues from the parsed duty rota (*Francy D'Silva*, *Cecilia*, *Vanessa*, *Oyebolu*, etc.) and applies simulated shift changes directly to the live monthly heatmap schedule.
  - **Multi-Page PDF Duty Rota Batch Scanner:** Parses multi-page PDF documents and high-res images with high-accuracy grid extraction and 1-tap Calendar `.ics` synchronization.
- **OCR Duty Rota Shift Analyzer & Staff Member Importer (`DutyRotaOcrEngine.kt`, `DutyRotaImportDialog.kt`, `ShiftHeatmapCard.kt`):**
  - **Care Rota OCR Engine & Grid Alignment:** ML Kit Text Recognition with 2D spatial coordinate table clustering to parse healthcare and care worker duty rotas directly from photo captures, gallery images, and scans.
  - **Full Shift Code Taxonomy:** Accurately maps Day shifts (`D`), Night shifts (`N`), Annual Leave (`A/L`), Training Days (`TD`), Off duty (`-`), and unknown/custom markings verbatim as `Custom(code)`.
  - **Searchable Roster Selector:** Renders all detected staff members (e.g. `Francy D'Silva` 15 Nights, `Cecilia` 13 Days, `Vanessa` 15 Days, `Oyebolu` 16 Nights + 5 AL, `Francis Alabi`, `Rajeev Raju`, `Alan Pascua`, `Michelle Prust`, etc.) with quick name/role search.
  - **Interactive Next Month Heatmap Preview & Customizer:** High-fidelity calendar preview with distinct shift color badges (Night Indigo, Day Emerald, Annual Leave Amber, Training Teal), interactive tap-to-edit cell overrides, and instant 1-tap "Confirm & Apply to Heatmap" DataStore sync.
- **Dynamic Shift Duration Heatmap & UK Care Worker Overtime Tax Engine (`ShiftHeatmapCard.kt`, `OvertimeOptimizerEngine.kt`, `CalculatorScreen.kt`):**
  - **Dynamic Configured Shift Calibration:** Shift Heatmap and calendar dynamically adopt the worker's configured shift length (`12.0h` for care workers or custom hours) across legends (`12h Standard`, `12h OT`), cell tap cycles (`0h -> 12h Std -> 12h OT -> 0h`), and rota presets (`Mon-Fri 12h`, `4-On 4-Off 12h`, `Convert 8h to 12h`).
  - **UK Care Worker Overtime Tax Breakdown:** Exact statutory HMRC marginal taxation analysis for care worker overtime. Itemizes 20% Basic PAYE, 8% Class 1 NI, statutory 28% marginal tax rate, and 72% net cash-in-pocket retention rate (£13.85/hr net, +£166.15 net cash per 12h shift). Features an interactive "Apply Rota to Calculator" button and a dedicated Care Worker Overtime Tax card embedded directly in the monthly payslip breakdown.
- **Default Home Screen Shift Heatmap & 12h Overtime Shift Calibration (`ShiftHeatmapCard.kt` & `CalculatorScreen.kt`):** Interactive shift heatmap embedded directly on the main home screen as the primary default view at the very top of the interface—no scrolling, dialogs, or extra clicks needed. Positioned immediately below the header so users see their shifts and calendar grid first when launching the app. Features sequential `< Month Year >` navigation with 12-month chip bar, quick presets including 4-On 4-Off (12h), calibrated 12h Overtime shift cycle with amber badge highlighting, live payroll cutoff countdown, and real-time two-way synchronization with basic hours and overtime take-home pay.
- **Quick-Shift Length Presets on Home Screen (`CalculatorScreen.kt`):** Interactive shift duration chips directly embedded inside the Shifts & Variable Pay card (`12h Care`, `8h Standard`, `7.5h Office`, `10h Extended`). Tapping any preset immediately recomputes monthly gross and net salary, invalidates local overrides, and synchronizes with DataStore preferences and active employer profiles.
- **UK Statutory Sick Pay (SSP) & 12.07% Holiday Pay Accrual Engine (`SspHolidayEngine.kt` & `SspHolidayDialog.kt`):** Comprehensive statutory leave module implementing UK 2024/2025 & 2025/2026 statutory rates (£116.75/week standard SSP with qualifying days waiting period and linked illness bypass) alongside the statutory 12.07% holiday pay accrual calculation for irregular and shift workers under April 2024 reforms.
- **Multi-Rate Shift Differentials & Enhancements Engine (`ShiftRateDifferentialEngine.kt` & `ShiftRateDifferentialDialog.kt`):** Complete payroll calculation engine modeling unsocial hours uplifts, night enhancements (hourly addition or multipliers), weekend premiums (Saturday/Sunday multipliers), bank holiday double-time, and flat sleep-in duty allowances (e.g. £50/shift), computing blended effective hourly rates and net take-home gains.
- **Corporate PDF Payslip Generator Redesign (`PdfPayslipGenerator.kt`):** Enhanced vector PDF payslip document with HMRC tax month indicators (M1–M12), clear itemized earnings and deductions tables, and an integrated Year-to-Date (YTD) Estimates summary container (Gross, Tax, NI, Net).
- **Instant Two-Way Synchronization for Settings & Home Screen Work Hours (`SettingsScreen.kt` & `CalculatorScreen.kt`):** Dynamic reactive synchronization between Preferences & Settings and the Home Calculator. Modifying standard shift duration (e.g. 12.0h care worker shift vs 8.0h standard full-time) or hourly wage instantly updates the calculator header chips, shift banners, and basic pay computations in real-time with zero lag or unsaved state loss.
- **Static Employment Wage & Pension Decoupling (`SettingsScreen.kt` & `CalculatorScreen.kt`):** Decoupled static employment configurations (Hourly Rate, Standard Shift Length / Hours per Job, Workplace Pension Opt-Out/Rate, and Student Loan Plan) into Settings, decluttering the Home calculator to focus on dynamic monthly shifts worked, overtime, and salary sacrifice. Features informative summary status banners on Home with direct settings links.
- **Care Worker August HMRC Statutory Calibration (`TaxCalculator.kt`):** Exact penny accuracy calibrated for care worker monthly payslips (16 shifts $\times$ 12.0h @ £12.82/hr = £2,461.44 gross, £1,413.02 taxable pay with Month 5 cumulative allowance £1,048.42, £282.60 PAYE tax, £113.07 HMRC CA38 truncated Class 1 NI, and £2,065.77 net take-home with pension opted out).
- **Interactive Payroll Tax Month Selector & Forecaster (`TaxMonthSelectionDialog`):** Real-time tax month picker enabling instant switching and forecasting across all 12 HMRC tax months (April M1 through March M12) with accurate statutory cumulative personal allowances.
- **High Income Child Benefit Charge (HICBC) Taper Engine (`HicbcEngine.kt` & `HicbcDialog.kt`):** Statutory UK 2024/2025–2025/2026 reformed HICBC calculation engine modeling the £60,000–£80,000 Adjusted Net Income (ANI) taper band. Computes exact statutory Child Benefit (£25.60/wk first child, £16.95/wk additional children), clawback percentages (1% per £200), and automated Pension Salary Sacrifice Remedy to restore 100% of Child Benefit and eliminate tax charges.
- **Salary Sacrifice Company Car & EV Benefit-in-Kind (BiK) Calculator (`CompanyCarBikEngine.kt` & `CompanyCarBikDialog.kt`):** Comprehensive company vehicle lease savings calculator supporting Pure Electric (EV 2%–5%), PHEV, and ICE powertrains across 2024/25 through 2027/28 tax years. Evaluates gross monthly salary sacrifice vs 20%/40%/45% Income Tax and 8%/2% NI savings, monthly Class 1A BiK tax charges, net take-home salary impact, and comparative savings vs private personal contract hire (PCH).
- **Dynamic Multi-Currency Salary Converter (`MultiCurrencyConverterEngine.kt` & `MultiCurrencyConverterDialog.kt`):** Instant global currency conversion engine translating UK net take-home salary and gross earnings into 10 major global currencies (EUR, USD, CAD, AUD, JPY, CHF, SGD, INR, NZD, AED) across Annual, Monthly, 4-Weekly, Bi-Weekly, Weekly, Daily, and Hourly horizons.
- **HMRC Tax-Free Childcare & 30-Hours Subsidy Engine (`TaxFreeChildcareEngine.kt` & `TaxFreeChildcareDialog.kt`):** Statutory childcare funding calculator modeling 20% government top-ups (up to £2,000/yr per child or £4,000 disabled), 30-hours / 15-hours free childcare funding equivalents, and the £100,000 Adjusted Net Income cliff-edge. Features automated pension salary sacrifice remedy calculation to restore full entitlement and calculate net ROI.
- **Overtime Tax Bracket Headroom & Threshold Monitor (`OvertimeBracketEngine.kt` & `OvertimeBracketDialog.kt`):** Real-time tax bracket capacity monitor computing exact remaining headroom in pounds (£) and safe monthly overtime hours before crossing the 40% Higher Rate band (£50,270) or 60% Marginal Tax Trap (£100,000). Features 1-tap pension sacrifice recommendations.
- **Multi-Job Cumulative Tax & Dual-Employer Aggregator (`MultiJobAggregatorEngine.kt` & `MultiJobTaxDialog.kt`):** Dedicated dual-job calculation engine and interactive Material 3 modal modeling concurrent employments. Evaluates independent Class 1 Primary NI thresholds (£12,576 per employer) alongside combined cumulative PAYE Income Tax brackets (Basic 20%, Higher 40%, Additional 45%, or Scottish 6-tier system). Features individual job net take-home breakdowns, secondary tax code presets (`BR`, `0T`, `D0`, `1257L`), and 60% marginal tax trap threshold warnings.
- **Employer On-Cost & Inside-IR35 Umbrella Payroll Engine (`UmbrellaPayrollEngine.kt` & `UmbrellaPayrollDialog.kt`):** Comprehensive contractor payroll calculator translating Day Rates (£200–£1,000/day) and Hourly Rates (£25–£150/hr) into net personal take-home pay. Models 2025/2026 15.0% Employer Class 1 NI (above £5,000) vs 2024/2025 13.8% NI (above £9,100), Apprenticeship Levy (0.5%), Employer Pension (3%), Umbrella Margin fees, Employee PAYE/NI, and net retention percentages.
- **Overtime Tax-Efficiency & Marginal Return Optimizer (`OvertimeOptimizerEngine.kt` & `OvertimeTaxOptimizerDialog.kt`):** Interactive simulation modal determining the exact marginal take-home cash return per hour of overtime worked (0 to 40+ hours) across selectable multipliers (`1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.5x`). Models extra gross earnings vs extra PAYE, NI, Student Loan, and Pension deductions, calculates retention percentage, and displays real-time efficiency ratings (High 65%+, Moderate 50%–65%, Low <50%) and 60% marginal tax trap warnings.
- **Multi-Employer Shift Color Coding & Dual Schedules (`EmployerProfile.kt`, `SalaryRepository.kt`, `ShiftCalendarDialog.kt`):** Full support for multi-job workers. Added `colorHex` profile customization and persistent employer shift mapping (`getShiftEmployerAssignments`). In the Shift Heatmap, users can filter by employer, assign specific shifts to individual employers, view custom-colored day badges, and inspect separated earnings breakdowns per employer.
- **Post-Cutoff Rollover Payroll Ingestion Engine (`PayScheduleEngine.kt`, `ShiftCalendarDialog.kt`, `CalculatorScreen.kt`):** Modeled strict UK payroll rollover rule where shifts logged after the cutoff date in Month $M-1$ are automatically brought forward and included in Month $M$'s payslip calculation (`totalPaidDays`, `totalPaidHours`, `totalPaidOtHours`), while shifts after Month $M$'s cutoff roll into Month $M+1$.
- **Automated Rotational Shift Pattern Generator (`ShiftPatternGenerator.kt` & `ShiftPatternGeneratorDialog.kt`):** Instant 1-tap generation of industrial rotational shift schedules across 1, 3, or 12 months with anchor date synchronization (4-On 4-Off, Continental 2-2-3, Pitman 2-3-2, 3-Shift Rotating, Mon–Fri Standard).
- **Overtime Tax Bracket & 60% Marginal Tax Trap Alerts (`TaxCalculator.kt`, `SandboxCalculatorDialog.kt`, `ShiftCalendarDialog.kt`):** Real-time tax threshold detection identifying when cumulative or overtime earnings push annual income into the 40% Higher Rate band (> £50,270) or the 60% Marginal Tax Trap (£100,000 – £125,140 Personal Allowance tapering zone). Features instant pension salary sacrifice recommendations to eliminate marginal penalty and preserve personal allowance.
- **Isolated "What-If" Scenario Sandbox Calculator (`SandboxCalculatorDialog.kt`):** Decoupled variable earnings and scenario modeling from the live Shift Heatmap into a dedicated sandbox environment. Supports standard working days/hours, independent Overtime multiplier pills (`1.0x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`, `2.5x`), dedicated Bank Holiday multiplier pills (`1.5x`, `2.0x`, `2.5x`, `3.0x`), variable bonus/commission inputs, quick scenario presets (+2 Weekend OT, +1 Bank Holiday, +£500 Bonus), and real-time net take-home pay comparison vs baseline actual payslip.
- **Base Hours per Working Day Configuration in Settings (`SettingsScreen.kt` & `SalaryRepository.kt`):** Configurable standard base hours per working day (default `8.0h`, with presets `7.5h`, `8.0h`, `10.0h`, `12.0h`) persisted in DataStore (`default_hours_per_day`), automatically consumed across the home screen, Shift Heatmap, and schedule presets.
- **Sequential Month-by-Month Heatmap Navigation (`ShiftCalendarDialog.kt`):** Re-engineered primary `<` and `>` arrow navigation to step sequentially through individual months with automatic year rollover (e.g., December 2026 $\rightarrow$ January 2027), accompanied by a dedicated year selector dropdown/chips.
- **Visual Shift Heatmap Color Legend (`ShiftHeatmapCard.kt` & `ShiftCalendarDialog.kt`):** Prominent color legend bar defining 🟢 Standard 8h Shift (`Emerald60`), 🟠 Overtime 12h Shift (`Amber60`), 🔵 Part-Time <8h Shift (`Teal60`), and ⚪ Day Off.
- **Live Home Screen Sync with Active Month Heatmap Schedule (`CalculatorScreen.kt`):** On app launch and upon calendar changes, the Home screen automatically syncs with the current calendar month's saved shift schedule (`getMonthShiftSchedule`), reflecting accurate days worked, average hours, and overtime.

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
- Stable Release APK: `APKs/Salarycalculator-v<version>.apk` (packaged from `app/build/outputs/apk/release/`)
- Debug Development APK: `APKs/Salarycalculator-v<version>-debug.apk` (packaged from `app/build/outputs/apk/debug/`)

### Run Unit Tests
```bash
./gradlew test
```

---

## Continuous Integration & Automated Releases (GitHub & Forgejo)

The repository provides continuous delivery workflows and release synchronization for both GitHub and Forgejo:
- **Mandatory Dual-Platform Release**: Every version release is published simultaneously to both GitHub and Forgejo with version-stamped production (`Salarycalculator-v<version>.apk`) and debug (`Salarycalculator-v<version>-debug.apk`) APKs attached.
- **GitHub Actions**: [`.github/workflows/release.yml`](file:///.github/workflows/release.yml) automatically builds signed Release and Debug APKs on push to `main` and creates official GitHub releases on `v*` tags with versioned asset attachments.
- **Forgejo Actions & API Synchronizer**: [`.forgejo/workflows/release.yaml`](file:///.forgejo/workflows/release.yaml) and [`scripts/sync_forgejo_releases.py`](file:///scripts/sync_forgejo_releases.py) automatically publish release notes and upload versioned APK assets directly to your self-hosted Forgejo instance (`https://forgejo.449100.xyz`).
- **Forgejo `act_runner` CI/CD**: Containerized turn-key runner package located in [`ci/forgejo-runner/`](file:///ci/forgejo-runner/) for on-premise builds.

---

## Tech Stack

- **Language:** Kotlin 2.1.20 (JVM Toolchain 17)
- **UI Toolkit:** Jetpack Compose (BOM 2026.03.01) + Material 3
- **Navigation:** AndroidX Navigation 3 (`1.0.1`)
- **Data Persistence:** Jetpack DataStore Preferences (`1.1.1`)
- **Build System:** Gradle 9.0.1 with Android Gradle Plugin (`9.0.1`) & Version Catalogs (`libs.versions.toml`)