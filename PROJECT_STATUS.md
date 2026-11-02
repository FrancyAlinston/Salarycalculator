# Salary Calculator — Tasks & Project Status Tracker

Track all ongoing implementations, tax calculations, mobile architectures, wearable suites, cloud synchronizations, and operational workflows for Salary Calculator.

---

## 📊 Summary Overview

| Feature / Subsystem | Target Component | Status | Last Updated |
| :--- | :--- | :--- | :--- |
| **UK Tax & Class 1 NI Engine (2024/25 & 2025/26)** | Domain / TaxCalculator.kt | 🟢 **Completed** | 2026-09-25 |
| **Scottish 6-Tier Income Tax Bands** | Domain / Scottish Tiers | 🟢 **Completed** | 2026-09-25 |
| **Student Loan Repayment Plans (1, 2, 4, 5, PG)** | Domain / Deductions | 🟢 **Completed** | 2026-09-25 |
| **Care Worker Cutoff Engine (Last Friday / Sun 23:59)** | Domain / Shift Rollover | 🟢 **Completed** | 2026-09-25 |
| **Foldable Dual-Screen Responsive Layout** | Presentation / Navigation | 🟢 **Completed** | 2026-09-25 |
| **Edge-to-Edge Material 3 Theming** | UI / Jetpack Compose | 🟢 **Completed** | 2026-09-25 |
| **DataStore Preferences & Sync SharedPreferences** | Persistence / SalaryRepository | 🟢 **Completed (v28.1)** | 2026-11-01 |
| **Nextcloud RFC 5545 iCalendar Feed (`/api/calendar.ics`)** | Integration / CalDAV Sync | 🟢 **Completed** | 2026-10-15 |
| **UGOS NAS NVMe Docker Stack (`salarycalculator-web`)** | Infrastructure / Port 8085 | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Cloudflare Pages & Edge CDN Distribution** | Web / Edge Caching | 🟢 **Completed** | 2026-10-20 |
| **Google Authentication Gatekeeper Screen** | Auth / Google Identity | 🟢 **Completed** | 2026-10-20 |
| **Google Drive AppData Backup & Restore** | Cloud Sync / GoogleDriveSync | 🟢 **Completed** | 2026-10-20 |
| **Zero-Latency NAS Long-Polling Sync (`/api/backup/poll`)** | Backend / server.py | 🟢 **Completed (v28.0)** | 2026-11-01 |
| **Cross-Device Peer Shift Adoption (`shouldApplyFromPeer`)** | Realtime Multi-Device Sync | 🟢 **Completed (v28.1)** | 2026-11-01 |
| **AutoBackupObserver Payload Fingerprinting** | Persistence / Loop Prevention | 🟢 **Completed (v28.1)** | 2026-11-01 |
| **Wear OS Smartwatch Companion App (:wear)** | Wear OS / WearHeatmapScreen | 🟢 **Completed (v27.9)** | 2026-11-01 |
| **Galaxy Watch 8 Classic Bezel Dial Navigation** | Wear OS / Rotary Scroll | 🟢 **Completed** | 2026-10-31 |
| **Watch Face Format (WFF v2 XML) Package (:watchface)** | Wear OS / watchface.xml | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Casio Classic Retro Digital Watch Face Suite** | Wear OS / CanvasRenderer | 🟢 **Completed (v27.8)** | 2026-11-01 |
| **Casio Selectable LCD Color Themes (4 Palettes)** | Wear OS / CasioColorTheme | 🟢 **Completed (v27.8)** | 2026-11-01 |
| **Casio Hourly Chime (`SIG`) & Shift Alert (`ALM`)** | Wear OS / Haptic Signals | 🟢 **Completed (v27.8)** | 2026-11-01 |
| **Wear OS Battery Lifecycle & Sensor Unregistration** | Wear OS / Optimization | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Throttled Battery State IPC & Zero-Wakeup Weather** | Wear OS / Power Efficiency | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Bluetooth-First Wearable Sync Route** | Wear OS / Bluetooth LE | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Dual-Platform Forgejo & GitHub Release Pipeline** | CI/CD / Release Automation | 🟢 **Completed (v28.2)** | 2026-11-02 |
| **Offline P2P Wi-Fi Direct Sync Fallback** | Multi-Device Sync | ⚪ **In Pipeline / Backlog** | — |
| **Direct Wear OS Complication Shift Toggle Slot** | Wear OS / Complications | ⚪ **In Pipeline / Backlog** | — |

