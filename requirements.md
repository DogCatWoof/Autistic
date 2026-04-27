# Autistic — Requirements

## Functional Requirements

### Task Management
- Users can create, edit, complete, and delete tasks
- Tasks have a title, optional due date, and optional reminder
- Completing a task marks it done and removes it from the active list
- Tasks with reminders trigger a local notification at the specified time before the due date
- Task list shows only open (not completed) tasks by default
- Tasks support an `expectedTimeMinutes` field for duration estimation

#### Calendar Events
- Calendar events from the user's primary Google Calendar are fetched and stored locally
- Events are fetched over a rolling 60-day window
- Events are displayed alongside tasks in the task list, sorted by date

#### Daily Tasks
- A separate list of task templates that repeat every day, managed in Settings
- Each template has a title, optional time (HH:mm), and optional expected duration
- Daily Reset Worker (WorkManager, midnight):
  - Deletes unfinished tasks in the main list that were generated from Daily Tasks
  - Re-inserts all Daily Task templates as new tasks due today at their stored time

### Google Tasks Sync
- Users connect a Google account via OAuth to enable sync
- Local tasks sync bidirectionally with the Google Tasks API (@default list)
- Conflict rule: remote wins on pull; local edits are queued and pushed before the next pull
- A manual sync button in the top bar shows a spinner during sync and the last-synced timestamp when idle
- A "Connect Google Tasks" banner is shown when the user is not authenticated
- [X] On a metered (cellular) network, a confirmation dialog is shown before manual sync

### Scan and Lookup Groceries
- Users scan grocery product barcodes using the device camera
- A live barcode detector identifies the first valid code; the camera pauses on detection
- The matched product's name, brand, quantity, ingredients, and nutrition facts are displayed
- If the barcode is not in the local database, a "Product not found" message is shown
- A "Scan again" button resets the camera
- Camera permission is requested at runtime; if denied, an explanation and settings link are shown

### Notes
- Users can create, edit, and delete free-form notes
- The note list shows each note's title or first line (truncated to one line)
- Tapping a note opens an editor where the full content can be modified or the note deleted
- Swipe-to-delete is available on the list
- A create (+) FAB adds a new note
- Notes are persisted in the local database

### Settings
- Connect / disconnect a Google account; connected account email is displayed
- Grant or revoke notification permission; a test notification button is available
- Daily Tasks: dedicated screen to add, edit, and delete repeating task templates
- Navigation Preferences: choose which items appear in the navigation bar
- Backup & Restore: manual backup to Google Drive; manual restore with confirmation prompt; last backup timestamp displayed
- Diagnostics: Query Log (database operation timings); Logcat viewer

#### Sync Section (Settings)
- Google Tasks + Calendar sync: manual trigger and background schedule (see Sync Scheduling)
- Daily Reset: shows last-run date and a manual "Run Now" button

### Food Log
- Users log daily food intake; each entry records description, calories, protein, fat, carbs, fiber, sugars, added sugars, and sugar alcohols
- Entries are organized by day with prev/next navigation; daily nutrition totals are displayed with a calorie breakdown bar
- **Manual entry**: a dialog with an optional description field and numeric nutrient inputs; autocomplete suggests from the local food cache
- **Photo entry**: single "Analyze" button in the photo preview; AI classifies the photo as food, product, or unknown
  - **Food**: Claude estimates nutrition per serving; results shown in an editable dialog with an editable name field
  - **Product/barcode**: app checks the food cache by name, then queries USDA FDC and Open Food Facts by barcode; if found, results shown in editable dialog
  - **Not found**: user is prompted to take a photo of the nutrition label; OCR extracts values and presents editable dialog
- Accepted entries are saved to the food cache (keyed by description) for future autocomplete
- Per-serving nutrients are multiplied by a user-specified servings count before saving
- Stale in-flight analysis items (app killed mid-analysis) are cleared on startup

### Conversation Scaffolding (Social Support)
Reduces three loads in real-time social situations: decoding intent, deciding what to say, and managing timing.

