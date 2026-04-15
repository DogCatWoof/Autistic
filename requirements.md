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
- On a metered (cellular) network, a confirmation dialog is shown before manual sync

### Daily View
- Combines tasks and calendar events for the current day in a single chronological list

### Scan and Lookup Groceries
- Users scan grocery product barcodes using the device camera
- A live barcode detector identifies the first valid code; the camera pauses on detection
- The matched product's name, brand, quantity, ingredients, and nutrition facts are displayed
- If the barcode is not in the local database, a "Product not found" message is shown
- A "Scan again" button resets the camera
- Camera permission is requested at runtime; if denied, an explanation and settings link are shown
- Scan is unavailable until the product database has been populated at least once

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
- Open Food Facts: "Sync Products" button downloads the full product database, decompresses on-device, parses each CSV row, and upserts `barcode → JSON` into the local product table
- Products sync at most once per day and only on Wi-Fi
- Progress is shown during sync ("Downloading…", "Importing X of Y rows…"); button is disabled while running
- Last-synced timestamp is displayed on completion; errors show a retry option
- Sync runs as a WorkManager task so it survives backgrounding
- Google Tasks + Calendar sync: manual trigger and background schedule (see Sync Scheduling)
- Daily Reset: shows last-run date and a manual "Run Now" button

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

### Habit Stabilizer
- Users define repeating habits (daily or weekly) they want to build or track
- Each habit displays a current streak and a completion history (calendar heatmap)
- Tapping a habit marks it complete for today; undo is available within the session
- Habits carry an optional energy cost that feeds into the Energy Budgeting model
- Habits can be reordered and archived; archived habits are hidden but their history is retained
- Managed in Settings (no separate nav tab); habits surface on the Task screen or a dedicated panel

