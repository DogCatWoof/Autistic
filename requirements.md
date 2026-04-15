# Autistic — Requirements

## Functional Requirements

### Task Management
- Users can create, edit, complete, and delete tasks
- Tasks have a title, optional due date, category, and optional reminder
- Categories: There is only one category
- Completing a task marks it done; removes it from the list
- Tasks with reminders trigger a local notification at the due time
- Task list view shows only open (not completed or deleted) tasks by default
- Calendar Events: 
    - Calendar events from the user's primary Google Calendar are fetched and stored locally
    - Events are fetched over a rolling 60-day window
    - Events are displayed alongside tasks in the daily and task views
- Daily Tasks: A separate management interface in Settings
    - Holds a list of tasks that repeat every day
    - Tasks have an optional Time (HH:mm), but no specific Date
    - Daily Reset Logic (Worker):
        - Runs every day at midnight (via WorkManager)
        - Removes any *unfinished* tasks in the main Task list that were generated from Daily Tasks
        - Re-adds all entries from the Daily Tasks list into the main Task list for the new day
        - Sets the due date to "Today" while preserving the original time from the Daily
- Time Tracking: Tasks support an `expectedTimeMinutes` field for duration estimation

### Google Tasks Sync
- Users can connect a Google account via OAuth to enable sync
- Local tasks sync bidirectionally with the Google Tasks API (@default list)
- Changes made locally are pushed to Google Tasks; remote changes are pulled locally
- Conflict rule: remote wins on pull; local edits are queued and pushed before the next pull
- A manual sync button is available in the top bar; it shows a spinner during sync and the last-synced time when idle
- A "Connect Google Tasks" banner is shown when the user is not authenticated
- On a metered (cellular) network, a confirmation dialog is shown before a manual sync

### Daily View
- A daily view combines tasks and calendar events for the current day

### Scan and Lookup Groceries
- Users can scan grocery product barcodes using the device camera (icon: Barcode)
- A camera preview fills the scan screen; a live barcode detector identifies the first valid barcode
- On barcode detection, the camera stops and a local product lookup is performed by barcode code
- The screen displays the matched product's stored JSON: product name, brand, quantity, ingredients, and nutrition facts where present
- If the barcode is not found in the local database, a "Product not found" message is shown
- A "Scan again" button resets the camera to scan a new barcode
- Camera permission is requested at runtime before the camera preview starts; if denied, an explanation and a settings-link are shown
- Scan is unavailable (with a prompt to sync) until the product database has been populated at least once

### Notes
- Users can write, edit and save free-form notes
  - Should have a list of existing notes with just the title, or first (line that will fit on screen) show.
  - Click on an entry in the list that brings up a view that can edit the note, or delete it.
  - Swipe to delete a note
  - Save to data base
  - Add a create (+) on list to create a new note.

### Settings
- Users can connect their Google account
- Connected account email is displayed
- Users can grant or revoke notification permission
- A test notification button is available for verification
- Daily Tasks Management: A dedicated screen to add/edit/delete repeating daily items
- Diagnostics: A "Query Log" screen displays the performance (duration in ms) of database operations
- Navigation Preferences: Users can customize which items appear in the navigation drawer/bottom bar

#### Sync Section (Settings)
- A "Sync" section lists data sources that can be downloaded locally
- Open Food Facts: a "Sync Products" button downloads the full product database from `https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz`, decompresses it on-device, parses each CSV row, and upserts a `barcode → JSON` record into the local product table
- Sync products only daily, when on wifi
- During sync, progress is shown (e.g. "Downloading…", "Importing X of Y rows…"); the button is disabled while a sync is in progress
- On completion, the last-synced timestamp is displayed beneath the button
- On failure, an error message is shown with a retry option
- The sync runs as a background WorkManager task so it survives the app being backgrounded

### Conversation Scaffolding (Social Support)
Reduces three loads in real-time social situations: decoding intent, deciding what to say, and managing timing.