---

## 🟢 Completed Subsystems & Deliverables

### 1. Wear OS Battery Optimization & Dynamic Lifecycle (`v28.2`)
- **Component**: `WearCasioWatchFaceService.kt`, `WearTelemetryManager.kt`, `WearSalaryRepository.kt`
- **Status**: 🟢 **Completed**
- **Deliverables**:
  - Unregisters `Sensor.TYPE_STEP_COUNTER` whenever screen turns off or enters ambient mode, allowing hardware sensor hub to sleep.
  - Replaced per-second `registerReceiver` system IPC calls with 15-second memory caching, eliminating binder transaction overhead.
  - 2-hour weather caching with 30-min failure backoff; minute render ticks operate strictly against local DataStore memory with zero HTTP network wakeups.
  - Direct HTTP watch-to-NAS sync is guarded by active Wi-Fi check, ensuring shift edits default exclusively to low-power Bluetooth `Wearable.MessageClient` route.

### 2. Cross-Device Peer Shift Adoption & Feedback Loop Elimination (`v28.1`)
- **Component**: `AutoBackupObserver.kt`, `SalaryRepository.kt`, `RealtimeMultiDeviceSyncEngine.kt`, `LedgerBackupManager.kt`
- **Status**: 🟢 **Completed**
- **Deliverables**:
  - Implemented payload fingerprinting over user data to prevent infinite 5-second auto-backup loops.
  - Decoupled `last_synced_remote_timestamp` out of reactive DataStore into private SharedPreferences (`salary_calculator_sync_meta`).
  - Added `shouldApplyFromPeer` to instantly restore remote shifts from peer phones (`bundle.deviceModel != myDeviceModel`).
  - Verified 100% data equivalence across Samsung Galaxy S25 Ultra (`SM-S938B`) and Google Pixel 8 Pro.

### 3. Zero-Latency Real-Time Long-Polling Multi-Device Sync Engine (`v28.0`)
- **Component**: `RealtimeMultiDeviceSyncEngine.kt`, `server.py`, `SalaryRepository.kt`, `LedgerBackupManager.kt`
- **Status**: 🟢 **Completed**
- **Deliverables**:
  - Built thread-safe HTTP long-polling (`/api/backup/poll`) on UGOS NAS backend with < 15ms wakeup latency.
  - Immediate local edit broadcast (`triggerImmediatePush`) on every shift cycle or edit.
  - Added local edit timestamp protection (`recordLocalEdit`) to prevent remote overwrites of active interactions.

### 4. Bidirectional Multi-Device & Watch Real-Time Cloud Sync (`v27.9`)
- **Component**: `RealtimeMultiDeviceSyncEngine.kt`, `PhoneWearableListenerService.kt`, `CloudSyncWorker.kt`, `WearSalaryRepository.kt`
- **Status**: 🟢 **Completed**
- **Deliverables**:
  - Continuous foreground polling of `/api/backup/timestamp` on NAS and Cloudflare CDN every 12 seconds.
  - Watch shift edits broadcast to connected phones via Google Play Services Wearable `MessageClient`.
  - Background synchronization executed every 15 minutes via WorkManager `CloudSyncWorker`.

### 5. Watch Face Format (WFF v2 XML) Package (`v27.8`)
- **Component**: `watchface/src/main/res/raw/watchface.xml` (`:watchface` module)
- **Status**: 🟢 **Completed**
- **Deliverables**:
  - Standalone declarative XML watch face for Wear OS 4+ / 5+ and One UI Watch.
  - Runs natively inside Samsung/Google system compositor with zero Kotlin service overhead and maximum battery life (35–45h).
  - Pure black (`#000000`) AMOLED background with digital clock (`HH:MM:SS`), live `[STEP_PERCENT]`, and `[BATTERY_PERCENT]`.

---

## ⚪ In Pipeline / Backlog

1. **[ACTION: FEAT-960] Offline Peer-to-Peer Wi-Fi Direct Sync Fallback**: Direct device-to-device local Wi-Fi pairing fallback when NAS is powered off or unreachable.
2. **[ACTION: FEAT-961] Direct Wear OS Complication Shift Toggle Slot**: One-tap toggle complication slot updating shifts without opening full companion UI.
