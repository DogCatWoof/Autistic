package org.meow.autistic.ui.screens

// TODO #1: Add Google Play Services Auth and Google API Client dependencies to build.gradle.kts
// DONE #2: Configure Google Cloud Console with OAuth 2.0 Client ID for Android
// TODO #3: Create a GoogleAuthManager to handle OAuth sign-in, token storage, and refreshing

// TODO #4: Update TodoEntity to include googleTaskId (String?), etag (String?), and lastSynced (Long)
// TODO #5: Add 'notes' field to TodoEntity to store the raw string from Google Tasks
// TODO #6: Create a data class (e.g., TodoMetadata) for custom JSON properties not in Google Tasks
// TODO #7: Implement a JSON parser/stripper to separate user-visible notes from the metadata blob

// TODO #8: Implement TodoSyncWorker using WorkManager for background synchronization
// TODO #9: Configure SyncWorker constraints: 15m interval on WiFi, 1h on Cellular
// TODO #10: Add a manual sync function to TodoRepository that triggers a one-time WorkRequest

// TODO #11: Create GoogleTasksService to wrap API calls (List, Insert, Patch, Delete)
// TODO #12: Create GoogleCalendarService to fetch and sync primary calendar events
// TODO #13: Implement conflict resolution logic (Local vs Remote timestamps)

// TODO #14: Add 'Sync' button to TodoListScreen TopAppBar
// TODO #15: Add an 'Account' or 'Login' button to trigger the OAuth popup if tokens are missing
// TODO #16: Update TodoItem UI to hide the JSON metadata string from the notes display

// DONE #17 PHASE 1 - Dependencies (requires build.gradle.kts approval)
// DONE #18 [Phase 1] Add play-services-auth for Google OAuth popup
// DONE #19 [Phase 1] Add google-api-client-android base client
// DONE #20 [Phase 1] Add google-api-services-tasks REST SDK
// DONE #21 [Phase 1] Add google-api-services-calendar REST SDK
// DONE #22 [Phase 1] Add security-crypto for EncryptedSharedPreferences (token storage)
// DONE #23 [Phase 1] Add okhttp3:logging-interceptor for HTTP boundary logging
// DONE #24 [Phase 1] Add INTERNET + ACCESS_NETWORK_STATE permissions to AndroidManifest.xml
// DONE #25 [Phase 1] Add OAuth redirect intent-filter to AndroidManifest.xml

// DONE #26 PHASE 2 - Auth Layer
// DONE #27 [Phase 2] Create data/auth/TokenStore.kt — read/write/clear access_token, refresh_token, expiry_ms via EncryptedSharedPreferences
// DONE #28 [Phase 2] Create data/auth/GoogleAuthManager.kt — OAuth popup via ActivityResultLauncher
// DONE #29 [Phase 2]   Scopes: tasks (read+write) + calendar.readonly
// DONE #30 [Phase 2]   isAuthenticated(): Boolean
// DONE #31 [Phase 2]   getValidToken(): String — auto-refresh before expiry
// DONE #32 [Phase 2]   signOut() — clears TokenStore

// TODO #33 PHASE 3 - Database Migration
// TODO #34 [Phase 3] Add to TodoEntity: googleTaskId, googleTaskListId, extraPropertiesJson, lastSyncedAt, syncStatus
// TODO #35 [Phase 3]   syncStatus values: "local" | "synced" | "pending_push" | "pending_delete"
// TODO #36 [Phase 3]   extraPropertiesJson: open JSON blob written into Google Tasks notes field — never displayed in UI
// TODO #37 [Phase 3] Bump TodoDatabase to version 2, add Migration(1, 2)
// TODO #38 [Phase 3] Create CalendarEventEntity (googleEventId, title, startAt, endAt, isAllDay, calendarId, lastSyncedAt)
// TODO #39 [Phase 3] Create CalendarDao
// TODO #40 [Phase 3] Add calendar_events table in TodoDatabase version 3, add Migration(2, 3)

// TODO #41 PHASE 4 - Google Tasks Repository (Bidirectional)
// TODO #42 [Phase 4] Create data/todo/GoogleTasksRemoteSource.kt — raw API calls only
// TODO #43 [Phase 4]   fetchTasks(token): List<RemoteTask> — GET @default/tasks (showDeleted=true, showHidden=true)
// TODO #44 [Phase 4]   createTask(token, task): RemoteTask
// TODO #45 [Phase 4]   updateTask(token, task): RemoteTask
// TODO #46 [Phase 4]   deleteTask(token, googleTaskId)
// TODO #47 [Phase 4] Create data/todo/GoogleTasksSyncService.kt — sync orchestration (no HTTP here)
// TODO #48 [Phase 4]   pushPending() — flush syncStatus=pending_push|pending_delete to remote, then mark synced
// TODO #49 [Phase 4]   pullAndMerge() — fetch remote, upsert into Room, delete locally anything deleted on remote
// TODO #50 [Phase 4]   Conflict rule: remote wins on pull; local edits queued as pending_push, flushed before next pull
// TODO #51 [Phase 4] Update TodoRepository: markPendingPush(id), markPendingDelete(id), upsertFromRemote(tasks), deleteSyncedIds(ids)
// TODO #52 [Phase 4] Update TodoDao: add queries for pending_push, pending_delete, upsert by googleTaskId