- **Core loop**: push-to-talk captures audio → on-device transcription → intent/sentiment classification → generate 2–4 response chips + 1 clarifying question → display as tappable chips; optionally spoken via earbud TTS
- **UI**: single "next move" line by default; expandable panel for alternatives; color/edge urgency cues; turn-taking indicator ("safe to speak now"); pace coach
- **Response types**: direct answer, clarifier, deferral ("I need a minute"), social glue, boundary-setting
- **Context modeling**: conversation state (topic, roles, formality); user tone preference (neutral/polite/direct); known scripts for recurring situations
- **Learning loop**: track selected/edited suggestions; adjust preferred tone, length, and per-contact patterns
- **Failure modes**: mis-transcription → neutral fallback; low-assist mode (turn-taking + one suggestion only); latency spike → cached generic responses
- **Privacy**: on-device ASR + intent model by default; no raw audio stored; ephemeral buffers; no data sent externally without explicit consent
- **Hardware integration**: earbud tap gestures, watch haptics, lock-screen widget
- **Architecture** (offline-first): (1) fully local — MVP default; (2) hybrid — local for common cases, optional cloud for complex phrasing; (3) cloud-first — explicit opt-in only. Principle: never fail without internet; only improve when it exists
- **MVP scope**: push-to-talk, on-device transcription, rule-based intent classifier (question/command/social/other), 3 response templates per class with tone settings, quick-tap UI, end-of-session feedback
- **Extensions**: meeting mode (action items), script packs (medical/interview), multilingual layer

### [X] Location-based Actions
- [X] Users define geofence triggers: label, location (map pick or address), radius, and action
- [X] Actions: show a reminder notification, open a specific screen, or add a predefined task
- [X] Examples: "Arrive at supermarket → show shopping list"; "Leave work → log commute"
- [X] Triggers are managed in Settings with an on/off toggle per trigger
- [X] Permissions required: `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- [X] Geofences re-register on device reboot via `BootCompletedReceiver`

### Sequences
- A sequence is an ordered checklist of steps for a complex, repeating multi-part task
- Each step has an instruction, optional estimated time, and a completion checkbox
- Examples: morning routine, packing for a trip, preparing for a medical visit
- Users start a "run"; the current step is highlighted and progress is saved
- A run can be ended early; completed runs are stored as history
- Sequences are managed in Settings; an active run surfaces as a persistent notification

### Energy Budgeting
Models cognitive load over time as a pacing system, not just a to-do list. Prevents overload by simulating the rest of the day before decisions are made.

- **Core model**: daily capacity in finite energy units; each activity has a cost; recovery restores units; costs are personalized and learned over time; recovery is nonlinear (short break = partial; long quiet period = full)
- **User inputs**: start-of-day check (sleep quality, stress, physical state); optional quick tags during the day; 1–2 tap end-of-block rating (easier/normal/harder); minimal logging — infer when possible
- [X] **Automatic signals**: calendar events, location transitions, wearables (HR/HRV), phone usage patterns
- **Key features**: live budget bar with confidence band; forward projection ("If you attend this meeting, projected end-of-day balance: −2 units"); [X] swap suggestions; [X] stop cues; [X] recovery prescriptions
- **Personalization loop**: daily predicted-vs-actual comparison; weighted update of per-activity costs; [X] track hidden costs (transitions, context switching)
- [X] **Modes**: Conservative (higher cost estimates), Learning (explore to improve model), Protection (hard cap, blocks new commitments when projected negative)
- [X] **Visualizations**: day timeline green→red; weekly overload patterns; trigger correlations
- [X] **Integrations**: Google Calendar (auto-tag meetings), wearables, optional clinician export
- **Privacy**: on-device modeling; [X] user can disable any signal; inspectable logs
- **MVP scope**: manual tagging + [X] calendar import, fixed initial cost table with quick user adjustments, live budget bar + forward projection, [X] end-of-day calibration (DailyCalibrationWorker not wired)

---

## Technical Requirements

### Authentication & Token Storage
- OAuth 2.0 via Google Sign-In with scopes: Tasks (read+write), Calendar (read-only), Drive File (read+write for backup)
- Tokens (access token, expiry) stored in `EncryptedSharedPreferences`; auto-refreshed before expiry
- Sign-out clears all stored tokens

### Sync Pipeline
Runs as a 4-step sequence (abort with retry if no valid token):
1. Validate authentication token
2. Push local pending changes to Google Tasks (`pending_push` → create/update; `pending_delete` → delete)
3. Pull and merge remote tasks into local Room database
4. Pull and merge Google Calendar events (read-only)

### Sync Scheduling
- Wi-Fi (unmetered): periodic background sync every 15 minutes via WorkManager
- Cellular: periodic background sync every 1 hour
- Manual: one-time immediate sync via `triggerImmediate()`

### Local Database
- Room database (SQLite), `TaskDatabase`, currently version 1
- `TaskEntity`: title, completion, `Instant` timestamps, reminder flag, Google Task mapping fields (`googleTaskId`, `googleTaskListId`), `syncStatus`, `lastSyncedAt`, `extraPropertiesJson`, `dailyTaskId`, `expectedTimeMinutes`, `reminderMinutesBefore`
- `syncStatus` values: `local` | `synced` | `pending_push` | `pending_delete`
- `CalendarEventEntity`: Google event ID, title, start/end times, all-day flag, calendar ID, last synced timestamp, `isHidden`
- `DailyTaskEntity`: repeating task templates
- `NoteEntity`, `MoodEntity`, `KetoLogEntry`: separate concerns, same database
- Schema migrations required for every version bump; no destructive migrations in production

### Data Integrity
- `extraPropertiesJson` is an internal metadata field embedded in Google Tasks notes; never displayed in any UI element
- Google Tasks notes field content must never be shown in the UI

### Incremental Calendar Sync
- After the initial full fetch, subsequent pulls use Google's `syncToken` for incremental updates
- If the `syncToken` expires (~7 days without sync), fall back to a full re-sync

### Sync State (ViewModel)
- `syncState: StateFlow<SyncState>` with values: `Idle` | `Syncing` | `Error` | `LastSynced`
- `isAuthenticated: StateFlow<Boolean>` drives UI banner visibility

### Product Database
- Separate `ProductDatabase` with a `ProductEntity` table: `barcode` (PK, text), `productJson` (text)
- Lookups by exact barcode string; no full-text search required

### Backup & Restore
- Backup: WAL checkpoint → AES-256-GCM encrypt (Android Keystore key) → upload SQLite file to Google Drive as `autistic_db_backup.enc`; timestamp stored in DataStore
- Drive location: folder named "Autism Backups" in the user's My Drive; retain the last 7 daily backups
- Restore: download → decrypt → close Room singleton → overwrite DB file → delete stale WAL/SHM → process restart
- Legacy unencrypted backups (`autistic_db_backup.sqlite`) supported as a restore fallback
- Encryption key is device-specific (Android Keystore); cross-device restore is not supported
- Automatic backup runs daily (WorkManager, Wi-Fi only); manual backup and restore available in Settings
- On first sign-in after reinstall, app checks Drive for a backup and offers a restore prompt

### Diagnostics
- `QueryLogger` records execution time of every database repository call
- Displayed in the "Query Log" settings screen

### Testing
- Unit tests must use mocks; no test may require a live network or external service connection
- All instrumented tests run against a clean database (cleared in `@Before`)

### Event List — Rules-Based Coloring, Icons, and Importance

#### Coloring
Each list item gets a pastel background determined by a priority-ordered rule set (first match wins):

| Priority | Condition | Color |
|---|---|---|
| 1 | `isImportant = true` | Amber pastel `#FFF8E1` |
| 2 | Past due | Red pastel `#FFEBEE` |
| 3 | Due within 2 hours | Orange pastel `#FFF3E0` |
| 4 | Due today | Blue pastel `#E3F2FD` |
| 5 | `isRequired = true` | Purple pastel `#F3E5F5` |
| 6 | Default | Surface (no tint) |

