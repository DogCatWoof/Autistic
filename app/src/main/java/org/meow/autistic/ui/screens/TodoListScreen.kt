package org.meow.autistic.ui.screens

// TODO PHASE 1 - Dependencies (requires build.gradle.kts approval)
// TODO [Phase 1] Add play-services-auth for Google OAuth popup
// TODO [Phase 1] Add google-api-client-android base client
// TODO [Phase 1] Add google-api-services-tasks REST SDK
// TODO [Phase 1] Add google-api-services-calendar REST SDK
// TODO [Phase 1] Add security-crypto for EncryptedSharedPreferences (token storage)
// TODO [Phase 1] Add okhttp3:logging-interceptor for HTTP boundary logging
// TODO [Phase 1] Add INTERNET + ACCESS_NETWORK_STATE permissions to AndroidManifest.xml
// TODO [Phase 1] Add OAuth redirect intent-filter to AndroidManifest.xml

// TODO PHASE 2 - Auth Layer
// TODO [Phase 2] Create data/auth/TokenStore.kt — read/write/clear access_token, refresh_token, expiry_ms via EncryptedSharedPreferences
// TODO [Phase 2] Create data/auth/GoogleAuthManager.kt — OAuth popup via ActivityResultLauncher
// TODO [Phase 2]   Scopes: tasks (read+write) + calendar.readonly
// TODO [Phase 2]   isAuthenticated(): Boolean
// TODO [Phase 2]   getValidToken(): String — auto-refresh before expiry
// TODO [Phase 2]   signOut() — clears TokenStore

// TODO PHASE 3 - Database Migration
// TODO [Phase 3] Add to TodoEntity: googleTaskId, googleTaskListId, extraPropertiesJson, lastSyncedAt, syncStatus
// TODO [Phase 3]   syncStatus values: "local" | "synced" | "pending_push" | "pending_delete"
// TODO [Phase 3]   extraPropertiesJson: open JSON blob written into Google Tasks notes field — never displayed in UI
// TODO [Phase 3] Bump TodoDatabase to version 2, add Migration(1, 2)
// TODO [Phase 3] Create CalendarEventEntity (googleEventId, title, startAt, endAt, isAllDay, calendarId, lastSyncedAt)
// TODO [Phase 3] Create CalendarDao
// TODO [Phase 3] Add calendar_events table in TodoDatabase version 3, add Migration(2, 3)

// TODO PHASE 4 - Google Tasks Repository (Bidirectional)
// TODO [Phase 4] Create data/todo/GoogleTasksRemoteSource.kt — raw API calls only
// TODO [Phase 4]   fetchTasks(token): List<RemoteTask> — GET @default/tasks (showDeleted=true, showHidden=true)
// TODO [Phase 4]   createTask(token, task): RemoteTask
// TODO [Phase 4]   updateTask(token, task): RemoteTask
// TODO [Phase 4]   deleteTask(token, googleTaskId)
// TODO [Phase 4] Create data/todo/GoogleTasksSyncService.kt — sync orchestration (no HTTP here)
// TODO [Phase 4]   pushPending() — flush syncStatus=pending_push|pending_delete to remote, then mark synced
// TODO [Phase 4]   pullAndMerge() — fetch remote, upsert into Room, delete locally anything deleted on remote
// TODO [Phase 4]   Conflict rule: remote wins on pull; local edits queued as pending_push, flushed before next pull
// TODO [Phase 4] Update TodoRepository: markPendingPush(id), markPendingDelete(id), upsertFromRemote(tasks), deleteSyncedIds(ids)
// TODO [Phase 4] Update TodoDao: add queries for pending_push, pending_delete, upsert by googleTaskId

// TODO PHASE 5 - Google Calendar Repository (Read-Only)
// TODO [Phase 5] Create data/calendar/CalendarRemoteSource.kt — read-only
// TODO [Phase 5]   fetchEvents(token, timeMin, timeMax): List<RemoteEvent> — rolling 60-day window
// TODO [Phase 5]   fetchDeletedEvents(token, syncToken): List<RemoteEvent> — incremental sync via Google syncToken
// TODO [Phase 5] Create data/calendar/CalendarSyncService.kt
// TODO [Phase 5]   pullAndMerge() — upsert events, delete cancelled events, store syncToken for next incremental pull
// TODO [Phase 5]   Trigger full re-sync if syncToken expires (Google invalidates after ~7 days of no sync)

// TODO PHASE 6 - Sync Worker & Scheduler
// TODO [Phase 6] Create data/sync/SyncWorker.kt (CoroutineWorker)
// TODO [Phase 6]   Step 1: getValidToken() — abort with retry if unavailable
// TODO [Phase 6]   Step 2: GoogleTasksSyncService.pushPending()
// TODO [Phase 6]   Step 3: GoogleTasksSyncService.pullAndMerge()
// TODO [Phase 6]   Step 4: CalendarSyncService.pullAndMerge()
// TODO [Phase 6] Create data/sync/SyncScheduler.kt
// TODO [Phase 6]   WiFi worker: Constraints(UNMETERED) + PeriodicWork(15 min)
// TODO [Phase 6]   Cellular worker: Constraints(CONNECTED) + PeriodicWork(1 hour) — fires as fallback when not on WiFi
// TODO [Phase 6]   triggerImmediate() — OneTimeWorkRequest for manual sync button

// TODO PHASE 7 - ViewModel & UI
// TODO [Phase 7] Add to TodoViewModel: syncState: StateFlow<SyncState> (Idle|Syncing|Error|LastSynced)
// TODO [Phase 7] Add to TodoViewModel: isAuthenticated: StateFlow<Boolean>
// TODO [Phase 7] Add to TodoViewModel: triggerSync() — checks network type, calls SyncScheduler.triggerImmediate()
// TODO [Phase 7] Add to TodoViewModel: authenticate(launcher) — delegates to GoogleAuthManager
// TODO [Phase 7] Add sync icon button to top bar — spins during Syncing, shows last-synced time when Idle
// TODO [Phase 7] Show "Connect Google Tasks" banner when !isAuthenticated — tapping triggers OAuth popup
// TODO [Phase 7] Show cellular confirmation dialog before manual sync on metered network
// TODO [Phase 7] Never render extraPropertiesJson or Google Tasks notes field in any UI element

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
