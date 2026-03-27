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

### Scan
- Users can scan grocery product barcodes using the device camera (icon: Barcode)
- A camera preview fills the scan screen; a live barcode detector identifies the first valid barcode
- On barcode detection, the camera stops and a product lookup is triggered automatically
- Product data is fetched from `https://world.openfoodfacts.org/api/v0/product/{barcode}.json`
- The screen displays the returned product information: product name, brand, quantity, ingredients, and nutrition facts (energy, fat, carbohydrates, sugars, protein, salt — per 100 g where available)
- If the product is not found (404 or empty `product` object) a "Product not found" message is shown
- If the network call fails, an error message is shown with a retry option
- A "Scan again" button resets the camera to scan a new barcode
- Camera permission is requested at runtime before the camera preview starts; if denied, an explanation and a settings-link are shown

### Notes
- Users can write and save free-form notes (not yet implemented — placeholder)

### Settings
- Users can connect their Google account
- Connected account email is displayed
- Users can grant or revoke notification permission
- A test notification button is available for verification

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

### Testing
- Unit tests must use mocks; no test may require a live network or external service connection
  _(see global `~/.claude/CLAUDE.md` — "Tests must be deterministic and not depend on external services")_