Rules are evaluated at render time in `ItemColorResolver`; no color data is persisted.

#### Icons
Each item row shows a leading icon:
- Regular task → `Icons.Default.CheckBoxOutlineBlank`
- Calendar event → `Icons.Default.Event`
- Daily task (generated from template) → `Icons.Default.Repeat`
- Important (any) → `Icons.Default.Star` overlaid or substituted
- Required (any) → `Icons.Default.Flag`

#### New Fields
- `TaskEntity`: add `isImportant: Boolean = false`, `isRequired: Boolean = false`
- `DailyTaskEntity`: add `isRequired: Boolean = false` (propagated when daily tasks are generated)
- `CalendarEventEntity`: no new fields (color by time rules only)

#### UI Changes
- `AddTaskDialog` / `EditTaskDialog`: add Important and Required toggles
- `DailyTaskDialog`: add Required toggle
- `TaskItem` / `CalendarEventItem`: accept computed `backgroundColor` and `leadingIcon` parameters
- `TaskListItemRow`: compute color and icon, pass to item composables

---

### Health Connect Integration

Reads health data from other apps (Samsung Health, Garmin, Google Fit, etc.) via Android's Health Connect platform.

#### What to pull
| Data Type | Usage in app |
|---|---|
| `StepsRecord` | Activity display; Energy Budgeting signal |
| `SleepSessionRecord` | Mood screen context; Energy Budgeting baseline |
| `HeartRateRecord` | Energy Budgeting signal; stress indicator |
| `WeightRecord` | Body metrics display |
| `TotalCaloriesBurnedRecord` | Food log context (calories in vs. out) |
| `BloodGlucoseRecord` | Optional; relevant for keto/diet tracking |
| `NutritionRecord` | Pull nutrition from other logging apps |