### Location-based Actions
- Users define geofence triggers: label, location (map pick or address), radius, and action
- Actions: show a reminder notification, open a specific screen, or add a predefined task
- Examples: "Arrive at supermarket → show shopping list"; "Leave work → log commute"
- Triggers are managed in Settings with an on/off toggle per trigger
- Permissions required: `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- Geofences re-register on device reboot via `BootCompletedReceiver`

### Sequences
- A sequence is an ordered checklist of steps for a complex, repeating multi-part task
- Each step has an instruction, optional estimated time, and a completion checkbox
- Examples: morning routine, packing for a trip, preparing for a medical visit
- Users start a "run"; the current step is highlighted and progress is saved
- A run can be paused and resumed; completed runs are stored as history
- Sequences are managed in Settings; an active run surfaces as a persistent notification

### Energy Budgeting
Models cognitive load over time as a pacing system, not just a to-do list. Prevents overload by simulating the rest of the day before decisions are made.

- **Core model**: daily capacity in finite energy units; each activity has a cost; recovery restores units; costs are personalized and learned over time; recovery is nonlinear (short break = partial; long quiet period = full)
- **User inputs**: start-of-day check (sleep quality, stress, physical state); optional quick tags during the day; 1–2 tap end-of-block rating (easier/normal/harder); minimal logging — infer when possible
- **Automatic signals**: calendar events, location transitions, wearables (HR/HRV), phone usage patterns
- **Key features**: live budget bar with confidence band; forward projection ("If you attend this meeting, projected end-of-day balance: −2 units"); swap suggestions; stop cues; recovery prescriptions
- **Personalization loop**: daily predicted-vs-actual comparison; weighted update of per-activity costs; track hidden costs (transitions, context switching)
- **Modes**: Conservative (higher cost estimates), Learning (explore to improve model), Protection (hard cap, blocks new commitments when projected negative)
- **Visualizations**: day timeline green→red; weekly overload patterns; trigger correlations
- **Integrations**: Google Calendar (auto-tag meetings), wearables, optional clinician export
- **Privacy**: on-device modeling; user can disable any signal; inspectable logs
- **MVP scope**: manual tagging + calendar import, fixed initial cost table with quick user adjustments, live budget bar + forward projection, end-of-day calibration

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
- Populated exclusively by the Open Food Facts sync worker

### Open Food Facts Sync Worker
- `CoroutineWorker` managed by WorkManager (one-time, on-demand)
- Streams `.csv.gz` via HTTP GET through `GZIPInputStream` (no intermediate disk write)
- Parses CSV line-by-line; first line = header; each data row upserted as `barcode → JSON` map
- Rows with blank `code` are skipped; upserts batched (~500 rows per transaction)
- Progress reported via `setProgress`; completion timestamp stored in DataStore

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

---

## Plan

### Habit Stabilizer
1. **Data layer** — `HabitEntity` (id, name, targetFrequency, energyCostUnits, isArchived, sortOrder); `HabitCompletionEntity` (id, habitId, completedAt); `HabitDao`; `HabitRepository`
2. **Room migration** — bump DB version; add both tables; write migration script
3. **ViewModel** — `HabitViewModel`: `habits: StateFlow<List<HabitWithStreak>>`, `complete(habit)`, `undo()`, `archive(habit)`
4. **Screens** — `HabitListScreen` (streak + heatmap row per habit, tap-to-complete, swipe-to-archive); `HabitEditorDialog` (add/edit); surfaced under Settings → Habits
5. **Energy integration** — on completion of a habit with `energyCostUnits > 0`, call `EnergyRepository.logActivity()`
6. **Tests** — unit tests for streak calculation edge cases; instrumented test for tap-to-complete flow

### Location-based Actions
1. **Data layer** — `LocationTriggerEntity` (id, label, latitude, longitude, radiusMeters, actionType, actionPayload, isEnabled); `LocationTriggerDao`; `LocationTriggerRepository`
2. **Room migration** — bump DB version; add table
3. **Geofencing** — `GeofenceManager` wraps `GeofencingClient`: registers/deregisters geofences; `GeofenceBroadcastReceiver` dispatches to `LocationActionHandler` (notification, task enqueue, or deep-link)
4. **Boot receiver** — `BootCompletedReceiver` re-registers all enabled geofences on restart
5. **Permissions** — runtime request for `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION`; settings warning when background permission missing
6. **ViewModel + Screen** — `LocationTriggerViewModel`; `LocationTriggersScreen` (list with toggle + map thumbnail); map-pick flow
7. **Tests** — unit tests for `LocationActionHandler` dispatch logic; mock `GeofencingClient` in repository tests

### Sequences
1. **Data layer** — `SequenceEntity` (id, name, description); `SequenceStepEntity` (id, sequenceId, stepIndex, instruction, estimatedMinutes); `SequenceRunEntity` (id, sequenceId, startedAt, completedAt?, isPaused); `SequenceStepProgressEntity` (runId, stepId, completedAt?); DAOs; `SequenceRepository`
2. **Room migration** — bump DB version; add four tables
3. **ViewModel** — `SequenceViewModel`: sequence list; `SequenceRunViewModel`: active run state, `completeStep()`, `pause()`, `resume()`, `abandon()`
4. **Screens** — `SequenceListScreen` (list + start button); `SequenceRunScreen` (current step highlighted, progress bar); `SequenceEditorScreen` (drag-to-reorder steps)
5. **Persistent notification** — active run shows sticky notification with current step + "Mark done" action
6. **Settings integration** — Sequences entry under Settings
7. **Tests** — unit tests for step progression and pause/resume state machine

### Conversation Scaffolding (MVP)
1. **ResponseTemplateRepository** — static JSON asset: `IntentClass → List<ResponseTemplate>` (3 per class, 3 tone variants); loaded at startup, cached in memory
2. **IntentClassifier** — rule-based (keyword + question-mark heuristic): `Question | Command | Social | Sarcasm | Unknown` with confidence score
3. **ConversationViewModel** — `transcribedText`, `chips`, `isListening` StateFlows; drives `SpeechRecognizer` (push-to-talk); pipes through classifier → templates
4. **ConversationScreen** — push-to-talk button; chip row; expandable alternatives; urgency strip; turn-taking indicator
5. **TonePreferencesStore** — DataStore for tone and verbosity; exposed in Settings
6. **TTS** — `TextToSpeech` for earbud output; optional, toggleable
7. **Nav tab** — add "Talk" tab to `BOTTOM_ITEMS`
8. **Tests** — unit tests for `IntentClassifier` across all classes; chip selection ordering

### Energy Budgeting (MVP)
1. **Data layer** — `EnergyProfileEntity` (dailyCapacity, mode); `ActivityCostEntity` (activityType, baseCost, learnedCost, sampleCount); `EnergyLogEntity` (date, activityType, startAt, endAt, reportedDifficulty); `StartOfDayCheckEntity` (date, sleepQuality, stressLevel, physicalState, baselineMultiplier); DAOs; `EnergyRepository`
2. **Room migration** — bump DB version; add tables
3. **EnergyRepository** — `getTodayBalance()`, `getProjection(pendingActivities)`, `calibrateDay()` (weighted moving-average cost update)
4. **ViewModel** — `EnergyViewModel`: `todayBalance`, `projection` StateFlows; `logActivity()`, `submitStartOfDay()`, `rateLastBlock()`
5. **EnergyScreen** — budget bar with confidence band; day timeline; start-of-day bottom sheet; quick-tag FAB; forward projection card
6. **DailyCalibrationWorker** — midnight; calls `calibrateDay()` for closing day
7. **Calendar integration** — annotate calendar events with estimated energy cost on fetch; meeting keywords map to activity types
8. **Nav tab** — add "Energy" tab
9. **Tests** — unit tests for balance calculation, projection logic, calibration math

### Backup — Remaining Work
- Move backup file into a Drive folder named "Autism Backups" (currently uploaded to Drive root)
- Retain only the last 7 daily backups; delete older files after each successful backup
- Scope change: migrate from `DRIVE_FILE` to `DRIVE_APPDATA` if cross-device restore is desired (requires re-authentication)
