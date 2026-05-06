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

### Sequences
- A sequence is an ordered checklist of steps for a complex, repeating multi-part task
- Each step has an instruction, optional estimated time, and a completion checkbox
- Examples: morning routine, packing for a trip, preparing for a medical visit
- Users start a "run"; the current step is highlighted and progress is saved
- A run can be ended early; completed runs are stored as history
- Sequences are managed in Settings; an active run surfaces as a persistent notification

---

## Technical Requirements

### Authentication & Token Storage
- OAuth 2.0 via Google Sign-In with scopes: Tasks (read+write), Calendar (read-only), Drive File (read+write for backup)
- Tokens (access token, expiry) stored in `EncryptedSharedPreferences`; auto-refreshed before expiry
- Firebase Auth linked via Google ID token after successful sign-in; `FirebaseAuth.getInstance().currentUser.uid` used for Firestore security rules
- Sign-out clears all stored tokens and Firebase session

### Sync Pipeline
Runs as a 6-step sequence (abort with retry if no valid token):
1. Validate authentication token
2. Push local pending changes to Google Tasks (`pending_push` → create/update; `pending_delete` → delete)
3. Pull and merge remote tasks into local Room database
4. Pull and merge Google Calendar events (read-only)
5. Push pending local changes to Firestore (non-fatal; skipped if no Firebase user)
6. Pull incremental Firestore changes into Room (non-fatal; skipped if no Firebase user)

Firestore failures are caught and logged — they never abort steps 1–4.

### Sync Scheduling
- Wi-Fi (unmetered): periodic background sync every 15 minutes via WorkManager
- Cellular: periodic background sync every 1 hour
- Manual: one-time immediate sync via `triggerImmediate()`

### Local Database
- Room database (SQLite), `TaskDatabase`, currently version 18
- Entities include Firestore sync metadata: `firestoreId` (String?), `lastModifiedAt` (Instant), `pendingFirestoreSync` (Boolean), `isDeleted` (Boolean)
- `TaskEntity`: title, completion, `Instant` timestamps, reminder flag, Google Task mapping fields (`googleTaskId`, `googleTaskListId`), `syncStatus`, `lastSyncedAt`, `extraPropertiesJson`, `dailyTaskId`, `expectedTimeMinutes`, `reminderMinutesBefore`
- `syncStatus` values: `local` | `synced` | `pending_push` | `pending_delete`
- `CalendarEventEntity`: Google event ID, title, start/end times, all-day flag, calendar ID, last synced timestamp, `isHidden`
- `DailyTaskEntity`: repeating task templates
- `NoteEntity`, `MoodEntity`, `KetoLogEntry`: separate concerns, same database
- Schema migrations required for every version bump; no destructive migrations in production

### Data Integrity
- `extraPropertiesJson` is an internal metadata field embedded in Google Tasks notes; never displayed in any UI element
- Google Tasks notes field content must never be shown in the UI

### Firestore Sync
- Room is the source of truth; all writes go to Room first, Firestore is sync target
- Push: records with `pendingFirestoreSync = true` are upserted/deleted in Firestore, then flag cleared
- Pull: documents modified since last pull are merged into Room using last-write-wins (local wins ties)
- Conflict resolution: local wins if newer; remote wins if newer; ties → local wins
- Soft-deleted records (`isDeleted = true`) are propagated to Firestore then hard-deleted from Room
- Collection structure: `users/{uid}/{tasks|notes|moods|foodLogItems|healthSnapshots|sequences|sequenceSteps|sequenceRuns|dailyTasks}`
- Firestore security rules restrict access to `request.auth.uid == userId`

### Incremental Calendar Sync
- After the initial full fetch, subsequent pulls use Google's `syncToken` for incremental updates
- If the `syncToken` expires (~7 days without sync), fall back to a full re-sync

### Sync State (ViewModel)
- `syncState: StateFlow<SyncState>` with values: `Idle` | `Syncing` | `Error` | `LastSynced`
- `isAuthenticated: StateFlow<Boolean>` drives UI banner visibility

### Product Database
- Separate `ProductDatabase` with a `ProductEntity` table: `barcode` (PK, text), `productJson` (text)
- Lookups by exact barcode string; no full-text search required

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

### Integrated Health Screen

A dedicated screen that surfaces accumulated health metrics for a single selected day, with navigation between days and a weekly summary popup.

#### Layout
- **Day navigator** — header bar with back/forward chevrons and the selected date (e.g. "Mon Apr 27"); tapping the date label opens the weekly popup; defaults to today; forward arrow disabled on today
- **Top section** — summary cards for the selected day:
  - Calories (food log intake vs. Health Connect burned, shown as in/out balance)
  - Blood sugar (latest reading for the day + daily average)
  - Steps (day total)
  - Sleep (hours for the night ending on this day)
  - Heart rate (average for the day)
  - Weight (most recent reading on or before this day)
- **Bottom section** — scrollable list of individual Health Connect records for the selected day; each row shows data type, value, and time; empty state per section when no data

#### Weekly Popup
- Triggered by tapping the date label in the day navigator
- Shows a 7-day summary ending on the selected day
- Contains charts and diagrams (specific charts TBD — candidates: step bar chart, sleep duration bar chart, calorie balance line, blood sugar scatter, weight trend line)
- Dismisses on tap-outside or back gesture

#### Requirements
- Data sourced from `HealthSnapshotEntity` (populated by `HealthConnectSyncWorker`) and the food log for calorie intake
- Pull-to-refresh on the day view triggers `refreshTodaySnapshot()` (only active when viewing today)
- Navigating to a past day with no snapshot triggers a background fetch for that date
- Empty states shown per metric when no Health Connect data is available for the selected day

---

## Plan

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
6. **`HealthConnectSyncWorker`** — `CoroutineWorker`; reads each data type and writes to a `HealthSnapshotEntity` table (date, steps, sleepMinutes, avgHeartRate, weightKg, caloriesBurned); runs on any network, once per hour
7. **Privacy policy** — add a privacy policy URL to app metadata and Play Store listing before shipping; document that health data stays on-device only
8. **Tests** — unit tests for `HealthConnectRepository` using a fake `HealthConnectClient`; verify null-safe handling when SDK unavailable
