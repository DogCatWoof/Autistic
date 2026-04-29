# Autistic — As-Is Architecture

## Overview

A personal Android app for autistic adults. Manages daily tasks, calendar events, food logging, mood tracking, energy budgeting, health data, notes, sequences (checklists), and a conversation assistant for tone coaching. All data is local-first (Room), with optional Google sync (Tasks + Calendar + Drive backup) and Health Connect integration.

---
L
## Tech Stack

| Area | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation (bottom bar + modal drawer) |
| DI | Koin |
| Database | Room (single main DB + separate product DB) |
| Background work | WorkManager |
| Google APIs | Tasks, Calendar, Drive (via Google API Java Client) |
| Health | Health Connect |
| Camera | CameraX + ML Kit (barcode) |
| AI vision | Claude Haiku (direct Anthropic HTTP API) |
| Auth tokens | EncryptedSharedPreferences |
| Backup encryption | Android Keystore, AES-256-GCM |
| Preferences | Jetpack DataStore |
| Serialization | Gson |
| Testing | JUnit4, MockK, Espresso, Room in-memory |

---

## Layer Structure

```
UI (Compose screens + ViewModels)
    ↓
Service / Orchestration (sync, vision, energy calculation)
    ↓
Repository (Room DAOs wrapped with timing + error handling)
    ↓
Database (Room) / Remote Sources (Google APIs, Anthropic)
```

All dependencies are constructor-injected via Koin. No global mutable state outside of singletons registered in DI modules.

---

## Application Entry Points

### AutisticApp
- `Application` subclass; initialises all Koin modules on `onCreate`.

### MainActivity
- Single Activity; hosts all Compose navigation.
- Bottom nav bar + collapsible modal drawer (10 destinations).
- Registers `DailyResetWorker`, `MoodCheckInWorker`, `HealthConnectSyncWorker` on start.
- Schedules periodic + immediate Google sync on start (if authenticated).
- Persists last-visited destination via `NavStateStore`.
- Filters visible nav items via `NavPreferencesStore`.
- Handles deep-link intents: `OPEN_SCAN`, `OPEN_TASKS`, `OPEN_NOTES`, `OPEN_MOOD`.

### HealthPermissionsRationaleActivity
- Required by Health Connect Play Store policy.
- Handles `ACTION_SHOW_PERMISSIONS_RATIONALE`.

---

## Database

### TaskDatabase (version 16)
Single Room database holding all app data except barcode products.

**Entities and their DAOs:**

| Entity | DAO | Notes |
|---|---|---|
| `TaskEntity` | `TaskDao` | Tasks with Google sync status |
| `CalendarEventEntity` | `CalendarDao` | Local mirror of Google Calendar |
| `DailyTaskEntity` | `DailyTaskDao` | Recurring daily task templates |
| `NoteEntity` | `NoteDao` | Soft-delete notes |
| `MoodEntity` | `MoodDao` | Mood log entries |
| `FoodLogEntry` | `FoodLogDao` | Daily nutrition aggregate |
| `FoodLogItemEntry` | `FoodLogItemDao` | Individual food log items |
| `FoodCacheEntity` | `FoodCacheDao` | AI-accepted nutrition cache |
| `HealthSnapshotEntity` | `HealthSnapshotDao` | Daily health metric snapshot |
| `EnergyProfileEntity` | `EnergyDao` | Singleton energy capacity profile |
| `EnergyLogEntry` | `EnergyDao` | Per-activity energy log |
| `ActivityCostEntry` | `EnergyDao` | Learned cost per activity type |
| `StartOfDayEntry` | `EnergyDao` | Daily baseline multipliers |
| `SequenceEntity` | `SequenceDao` | Checklist template |
| `SequenceStepEntity` | `SequenceDao` | Steps within a checklist |
| `SequenceRunEntity` | `SequenceDao` | Active/completed run of a sequence |
| `SequenceStepProgressEntity` | `SequenceDao` | Per-step completion within a run |

Uses `InstantConverter` (Instant ↔ ISO-8601 string). Supports `checkpoint()` for WAL flush before backup. Has `closeInstance()` for restore operations.

### ProductDatabase (version 4)
Separate database for barcode → product lookups.

| Entity | DAO |
|---|---|
| `ProductEntity` | `ProductDao` |

Uses `NutrimentsConverter` for JSON-serialised `Nutriments`.

---

## Feature Areas

### Task Management

**`TaskEntity`** sync lifecycle: `local` → `pending_push` → `synced`, or `pending_delete` before remote deletion. Daily tasks carry a non-null `dailyTaskId` and are excluded from Google Tasks sync.

**`TaskViewModel`** combines `TaskRepository` and `CalendarRepository` into a single sorted, sectioned list (`GroupedTaskItems`): Past Due / Today / Later (grouped by date). Sort key: `dueAt + expectedTimeMinutes` if both set, else `dueAt`, else end-of-today.

Task completion flow: local-only tasks (no `googleTaskId`) are updated without queuing a sync push; Google-linked tasks mark `pending_push`.