**Core loop (sub-second)**
- Push-to-talk captures a short audio window (MVP); continuous listening as an extension
- On-device transcription → classify intent, sentiment, question type, ambiguity
- Generate 2–4 candidate response chips + 1 clarifying question
- Display as quick taps; optionally spoken via earbud (whisper TTS)

**UI**
- Default: single "next move" line; expandable panel with alternatives
- Color/edge cues for urgency (e.g. direct question requiring answer)
- Turn-taking indicator (speaking pace, gaps; "safe to speak now" cue)
- Pace coach: nudge if speaking too long or fast

**Response types**: direct answer, clarifier, deferral ("I need a minute"), social glue, boundary-setting

**Context modeling**
- Conversation state: topic, roles (peer/manager/service), formality
- User preferences: tone (neutral/polite/direct), verbosity
- Known scripts: recurring situations (check-in desk, meetings, small talk)

**Learning loop**: track selected/edited suggestions; adjust preferred tone, length, and per-contact patterns

**Failure modes**
- Mis-transcription → neutral fallback ("Could you repeat that?")
- Low-assist mode: turn-taking cues + one suggestion only
- Latency spike → cached generic responses

**Privacy architecture**
- On-device ASR + intent model by default; no raw audio stored; ephemeral buffers
- Per-contact opt-in indicator (visible to others if required by law/policy)
- No audio/text sent to external services without explicit user consent

**Hardware integration**: earbud tap gestures, watch haptics for turn-taking/overload, lock-screen widget

**Architecture** (offline-first):
1. Fully local (MVP default): on-device STT + intent classifier + template generation + TTS
2. Hybrid: local for common cases; optional cloud for complex phrasing
3. Cloud-first: not the default; requires explicit opt-in
- Design principle: never fail because internet is unavailable; only improve when it exists

**MVP scope**: push-to-talk, on-device transcription, simple intent classifier (question/command/other), 3 response templates per class with tone settings, quick-tap UI, end-of-session feedback

**Extensions**: meeting mode (action items), script packs (medical visits, interviews), multilingual layer

### Energy Budgeting
Models cognitive load over time as a pacing system, not just a to-do list. Prevents overload by simulating the rest of the day before decisions are made.

**Core model**
- Daily capacity represented as finite energy units
- Each activity has a cost; recovery activities restore units
- Costs are personalized and learned over time (not fixed estimates)
- Recovery is nonlinear: short breaks = partial recovery; longer quiet periods = full restoration

**User inputs (lightweight)**
- Start-of-day check: sleep quality, baseline stress, physical state
- Optional quick tags during the day: "focused work", "meeting", "errand", "social", "commute"
- End-of-block 1–2 tap rating: easier / normal / harder than expected
- Avoid requiring constant logging; infer as much as possible

**Automatic signals**
- Calendar parsing (meetings vs. solo work)
- Location transitions (home → store → home)
- Wearables if available (heart rate, HRV)
- Phone usage patterns (screen time bursts, app categories)

**Key features**
- Live budget bar: remaining units with a confidence band (not a single number)
- Forward projection: "If you attend this meeting at 3pm, projected end-of-day balance: −2 units (overload risk: high)"
- Swap suggestions: propose moving/splitting tasks to keep balance ≥ 0
- Stop cues: alert when next activity cost exceeds remaining capacity
- Recovery prescriptions: suggest short interventions that have worked before

**Personalization loop**
- After each day: compare predicted vs. actual fatigue; update per-activity costs
- Track hidden costs: transitions, commuting, context switching
- Modes: Conservative (higher cost estimates), Learning (explore to improve model), Protection (hard cap, blocks new commitments when projected negative)

**Visualizations**: day timeline colored by load (green → red), weekly overload patterns, trigger correlations (noise, social density, sleep vs. energy dips)

**Integrations**: Google Calendar (auto-tag meetings), wearables, optional clinician export