#### Permissions declared in manifest
`android.permission.health.READ_STEPS`, `READ_SLEEP`, `READ_HEART_RATE`, `READ_WEIGHT`, `READ_TOTAL_CALORIES_BURNED`, `READ_BLOOD_GLUCOSE`, `READ_NUTRITION`

#### Requirements
- Health Connect is built into Android 14+; users on Android 8–13 must install the Health Connect app from Play Store
- A privacy policy must be linked in both the app and Play Store listing
- Health data must never be used for advertising or sold to third parties
- The user must explicitly consent before any read; permissions revocable at any time from the Health Connect settings

---

## Plan

### [X] Location-based Actions
1. [X] **Data layer** — `LocationTriggerEntity` (id, label, latitude, longitude, radiusMeters, actionType, actionPayload, isEnabled); `LocationTriggerDao`; `LocationTriggerRepository`
2. [X] **Room migration** — bump DB version; add table
3. [X] **Geofencing** — `GeofenceManager` wraps `GeofencingClient`: registers/deregisters geofences; `GeofenceBroadcastReceiver` dispatches to `LocationActionHandler` (notification, task enqueue, or deep-link)
4. [X] **Boot receiver** — `BootCompletedReceiver` re-registers all enabled geofences on restart
5. [X] **Permissions** — runtime request for `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION`; settings warning when background permission missing
6. [X] **ViewModel + Screen** — `LocationTriggerViewModel`; `LocationTriggersScreen` (list with toggle + map thumbnail); map-pick flow
7. [X] **Tests** — unit tests for `LocationActionHandler` dispatch logic; mock `GeofencingClient` in repository tests

### Sequences (MVP)
1. **Data layer** — `SequenceEntity` (id, name); `SequenceStepEntity` (id, sequenceId, instruction, estimatedMinutes, position); `SequenceRunEntity` (id, sequenceId, startedAt, completedAt); `SequenceStepProgressEntity` (runId, stepId, completedAt); `SequenceDao`; `SequenceRepository`
2. **Room migration** — bump DB to version 16; four new tables
3. **ViewModels** — `SequenceViewModel` (sequence/step CRUD); `SequenceRunViewModel` (active run state via `flatMapLatest`, `startRun()`, `completeStep()`, `endRun()`)
4. **Screens** — `SequenceListScreen` (sequence management + active run card with progress bar, current step, Done/End buttons); Settings entry navigates here
5. **Persistent notification** — `SequenceRunNotificationManager` object; `SequenceStepReceiver` handles Done/End actions from notification; DB version 16
6. **Settings integration** — Sequences entry under Tasks in Settings
7. **Tests** — `SequenceRepositoryTest` (unit); `SequenceRunViewModelTest` (unit, mocks notification manager); `SequenceDaoTest` (instrumented)

### Conversation Scaffolding (MVP)
1. **ResponseTemplateRepository** — static JSON asset: `IntentClass → List<ResponseTemplate>` (3 per class, 3 tone variants); loaded at startup, cached in memory
2. **IntentClassifier** — rule-based (keyword + question-mark heuristic): `Question | Command | Social | Sarcasm | Unknown` with confidence score
3. **ConversationViewModel** — `transcribedText`, `chips`, `isListening` StateFlows; drives `SpeechRecognizer` (push-to-talk); pipes through classifier → templates
4. **ConversationScreen** — push-to-talk button; chip row; expandable alternatives; urgency strip; turn-taking indicator
5. **TonePreferencesStore** — DataStore for tone and verbosity; exposed in Settings
6. **TTS** — `TextToSpeech` for earbud output; optional, toggleable
7. **Nav tab** — add "Talk" tab to `BOTTOM_ITEMS`
8. **Tests** — unit tests for `IntentClassifier` across all classes; chip selection ordering

### Event List — Rules-Based Coloring, Icons, Importance, and Required