Task reminders: `TaskReminderWorker` scheduled per task at `dueAt − reminderMinutesBefore`, REPLACE policy keyed by task id.

### Calendar Integration

`CalendarEventEntity` mirrors Google Calendar events locally. Users can "complete" an event (marks `isHidden`; hidden from list but not deleted remotely) or delete it (marks `pending_delete`; deleted on next sync).

Full 60-day rolling-window sync on every sync run — no incremental token. Cancelled events are deleted locally; all others are upserted (remote wins).

### Daily Tasks

`DailyTaskEntity` stores recurring templates (title, time, expected duration, required flag). `DailyResetWorker` runs once per calendar day: deletes all unfinished daily-generated tasks, then re-inserts fresh `TaskEntity` rows from active templates.

### Notes

`NoteEntity` supports soft delete (restore within session). `displayTitle` returns the first non-blank content line if no explicit title is set.

### Mood Tracking

`MoodEntity`: level, emoji (14 options), activity, notes, timestamp. Hourly `MoodCheckInWorker` triggers a notification. `MoodBroadcastReceiver` handles taps on emoji action buttons and `RemoteInput` for notes directly from the notification shade.

### Food Logging & Nutrition

Two-level storage: `FoodLogEntry` (daily aggregate) + `FoodLogItemEntry` (individual items with photo path and AI analysis result). 14-day retention enforced by `DailyResetWorker`.

Photo analysis flow (`FoodLogViewModel` state machine):
1. **Idle** — no active photo
2. **Classifying** — Claude Haiku classifies image as FOOD / PRODUCT / UNKNOWN
3. If FOOD → **Ready** (nutrition pre-filled from AI)
4. If PRODUCT with barcode → **LookingUp** → tries food cache → barcode lookup (USDA FDC, then Open Food Facts) → **Ready**
5. If no nutrition found → **NeedNutritionPhoto** → user photographs nutrition label → **OcrLoading** → Claude Haiku parses label → **Ready**
6. User confirms, scales by servings, caches accepted result, saves item.

`FoodCacheEntity` stores previously accepted AI analyses keyed by description (case-insensitive) for instant pre-fill on repeat items.

Barcode lookup chain: local `ProductDatabase` → USDA FDC API → Open Food Facts API. Results cached locally on first fetch.

`netCarbs` computed as `totalCarbs − fiber − sugarAlcohols`.

### Energy Budget

Singleton `EnergyProfileEntity` stores daily capacity and mode. Per-day: `StartOfDayEntry` (sleep quality, stress, physical state, baseline multiplier) + `EnergyLogEntry` per activity.

`ActivityCostEntry` holds a learned cost per activity type using exponential smoothing over up to 50 samples, adjusted by post-activity difficulty rating (1=easy ×0.85, 3=hard ×1.2). Default costs provided for 11 activity types (Focus Work=2.0, Meeting=3.0, Break=−1.0, Nap=−2.0, etc.).

`EnergyRepository` computes: today's total spent, balance, and projected balance after a hypothetical activity.

### Sequences (Checklists)

Templates: `SequenceEntity` + ordered `SequenceStepEntity` rows. Running a sequence creates a `SequenceRunEntity`; completing individual steps writes `SequenceStepProgressEntity`.

`SequenceRunNotificationManager` posts a persistent notification showing the current step and progress (e.g., "Step 3 of 7"). Action buttons: **Done** (complete current step) and **End** (complete entire run). `SequenceStepReceiver` handles both actions; auto-completes run when all steps are done.

### Health Connect Integration

`HealthConnectRepository` reads 6 data types: steps, sleep, heart rate, weight, calories burned, blood glucose. `refreshTodaySnapshot()` reads all 6 and upserts a `HealthSnapshotEntity` for today. `backfillRecentDays(7)` backfills the last 7 days.

`HealthConnectSyncWorker` runs hourly (KEEP policy) to refresh today's snapshot silently.

### Conversation Assistant

Rule-based `IntentClassifier` (no network call): classifies input as Question / Command / Social / Sarcasm / Unknown with confidence score. `ResponseTemplateRepository` loads `response_templates.json` and returns matching templates by intent + tone. Three tones: neutral, polite, direct.

`ConversationViewModel` manages live speech recognition (Android `SpeechRecognizer`) and TTS (`TextToSpeech`). Response chips shown after classification; selecting a chip optionally speaks it.

---

## Sync Pipeline

```
triggerSync() [TaskViewModel]
    → SyncScheduler.triggerImmediate()
        → WorkManager: enqueue SyncWorker (REPLACE, name="sync_immediate")
            → SyncOrchestrator.sync()
                1. authManager.isAuthenticated() — abort with Retry if false
                2. GoogleTasksSyncService.pushPending()
                   - CREATE tasks with syncStatus IN ('pending_push', 'local' AND dailyTaskId IS NULL) and no googleTaskId
                   - UPDATE tasks with syncStatus='pending_push' and googleTaskId set
                   - DELETE tasks with syncStatus='pending_delete'
                3. GoogleTasksSyncService.pullAndMerge()
                   - Fetch all remote tasks (paginated)
                   - Delete locally any remote-deleted tasks
                   - Upsert active tasks (remote wins)
                   - Prune completed tasks older than 7 days
                4. CalendarSyncService.pullAndMerge()
                   - Push pending local event deletes
                   - Fetch events from 60 days ago (paginated)
                   - Delete cancelled events locally
                   - Upsert active events
```

