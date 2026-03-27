# Autistic — Requirements

## Functional Requirements

### Todo Management
- Users can create, edit, complete, and delete tasks
- Tasks have a title, optional due date, category, and optional reminder
- Categories: General, Work, Personal, Health
- Completing a task marks it done; it remains visible until dismissed or deleted
- Tasks with reminders trigger a local notification at the due time
- Only show not completed tasks / todos
- **Today View**: A dedicated view showing tasks and events for the current day
- **Calendar Events (Read-Only)**: 
    - Calendar events from the user's primary Google Calendar are fetched and stored locally
    - Events are fetched over a rolling 60-day window
    - Events are displayed alongside tasks in the daily and task views

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
- Users can write and save free-form notes (not yet implemented — placeholder)

### Settings
- Users can connect their Google account
- Connected account email is displayed
- Users can grant or revoke notification permission
- A test notification button is available for verification

#### Sync Section (Settings)
- A "Sync" section lists data sources that can be downloaded locally
- **Open Food Facts**: a "Sync Products" button downloads the full product database from `https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz`, decompresses it on-device, parses each CSV row, and upserts a `barcode → JSON` record into the local product table
- During sync, progress is shown (e.g. "Downloading…", "Importing X of Y rows…"); the button is disabled while a sync is in progress
- On completion, the last-synced timestamp is displayed beneath the button
- On failure, an error message is shown with a retry option
- The sync runs as a background WorkManager task so it survives the app being backgrounded

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
- Room database (SQLite), currently version 3
- `TodoEntity` tracks: title, completion, timestamps, category, reminder flag, Google Task mapping fields (`googleTaskId`, `googleTaskListId`), `syncStatus`, `lastSyncedAt`, and `extraPropertiesJson`
- `syncStatus` values: `local` | `synced` | `pending_push` | `pending_delete`
- `CalendarEventEntity` tracks: Google event ID, title, start/end times, all-day flag, calendar ID, last synced timestamp
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
- Schema version must be bumped and a migration provided when the table is added

### Open Food Facts Sync Worker
- Implemented as a `CoroutineWorker` managed by WorkManager (one-time, on-demand)
- Downloads the `.csv.gz` file with an HTTP GET; streams the response body directly through a `GZIPInputStream` to avoid writing the compressed file to disk
- Parses the CSV line-by-line; the first line is the header row — use it to build a column-name index
- Each data row is converted to a `Map<String, String>` (column name → value) serialised to JSON, then upserted into `ProductEntity` by barcode (first column, `code`)
- Rows with an empty or blank `code` field are skipped
- Upserts are batched (e.g. 500 rows per transaction) to bound memory usage
- Worker exposes progress via `setProgress` so the UI can display live row counts
- The worker stores the completion timestamp in `DataStore<Preferences>` on success; this value is displayed in Settings

### Testing
- Unit tests must use mocks; no test may require a live network or external service connection
  _(see global `~/.claude/CLAUDE.md` — "Tests must be deterministic and not depend on external services")_
