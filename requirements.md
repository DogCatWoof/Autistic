# Autistic — Requirements

## Legacy / Pre-Phase (superseded by phased plan below)

- [ ] #1 Add Google Play Services Auth and Google API Client dependencies to build.gradle.kts
- [x] #2 Configure Google Cloud Console with OAuth 2.0 Client ID for Android
- [ ] #3 Create a GoogleAuthManager to handle OAuth sign-in, token storage, and refreshing
- [ ] #4 Update TodoEntity to include googleTaskId (String?), etag (String?), and lastSynced (Long)
- [ ] #5 Add 'notes' field to TodoEntity to store the raw string from Google Tasks
- [ ] #6 Create a data class (e.g., TodoMetadata) for custom JSON properties not in Google Tasks
- [ ] #7 Implement a JSON parser/stripper to separate user-visible notes from the metadata blob
- [ ] #8 Implement TodoSyncWorker using WorkManager for background synchronization
- [ ] #9 Configure SyncWorker constraints: 15m interval on WiFi, 1h on Cellular
- [ ] #10 Add a manual sync function to TodoRepository that triggers a one-time WorkRequest
- [ ] #11 Create GoogleTasksService to wrap API calls (List, Insert, Patch, Delete)
- [ ] #12 Create GoogleCalendarService to fetch and sync primary calendar events
- [ ] #13 Implement conflict resolution logic (Local vs Remote timestamps)
- [ ] #14 Add 'Sync' button to TodoListScreen TopAppBar
- [ ] #15 Add an 'Account' or 'Login' button to trigger the OAuth popup if tokens are missing
- [ ] #16 Update TodoItem UI to hide the JSON metadata string from the notes display

---

## Phase 1 — Dependencies

- [x] #17 Phase 1 — Dependencies (requires build.gradle.kts approval)
- [x] #18 Add play-services-auth for Google OAuth popup
- [x] #19 Add google-api-client-android base client
- [x] #20 Add google-api-services-tasks REST SDK
- [x] #21 Add google-api-services-calendar REST SDK
- [x] #22 Add security-crypto for EncryptedSharedPreferences (token storage)
- [x] #23 Add okhttp3:logging-interceptor for HTTP boundary logging
- [x] #24 Add INTERNET + ACCESS_NETWORK_STATE permissions to AndroidManifest.xml
- [x] #25 Add OAuth redirect intent-filter to AndroidManifest.xml

## Phase 2 — Auth Layer

- [x] #26 Phase 2 — Auth Layer
- [x] #27 Create data/auth/TokenStore.kt — read/write/clear access_token, refresh_token, expiry_ms via EncryptedSharedPreferences
- [x] #28 Create data/auth/GoogleAuthManager.kt — OAuth popup via ActivityResultLauncher
- [x] #29   Scopes: tasks (read+write) + calendar.readonly
- [x] #30   isAuthenticated(): Boolean
- [x] #31   getValidToken(): String — auto-refresh before expiry
- [x] #32   signOut() — clears TokenStore

## Phase 3 — Database Migration

- [x] #33 Phase 3 — Database Migration
- [x] #34 Add to TodoEntity: googleTaskId, googleTaskListId, extraPropertiesJson, lastSyncedAt, syncStatus
- [x] #35   syncStatus values: "local" | "synced" | "pending_push" | "pending_delete"
- [x] #36   extraPropertiesJson: open JSON blob written into Google Tasks notes field — never displayed in UI
- [x] #37 Bump TodoDatabase to version 2, add Migration(1, 2)
- [x] #38 Create CalendarEventEntity (googleEventId, title, startAt, endAt, isAllDay, calendarId, lastSyncedAt)
- [x] #39 Create CalendarDao
- [x] #40 Add calendar_events table in TodoDatabase version 3, add Migration(2, 3)

## Phase 4 — Google Tasks Repository (Bidirectional)

- [x] #41 Phase 4 — Google Tasks Repository (Bidirectional)
- [x] #42 Create data/todo/GoogleTasksRemoteSource.kt — raw API calls only
- [x] #43   fetchTasks(token): List<RemoteTask> — GET @default/tasks (showDeleted=true, showHidden=true)
- [x] #44   createTask(token, task): RemoteTask
- [x] #45   updateTask(token, task): RemoteTask
- [x] #46   deleteTask(token, googleTaskId)
- [x] #47 Create data/todo/GoogleTasksSyncService.kt — sync orchestration (no HTTP here)
- [x] #48   pushPending() — flush syncStatus=pending_push|pending_delete to remote, then mark synced
- [x] #49   pullAndMerge() — fetch remote, upsert into Room, delete locally anything deleted on remote
- [x] #50   Conflict rule: remote wins on pull; local edits queued as pending_push, flushed before next pull
- [x] #51 Update TodoRepository: markPendingPush(id), markPendingDelete(id), upsertFromRemote(tasks), deleteSyncedIds(ids)
- [x] #52 Update TodoDao: add queries for pending_push, pending_delete, upsert by googleTaskId

## Phase 5 — Google Calendar Repository (Read-Only)

- [x] #53 Phase 5 — Google Calendar Repository (Read-Only)
- [x] #54 Create data/calendar/CalendarRemoteSource.kt — read-only
- [x] #55   fetchEvents(token, timeMin, timeMax): List<RemoteEvent> — rolling 60-day window
- [x] #56   fetchDeletedEvents(token, syncToken): List<RemoteEvent> — incremental sync via Google syncToken
- [x] #57 Create data/calendar/CalendarSyncService.kt
- [x] #58   pullAndMerge() — upsert events, delete cancelled events, store syncToken for next incremental pull
- [x] #59   Trigger full re-sync if syncToken expires (Google invalidates after ~7 days of no sync)

## Phase 6 — Sync Worker & Scheduler

- [x] #60 Phase 6 — Sync Worker & Scheduler
- [x] #61 Create data/sync/SyncWorker.kt (CoroutineWorker)
- [x] #62   Step 1: getValidToken() — abort with retry if unavailable
- [x] #63   Step 2: GoogleTasksSyncService.pushPending()
- [x] #64   Step 3: GoogleTasksSyncService.pullAndMerge()
- [x] #65   Step 4: CalendarSyncService.pullAndMerge()
- [x] #66 Create data/sync/SyncScheduler.kt
- [x] #67   WiFi worker: Constraints(UNMETERED) + PeriodicWork(15 min)
- [x] #68   Cellular worker: Constraints(CONNECTED) + PeriodicWork(1 hour) — fires as fallback when not on WiFi
- [x] #69   triggerImmediate() — OneTimeWorkRequest for manual sync button

## Phase 7 — ViewModel & UI

- [ ] #70 Phase 7 — ViewModel & UI
- [ ] #71 Add to TodoViewModel: syncState: StateFlow<SyncState> (Idle|Syncing|Error|LastSynced)
- [ ] #72 Add to TodoViewModel: isAuthenticated: StateFlow<Boolean>
- [ ] #73 Add to TodoViewModel: triggerSync() — checks network type, calls SyncScheduler.triggerImmediate()
- [ ] #74 Add to TodoViewModel: authenticate(launcher) — delegates to GoogleAuthManager
- [ ] #75 Add sync icon button to top bar — spins during Syncing, shows last-synced time when Idle
- [ ] #76 Show "Connect Google Tasks" banner when !isAuthenticated — tapping triggers OAuth popup
- [ ] #77 Show cellular confirmation dialog before manual sync on metered network
- [ ] #78 Never render extraPropertiesJson or Google Tasks notes field in any UI element

## General

- [ ] #79 All tests should use mocks and not require access to any external service