1. **Schema** — add `isImportant: Boolean = false` and `isRequired: Boolean = false` to `TaskEntity`; add `isRequired: Boolean = false` to `DailyTaskEntity`; bump DB version to 11
2. **`ItemColorResolver.kt`** — pure `@Composable`-free function: takes `(item: TaskListItem, nowMillis: Long) → Color`; encodes the 5-rule priority table using hardcoded pastel `Color` values; no external dependencies
3. **`ItemIconResolver.kt`** — pure function: takes `(item: TaskListItem) → ImageVector`; maps item type and flags to Material icons
4. **`TaskListItemRow`** — call both resolvers; pass `backgroundColor` and `leadingIcon` down to `TaskItem` / `CalendarEventItem`
5. **`TaskItem` / `CalendarEventItem`** — accept `backgroundColor: Color` and `leadingIcon: ImageVector`; apply `background(backgroundColor)` to the row container; add `Icon(leadingIcon)` as the first element in the row
6. **Dialogs** — add `isImportant` / `isRequired` toggle rows to `AddTaskDialog`, `EditTaskDialog`, and `DailyTaskDialog`
7. **DailyResetWorker** — propagate `isRequired` from `DailyTaskEntity` when generating `TaskEntity` rows each midnight
8. **Tests** — unit tests for `ItemColorResolver`: one test per rule, verify correct pastel returned; test tie-breaking order; test that `isImportant` beats past-due

### Health Connect Integration

1. **Dependency** — add `androidx.health.connect:connect-client:1.1.0` to `app/build.gradle.kts`; add `READ_HEALTH_DATA_IN_BACKGROUND` and the 7 type-specific `READ_*` permissions to `AndroidManifest.xml`; add a `<activity-alias>` for `ViewPermissionUsageActivity` (required by Play Store)
2. **`HealthConnectRepository.kt`** — wraps `HealthConnectClient`; one suspend function per data type: `readTodaySteps()`, `readLastNightSleep()`, `readRecentHeartRate()`, `readLatestWeight()`, `readTodayCaloriesBurned()`, `readRecentBloodGlucose()`, `readTodayNutrition()`; all return nullable domain objects; `HealthConnectClient.getSdkStatus()` checked before every call
3. **`HealthConnectPermissionRequest`** — composable helper that calls `rememberLauncherForActivityResult(HealthDataRequestPermissions())`; invoked from a Settings entry; shows which permissions are granted vs. missing
4. **Settings screen entry** — "Health Connect" row in Settings; opens permission request flow; shows last-synced timestamp per data type
5. **Surface in existing screens** — Food Log: show today's steps + calories burned as a summary card; Mood: show last night's sleep duration + avg heart rate; Energy Budgeting: feed all signals into the model
6. [X] **`HealthConnectSyncWorker`** — `CoroutineWorker`; reads each data type and writes to a `HealthSnapshotEntity` table (date, steps, sleepMinutes, avgHeartRate, weightKg, caloriesBurned); runs on any network, once per hour
7. **Privacy policy** — add a privacy policy URL to app metadata and Play Store listing before shipping; document that health data stays on-device only
8. **Tests** — unit tests for `HealthConnectRepository` using a fake `HealthConnectClient`; verify null-safe handling when SDK unavailable

### Energy Budgeting (MVP)
1. **Data layer** — `EnergyProfileEntity` (dailyCapacity, mode); `ActivityCostEntity` (activityType, baseCost, learnedCost, sampleCount); `EnergyLogEntity` (date, activityType, startAt, endAt, reportedDifficulty); `StartOfDayCheckEntity` (date, sleepQuality, stressLevel, physicalState, baselineMultiplier); DAOs; `EnergyRepository`
2. **Room migration** — bump DB version; add tables
3. **EnergyRepository** — `getTodayBalance()`, `getProjection(pendingActivities)`, `calibrateDay()` (weighted moving-average cost update)
4. **ViewModel** — `EnergyViewModel`: `todayBalance`, `projection` StateFlows; `logActivity()`, `submitStartOfDay()`, `rateLastBlock()`
5. **EnergyScreen** — budget bar with confidence band; day timeline; start-of-day bottom sheet; quick-tag FAB; forward projection card
6. [X] **DailyCalibrationWorker** — midnight; calls `calibrateDay()` for closing day
7. [X] **Calendar integration** — annotate calendar events with estimated energy cost on fetch; meeting keywords map to activity types
8. **Nav tab** — add "Energy" tab
9. **Tests** — unit tests for balance calculation, projection logic, calibration math