// TODO #53 PHASE 5 - Google Calendar Repository (Read-Only)
// TODO #54 [Phase 5] Create data/calendar/CalendarRemoteSource.kt — read-only
// TODO #55 [Phase 5]   fetchEvents(token, timeMin, timeMax): List<RemoteEvent> — rolling 60-day window
// TODO #56 [Phase 5]   fetchDeletedEvents(token, syncToken): List<RemoteEvent> — incremental sync via Google syncToken
// TODO #57 [Phase 5] Create data/calendar/CalendarSyncService.kt
// TODO #58 [Phase 5]   pullAndMerge() — upsert events, delete cancelled events, store syncToken for next incremental pull
// TODO #59 [Phase 5]   Trigger full re-sync if syncToken expires (Google invalidates after ~7 days of no sync)

// TODO #60 PHASE 6 - Sync Worker & Scheduler
// TODO #61 [Phase 6] Create data/sync/SyncWorker.kt (CoroutineWorker)
// TODO #62 [Phase 6]   Step 1: getValidToken() — abort with retry if unavailable
// TODO #63 [Phase 6]   Step 2: GoogleTasksSyncService.pushPending()
// TODO #64 [Phase 6]   Step 3: GoogleTasksSyncService.pullAndMerge()
// TODO #65 [Phase 6]   Step 4: CalendarSyncService.pullAndMerge()
// TODO #66 [Phase 6] Create data/sync/SyncScheduler.kt
// TODO #67 [Phase 6]   WiFi worker: Constraints(UNMETERED) + PeriodicWork(15 min)
// TODO #68 [Phase 6]   Cellular worker: Constraints(CONNECTED) + PeriodicWork(1 hour) — fires as fallback when not on WiFi
// TODO #69 [Phase 6]   triggerImmediate() — OneTimeWorkRequest for manual sync button

// TODO #70 PHASE 7 - ViewModel & UI
// TODO #71 [Phase 7] Add to TodoViewModel: syncState: StateFlow<SyncState> (Idle|Syncing|Error|LastSynced)
// TODO #72 [Phase 7] Add to TodoViewModel: isAuthenticated: StateFlow<Boolean>
// TODO #73 [Phase 7] Add to TodoViewModel: triggerSync() — checks network type, calls SyncScheduler.triggerImmediate()
// TODO #74 [Phase 7] Add to TodoViewModel: authenticate(launcher) — delegates to GoogleAuthManager
// TODO #75 [Phase 7] Add sync icon button to top bar — spins during Syncing, shows last-synced time when Idle
// TODO #76 [Phase 7] Show "Connect Google Tasks" banner when !isAuthenticated — tapping triggers OAuth popup
// TODO #77 [Phase 7] Show cellular confirmation dialog before manual sync on metered network
// TODO #78 [Phase 7] Never render extraPropertiesJson or Google Tasks notes field in any UI element

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.meow.autistic.data.todo.TodoEntity
import java.text.SimpleDateFormat
import java.util.*

private val TODO_CATEGORIES = listOf("General", "Work", "Personal", "Health")

@Composable
fun TodoListScreen(viewModel: TodoViewModel = viewModel()) {
    val todos by viewModel.allTodos.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(todos) { todo ->
                TodoItem(
                    todo = todo,
                    onToggle = { viewModel.update(todo.copy(isCompleted = it)) },
                    onDelete = { viewModel.delete(todo) }
                )
            }
        }

        if (showAddDialog) {
            AddTodoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { task, dueAt, category, reminder ->
                    viewModel.insert(
                        TodoEntity(
                            task = task,
                            dueAt = dueAt,
                            category = category,
                            isCompleted = false,
                            reminderSet = reminder,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TodoItem(todo: TodoEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(checked = todo.isCompleted, onCheckedChange = onToggle)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = todo.task,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (todo.isCompleted) dimColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (todo.dueAt != null) {
                        Text(
                            text = "Due: ${dateFormatter.format(Date(todo.dueAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = dimColor
                        )
                    }
                    if (todo.category.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(todo.category) }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, String, Boolean) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var reminderSet by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminderSet, onCheckedChange = { reminderSet = it })
                    Text("Remind me")
                    Icon(
                        imageVector = if (reminderSet) Icons.Default.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp),
                        tint = if (reminderSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TODO_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(taskText, null, category, reminderSet) },
                enabled = taskText.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