**Periodic sync:** two WorkManager periodic workers — WiFi (15 min, unmetered) and Cellular (1 hour, any network). Both replaced on each app start.

**Sync outcomes:** `Success` → saves timestamp to DataStore. `Retry` → WorkManager retries with exponential backoff (any exception except 403). `Error` → terminal failure, surfaced in `SyncState.Error` (currently only 403 scope errors).

**`syncState` flow** in `TaskViewModel`: maps WorkManager `WorkInfo.State` → `SyncState` (Idle / Syncing / Error / LastSynced) driving the toolbar sync icon and spinner.

---

## Authentication & Security

`GoogleAuthManager` handles three Google scopes: Tasks, Calendar, Drive File. Token lifecycle:

1. Sign-in intent → `handleSignInResult()` saves account email to `TokenStore`.
2. `getValidToken()`: checks `TokenStore` cache (valid if expiry > now + 60s), otherwise fetches via `GoogleAuthUtil` on IO dispatcher, stores with 1-hour TTL.
3. On 403 from sync: `invalidateTokenCache()` clears cached token, forcing re-fetch next call.
4. Sign-out: revokes via Google sign-in client, clears all stored tokens.

`TokenStore` uses `EncryptedSharedPreferences` (AES-256-GCM + AES-256-SIV). Recovers from Keystore corruption by deleting and recreating; falls back to plaintext on persistent failure.

### Drive Backup

`DriveBackupService` encrypts the Room WAL-checkpointed database with AES-256-GCM (Android Keystore), uploads to a "Autism Backups" Drive folder, and retains the last 7 files. Restore decrypts the most recent backup; falls back to legacy unencrypted if present. Backup runs daily from `DailyResetWorker` when authenticated.

---

## Background Workers

| Worker | Trigger | Policy | Action |
|---|---|---|---|
| `DailyResetWorker` | App start | KEEP (once per day) | Reset daily tasks, prune food logs, Drive backup |
| `MoodCheckInWorker` | App start | KEEP, 1 hr periodic | Post mood notification |
| `HealthConnectSyncWorker` | App start | KEEP, 1 hr periodic | Refresh today's Health Connect snapshot |
| `SyncWorker` (periodic) | App start | UPDATE, 15 min WiFi / 1 hr cellular | Full Google sync |
| `SyncWorker` (immediate) | Manual / sign-in | REPLACE, one-time | Full Google sync |
| `TaskReminderWorker` | Task insert/update | REPLACE per task id | Post task reminder notification |

---

## Dependency Injection (Koin Modules)

| Module | Key bindings |
|---|---|
| `databaseModule` | `TaskDatabase`, `ProductDatabase`, all DAOs, `NoteRepository`, `MoodRepository`, `FoodLogRepository`, `FoodCacheRepository`, `EnergyRepository` |
| `diagnosticsModule` | `QueryLogger`, `DebugSettings`, `ExceptionReporter` |
| `authModule` | `TokenStore`, `GoogleAuthManager` |
| `repositoryModule` | `TaskRepository`, `CalendarRepository`, `DailyTaskRepository`, `HealthConnectRepository`, `SequenceRepository`, `ResponseTemplateRepository`, `ClaudeVisionClient`, `FoodProductLookupService`, `UsdaFdcApiClient`, `OpenFoodFactsApiClient`, `ProductRepository` |
| `syncModule` | `WorkManager`, `SyncScheduler`, `GoogleTasksRemoteSource`, `CalendarRemoteSource`, `GoogleTasksSyncService`, `CalendarSyncService`, `DriveBackupService`, `SyncOrchestrator` |
| `viewModelModule` | All 11 ViewModels |

---

## Notifications

| Channel | Purpose | Priority |
|---|---|---|
| `mood_channel` | Hourly mood check-in with emoji action buttons | Default |
| `reminders_channel` | Task reminders | High |
| `sequences_channel` | Active sequence step progress | Default |

---

## Error Handling

- `GlobalErrorHandler`: singleton `SharedFlow<String>`, consumed by a `Snackbar` in `MainActivity`.
- `ExceptionReporter`: logs to Logcat ERROR; if debug mode on, emits to `GlobalErrorHandler`.
- `QueryLogger`: records any DB operation taking > 10 seconds (max 200 entries in-memory, newest-first).
- Sync errors: non-fatal → `Result.retry()` (WorkManager backoff); 403 → `Result.failure()` + snackbar.
- Token corruption: `TokenStore` self-heals by recreating the encrypted prefs file.
- Backup errors: swallowed entirely — never block daily reset.
