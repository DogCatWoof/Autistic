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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.task.TaskEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel = koinViewModel()) {
    val grouped by viewModel.groupedItems.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }

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
                Icon(Icons.Default.Add, contentDescription = "Add Task")
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
                        TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it })
                    }
                }
                if (grouped.today.isNotEmpty()) {
                    item(key = "header_today") { SectionHeader("Today") }
                    items(grouped.today, key = { it.itemKey }) { item ->
                        TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it })
                    }
                }
                grouped.later.forEach { (dateLabel, sectionItems) ->
                    item(key = "header_later_$dateLabel") { SectionHeader(dateLabel) }
                    items(sectionItems, key = { it.itemKey }) { item ->
                        TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { task, dueAt, reminder, notes, expectedTime ->
                    viewModel.insert(
                        TaskEntity(
                            task = task,
                            dueAt = dueAt,
                            notes = notes,
                            isCompleted = false,
                            reminderSet = reminder,
                            createdAt = Instant.now(),
                            syncStatus = "local",
                            expectedTimeMinutes = expectedTime,
                        )
                    )
                    showAddDialog = false
                }
            )
        }

        selectedTask?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = { selectedTask = null },
                onSave = { taskText, reminder, notes, expectedTime ->
                    viewModel.update(task.copy(
                        task = taskText,
                        reminderSet = reminder,
                        notes = notes,
                        expectedTimeMinutes = expectedTime,
                    ))
                    selectedTask = null
                },
                onComplete = {
                    viewModel.update(task.copy(isCompleted = !task.isCompleted))
                    selectedTask = null
                },
                onDelete = {
                    viewModel.delete(task)
                    selectedTask = null
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
fun TaskListItemRow(item: TaskListItem, viewModel: TaskViewModel, onTaskClick: (TaskEntity) -> Unit) {
    when (item) {
        is TaskListItem.Task -> TaskItem(
            task = item.entity,
            onToggle = { viewModel.update(item.entity.copy(isCompleted = it)) },
            onDelete = { viewModel.delete(item.entity) },
            onClick = { onTaskClick(item.entity) },
        )
        is TaskListItem.Event -> CalendarEventItem(item.entity, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventItem(event: CalendarEventEntity, viewModel: TaskViewModel) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> viewModel.completeEvent(event)
            SwipeToDismissBoxValue.EndToStart -> viewModel.deleteEvent(event)
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
                    imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Delete,
                    contentDescription = if (isComplete) "Complete" else "Delete",
                    tint = if (isComplete) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = padding
                )
            }
        }
    ) {
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
                            "${dateFormatter.format(Date.from(event.startAt))} – ${dateFormatter.format(Date.from(event.endAt))}",
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(task: TaskEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onToggle(!task.isCompleted)
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
                    imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Delete,
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
                .clickable { onClick() }
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
                        text = task.task,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.isCompleted) dimColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.dueAt != null) {
                        Text(
                            text = "Due: ${dateFormatter.format(Date.from(task.dueAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = dimColor
                        )
                    }
                    if (!task.notes.isNullOrEmpty()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = dimColor
                        )
                    }
                }
                if (task.expectedTimeMinutes != null) {
                    val h = task.expectedTimeMinutes / 60
                    val m = task.expectedTimeMinutes % 60
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
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Instant?, Boolean, String?, Int?) -> Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?, Int?) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var taskText by remember { mutableStateOf(task.task) }
    var notesText by remember { mutableStateOf(task.notes ?: "") }
    var expectedTimeText by remember { mutableStateOf(task.expectedTimeMinutes?.toString() ?: "") }
    var reminderSet by remember { mutableStateOf(task.reminderSet) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
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
                        tint = if (reminderSet) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(taskText, reminderSet, notesText.takeIf { it.isNotBlank() }, expectedTimeText.toIntOrNull())
                },
                enabled = taskText.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onComplete) {
                    Text(if (task.isCompleted) "Reopen" else "Complete")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