**Privacy**: on-device modeling by default; user can disable any signal source; inspectable logs

**MVP scope**: manual tagging + calendar import, fixed initial cost table with quick user adjustments, live budget bar + forward projection, end-of-day calibration

---

## Technical Requirements

### Authentication & Token Storage
- OAuth 2.0 via Google Sign-In with scopes: Tasks (read+write) and Calendar (read-only)
- Tokens (access token, refresh token, expiry) stored in `EncryptedSharedPreferences`
- Tokens are auto-refreshed before expiry; sign-out clears all stored tokens

### Sync Pipeline
Sync runs as a 4-step sequence (abort with retry if no valid token):
1. Validate authentication token
2. Push local pending changes to Google Tasks (`pending_push` → create/update; `pending_delete` → delete)
3. Pull and merge remote tasks into local Room database
4. Pull and merge Google Calendar events (read-only)

### Sync Scheduling
- WiFi (unmetered): periodic background sync every 15 minutes via WorkManager
- Cellular (connected): periodic background sync every 1 hour (fallback)
- Manual: one-time immediate sync via `triggerImmediate()`

### Local Database
- Room database (SQLite), currently version 3 (implemented as `TaskDatabase`)
- `TaskEntity` tracks: title, completion, `Instant` timestamps, category, reminder flag, Google Task mapping fields (`googleTaskId`, `googleTaskListId`), `syncStatus`, `lastSyncedAt`, `extraPropertiesJson`, `dailyTaskId` (parent link), and `expectedTimeMinutes`
- `syncStatus` values: `local` | `synced` | `pending_push` | `pending_delete`
- `CalendarEventEntity` tracks: Google event ID, title, start/end times, all-day flag, calendar ID, last synced timestamp
- `DailyTaskEntity` stores the templates for repeating tasks
- Schema migrations must be provided for each version bump; no destructive migrations

### Data Integrity
- `extraPropertiesJson` is an internal metadata field stored inside Google Tasks notes; it must never be displayed in any UI element
- Google Tasks notes field content must never be shown in the UI

### Incremental Calendar Sync
- After the initial full fetch, subsequent calendar pulls use Google's `syncToken` for incremental updates
- If the `syncToken` expires (after ~7 days without sync), fall back to a full re-sync

### Sync State (ViewModel)
- `syncState: StateFlow<SyncState>` with values: `Idle` | `Syncing` | `Error` | `LastSynced`
- `isAuthenticated: StateFlow<Boolean>` drives UI banner visibility

### Product Database
- A `ProductEntity` table stores `barcode` (primary key, text) and `productJson` (text — the raw JSON-serialised CSV row as a key/value map)
- Lookups are by exact barcode string; no full-text or partial search is required
- The table is populated exclusively by the Open Food Facts sync worker; no manual inserts from the UI

### Open Food Facts Sync Worker
- Implemented as a `CoroutineWorker` managed by WorkManager (one-time, on-demand)
- Downloads the `.csv.gz` file with an HTTP GET; streams the response body directly through a `GZIPInputStream` to avoid writing the compressed file to disk
- Parses the CSV line-by-line; the first line is the header row — use it to build a column-name index
- Each data row is converted to a `Map<String, String>` (column name → value) serialised to JSON, then upserted into `ProductEntity` by barcode (first column, `code`)
- Rows with an empty or blank `code` field are skipped
- Upserts are batched (e.g. 500 rows per transaction) to bound memory usage
- Worker exposes progress via `setProgress` so the UI can display live row counts
- The worker stores the completion timestamp in `DataStore<Preferences>` on success; this value is displayed in Settings

### Diagnostics
- `QueryLogger` records the execution time of every database repository method
- Data is stored in memory and displayed in the "Query Log" settings screen for performance monitoring

### Testing
- Unit tests must use mocks; no test may require a live network or external service connection
  _(see global `~/.claude/CLAUDE.md` — "Tests must be deterministic and not depend on external services")_
