package org.meow.autistic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.todo.TodoEntity
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(viewModel: TodoViewModel = koinViewModel()) {
    val grouped by viewModel.groupedItems.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                actions = {
                    SyncStatusIcon(
                        syncState = syncState,
                        isAuthenticated = isAuthenticated,
                        onSyncClick = { viewModel.triggerSync() }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!isAuthenticated) {
                GoogleAuthBanner(onConnectClick = {
                    signInLauncher.launch(viewModel.getSignInIntent())
                })
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (grouped.pastDue.isNotEmpty()) {
                    item(key = "header_past_due") { SectionHeader("Past Due") }
                    items(grouped.pastDue, key = { it.itemKey }) { item ->
                        TodoListItemRow(item, viewModel)
                    }
                }
                if (grouped.today.isNotEmpty()) {
                    item(key = "header_today") { SectionHeader("Today") }
                    items(grouped.today, key = { it.itemKey }) { item ->
                        TodoListItemRow(item, viewModel)
                    }
                }
                if (grouped.later.isNotEmpty()) {
                    item(key = "header_later") { SectionHeader("Later") }
                    items(grouped.later, key = { it.itemKey }) { item ->
                        TodoListItemRow(item, viewModel)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTodoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { task, dueAt, reminder, notes, expectedTime ->
                    viewModel.insert(
                        TodoEntity(
                            task = task,
                            dueAt = dueAt,
                            notes = notes,
                            isCompleted = false,
                            reminderSet = reminder,
                            createdAt = System.currentTimeMillis(),
                            syncStatus = "local",
                            expectedTimeMinutes = expectedTime,
                        )
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SyncStatusIcon(
    syncState: SyncState,
    isAuthenticated: Boolean,
    onSyncClick: () -> Unit
) {
    if (!isAuthenticated) return

    val rotation = remember { Animatable(0f) }
    
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Syncing) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
            rotation.snapTo(0f)
        }
    }

    IconButton(onClick = onSyncClick, enabled = syncState !is SyncState.Syncing) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync",
            modifier = Modifier.rotate(rotation.value)
        )
    }
}

@Composable
fun GoogleAuthBanner(onConnectClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Connect Google Tasks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sync your tasks across devices",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun TodoListItemRow(item: TodoListItem, viewModel: TodoViewModel) {
    when (item) {
        is TodoListItem.Task -> TodoItem(
            todo = item.entity,
            onToggle = { viewModel.update(item.entity.copy(isCompleted = it)) },
            onDelete = { viewModel.delete(item.entity) },
        )
        is TodoListItem.Event -> CalendarEventItem(item.entity)
    }
}

@Composable
fun CalendarEventItem(event: CalendarEventEntity) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(event.title, style = MaterialTheme.typography.bodyLarge)
                if (event.isAllDay) {
                    Text("All day", style = MaterialTheme.typography.bodySmall, color = dimColor)
                } else {
                    Text(
                        "${dateFormatter.format(Date(event.startAt))} – ${dateFormatter.format(Date(event.endAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = dimColor,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(todo: TodoEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onToggle(!todo.isCompleted)
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isComplete by remember {
                derivedStateOf {
                    try { dismissState.requireOffset() > 0f } catch (_: IllegalStateException) { false }
                }
            }
            val bgColor = if (isComplete) MaterialTheme.colorScheme.primaryContainer
                          else MaterialTheme.colorScheme.errorContainer
            val alignment = if (isComplete) Alignment.CenterStart else Alignment.CenterEnd
            val padding = if (isComplete) Modifier.padding(start = 24.dp) else Modifier.padding(end = 24.dp)
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isComplete) "Complete" else "Delete",
                    tint = if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = padding
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    if (!todo.notes.isNullOrEmpty()) {
                        Text(
                            text = todo.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = dimColor
                        )
                    }
                }
                if (todo.expectedTimeMinutes != null) {
                    val h = todo.expectedTimeMinutes / 60
                    val m = todo.expectedTimeMinutes % 60
                    val timeLabel = when {
                        h == 0 -> "~${m} min"
                        m == 0 -> "~${h}h"
                        else -> "~${h}h ${m}m"
                    }
                    Text(text = timeLabel, style = MaterialTheme.typography.bodySmall, color = dimColor)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Boolean, String?, Int?) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expectedTimeText by remember { mutableStateOf("") }
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

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = expectedTimeText,
                    onValueChange = { expectedTimeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected time (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        taskText,
                        null,
                        reminderSet,
                        notesText.takeIf { it.isNotBlank() },
                        expectedTimeText.toIntOrNull(),
                    )
                },
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
