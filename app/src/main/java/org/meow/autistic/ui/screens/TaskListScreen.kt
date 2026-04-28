package org.meow.autistic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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


private enum class AddType { Task, Calendar, DailyTask }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = koinViewModel(),
    dailyViewModel: DailyTasksViewModel = koinViewModel(),
) {
    val grouped by viewModel.groupedItems.collectAsState()
    val completedItems by viewModel.completedItems.collectAsState()
    val showCompleted by viewModel.showCompleted.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf<AddType?>(null) }
    val fabRotation by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        label = "fab_rotation",
    )
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

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
                    IconButton(onClick = { viewModel.toggleShowCompleted() }) {
                        Icon(
                            if (showCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = if (showCompleted) "Show active tasks" else "Show completed tasks",
                        )
                    }
                    SyncStatusIcon(
                        syncState = syncState,
                        isAuthenticated = isAuthenticated,
                        onSyncClick = { viewModel.triggerSync() }
                    )
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = fabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        SpeedDialItem("Daily Task") { fabExpanded = false; addType = AddType.DailyTask }
                        SpeedDialItem("Calendar Event") { fabExpanded = false; addType = AddType.Calendar }
                        SpeedDialItem("Task") { fabExpanded = false; addType = AddType.Task }
                    }
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.rotate(fabRotation),
                    )
                }
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
                if (showCompleted) {
                    if (completedItems.isEmpty()) {
                        item { SectionHeader("No completed tasks in the last 7 days") }
                    } else {
                        stickyHeader(key = "header_completed") { SectionHeader("Completed") }
                        items(completedItems, key = { it.itemKey }) { item ->
                            TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it }, onEventClick = { selectedEvent = it })
                        }
                    }
                } else {
                    if (grouped.pastDue.isNotEmpty()) {
                        stickyHeader(key = "header_past_due") { SectionHeader("Past Due") }
                        items(grouped.pastDue, key = { it.itemKey }) { item ->
                            TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it }, onEventClick = { selectedEvent = it })
                        }
                    }
                    stickyHeader(key = "header_today") {
                        val todayDate = LocalDate.now(ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))
                        SectionHeader("Today — $todayDate", background = ColorToday, contentColor = Color.White)
                    }
                    items(grouped.today, key = { it.itemKey }) { item ->
                        TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it }, onEventClick = { selectedEvent = it })
                    }
                    grouped.later.forEach { (dateLabel, date, sectionItems) ->
                        val weekEnd = LocalDate.now(ZoneId.systemDefault()).with(DayOfWeek.SUNDAY)
                        val bg = when {
                            date != null && !date.isAfter(weekEnd) -> ColorThisWeek
                            else -> ColorFuture
                        }
                        stickyHeader(key = "header_later_$dateLabel") { SectionHeader(dateLabel, background = bg, contentColor = Color.White) }
                        items(sectionItems, key = { it.itemKey }) { item ->
                            TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it }, onEventClick = { selectedEvent = it })
                        }
                    }
                }
            }
        }

        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { fabExpanded = false },
            )
        }

        when (addType) {
            AddType.Task -> AddTaskDialog(
                onDismiss = { addType = null },
                onConfirm = { task, dueAt, reminderMinutesBefore, notes, expectedTime, isImportant, isRequired ->
                    viewModel.insert(
                        TaskEntity(
                            task = task,
                            dueAt = dueAt,
                            notes = notes,
                            isCompleted = false,
                            reminderSet = reminderMinutesBefore != null,
                            reminderMinutesBefore = reminderMinutesBefore,
                            createdAt = Instant.now(),
                            syncStatus = "local",
                            expectedTimeMinutes = expectedTime,
                            isImportant = isImportant,
                            isRequired = isRequired,
                        )
                    )
                    addType = null
                },
            )
            AddType.Calendar -> AddCalendarEventDialog(
                onDismiss = { addType = null },
                onConfirm = { title, startAt, endAt, isAllDay ->
                    viewModel.insertCalendarEvent(
                        CalendarEventEntity(
                            googleEventId = "local_${System.currentTimeMillis()}",
                            title = title,
                            startAt = startAt,
                            endAt = endAt,
                            isAllDay = isAllDay,
                            calendarId = "",
                            lastSyncedAt = Instant.now(),
                            syncStatus = "local",
                        )
                    )
                    addType = null
                },
            )
            AddType.DailyTask -> DailyTaskDialog(
                initial = null,
                onSave = { task ->
                    dailyViewModel.insert(task)
                    addType = null
                },
                onDismiss = { addType = null },
            )
            null -> Unit
        }

        selectedEvent?.let { event ->
            CalendarEventDialog(
                event = event,
                onDismiss = { selectedEvent = null },
                onDone = {
                    viewModel.completeEvent(event)
                    selectedEvent = null
                },
                onDelete = {
                    viewModel.deleteEvent(event)
                    selectedEvent = null
                },
            )
        }

        selectedTask?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = { selectedTask = null },
                onSave = { taskText, reminderMinutesBefore, notes, expectedTime, isImportant, isRequired ->
                    viewModel.update(task.copy(
                        task = taskText,
                        reminderSet = reminderMinutesBefore != null,
                        reminderMinutesBefore = reminderMinutesBefore,
                        notes = notes,
                        expectedTimeMinutes = expectedTime,
                        isImportant = isImportant,
                        isRequired = isRequired,
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
fun SectionHeader(
    title: String,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private val ColorToday = Color(0xFF4A7C59)
private val ColorThisWeek = Color(0xFF6B8F5E)
private val ColorFuture = Color(0xFF8A7A4A)

@Composable
fun TaskListItemRow(
    item: TaskListItem,
    viewModel: TaskViewModel,
    onTaskClick: (TaskEntity) -> Unit,
    onEventClick: (CalendarEventEntity) -> Unit,
) {
    val now = Instant.now()
    val bgColor = resolveItemColor(item, now)
    val icon = resolveItemIcon(item)
    when (item) {
        is TaskListItem.Task -> TaskItem(
            task = item.entity,
            backgroundColor = bgColor,
            leadingIcon = icon,
            onToggle = { viewModel.update(item.entity.copy(isCompleted = it)) },
            onDelete = { viewModel.delete(item.entity) },
            onClick = { onTaskClick(item.entity) },
        )
        is TaskListItem.Event -> CalendarEventItem(
            event = item.entity,
            backgroundColor = bgColor,
            leadingIcon = icon,
            viewModel = viewModel,
            onClick = { onEventClick(item.entity) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventItem(
    event: CalendarEventEntity,
    viewModel: TaskViewModel,
    backgroundColor: Color = Color.Unspecified,
    leadingIcon: ImageVector,
    onClick: () -> Unit = {},
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> viewModel.deleteEvent(event)
            SwipeToDismissBoxValue.EndToStart -> viewModel.completeEvent(event)
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isDelete by remember {
                derivedStateOf {
                    try { dismissState.requireOffset() > 0f } catch (_: IllegalStateException) { false }
                }
            }
            val bgColor = if (isDelete) MaterialTheme.colorScheme.errorContainer
                          else MaterialTheme.colorScheme.primaryContainer
            val alignment = if (isDelete) Alignment.CenterStart else Alignment.CenterEnd
            val padding = if (isDelete) Modifier.padding(start = 24.dp) else Modifier.padding(end = 24.dp)
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.CheckCircle,
                    contentDescription = if (isDelete) "Delete" else "Complete",
                    tint = if (isDelete) MaterialTheme.colorScheme.onErrorContainer
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = padding
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(if (backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.surface else backgroundColor)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 0.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
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
fun TaskItem(
    task: TaskEntity,
    backgroundColor: Color = Color.Unspecified,
    leadingIcon: ImageVector,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onDelete()
            SwipeToDismissBoxValue.EndToStart -> onToggle(!task.isCompleted)
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isDelete by remember {
                derivedStateOf {
                    try { dismissState.requireOffset() > 0f } catch (_: IllegalStateException) { false }
                }
            }
            val bgColor = if (isDelete) MaterialTheme.colorScheme.errorContainer
                          else MaterialTheme.colorScheme.primaryContainer
            val alignment = if (isDelete) Alignment.CenterStart else Alignment.CenterEnd
            val padding = if (isDelete) Modifier.padding(start = 24.dp) else Modifier.padding(end = 24.dp)
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.CheckCircle,
                    contentDescription = if (isDelete) "Delete" else "Complete",
                    tint = if (isDelete) MaterialTheme.colorScheme.onErrorContainer
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = padding
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.surface else backgroundColor)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
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

@Composable
private fun SpeedDialItem(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 6.dp,
            shadowElevation = 2.dp,
            onClick = onClick,
        ) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.Add, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startAt: Instant, endAt: Instant, isAllDay: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var startHour by remember { mutableStateOf("09") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMinute by remember { mutableStateOf("00") }

    fun toInstant(date: LocalDate, hour: String, minute: String): Instant =
        date.atTime(hour.toIntOrNull() ?: 0, minute.toIntOrNull() ?: 0)
            .atZone(ZoneId.systemDefault()).toInstant()

    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Calendar Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    Text("All day")
                }
                Text("Start: ${startDate}", style = MaterialTheme.typography.labelMedium)
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = startHour, onValueChange = { startHour = it.take(2) },
                            label = { Text("HH") }, singleLine = true,
                            modifier = androidx.compose.ui.Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(":", modifier = androidx.compose.ui.Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(
                            value = startMinute, onValueChange = { startMinute = it.take(2) },
                            label = { Text("MM") }, singleLine = true,
                            modifier = androidx.compose.ui.Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                Text("End: ${endDate}", style = MaterialTheme.typography.labelMedium)
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = endHour, onValueChange = { endHour = it.take(2) },
                            label = { Text("HH") }, singleLine = true,
                            modifier = androidx.compose.ui.Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(":", modifier = androidx.compose.ui.Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(
                            value = endMinute, onValueChange = { endMinute = it.take(2) },
                            label = { Text("MM") }, singleLine = true,
                            modifier = androidx.compose.ui.Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startAt = if (isAllDay) startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                               else toInstant(startDate, startHour, startMinute)
                    val endAt = if (isAllDay) endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                              else toInstant(endDate, endHour, endMinute)
                    onConfirm(title, startAt, endAt, isAllDay)
                },
                enabled = canSave,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Instant?, Int?, String?, Int?, Boolean, Boolean) -> Unit,
) {
    var taskText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expectedTimeText by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderMinutesText by remember { mutableStateOf("15") }
    var isImportant by remember { mutableStateOf(false) }
    var isRequired by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
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
                    Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    Text("Remind me")
                    Icon(
                        imageVector = if (reminderEnabled) Icons.Default.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp),
                        tint = if (reminderEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderMinutesText,
                        onValueChange = { reminderMinutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes before due") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                    Text("Important")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isRequired, onCheckedChange = { isRequired = it })
                    Text("Required")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminder = if (reminderEnabled) reminderMinutesText.toIntOrNull() ?: 15 else null
                    onConfirm(taskText, null, reminder, notesText.takeIf { it.isNotBlank() }, expectedTimeText.toIntOrNull(), isImportant, isRequired)
                },
                enabled = taskText.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CalendarEventDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd · HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (event.isAllDay) {
                    Text("All day", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        dateFormatter.format(Date.from(event.startAt)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "– ${dateFormatter.format(Date.from(event.endAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("Done") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String, Int?, String?, Int?, Boolean, Boolean) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var taskText by remember { mutableStateOf(task.task) }
    var notesText by remember { mutableStateOf(task.notes ?: "") }
    var expectedTimeText by remember { mutableStateOf(task.expectedTimeMinutes?.toString() ?: "") }
    val initialReminderEnabled = task.reminderMinutesBefore != null || task.reminderSet
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var reminderMinutesText by remember { mutableStateOf(task.reminderMinutesBefore?.toString() ?: "15") }
    var isImportant by remember { mutableStateOf(task.isImportant) }
    var isRequired by remember { mutableStateOf(task.isRequired) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
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
                    Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    Text("Remind me")
                    Icon(
                        imageVector = if (reminderEnabled) Icons.Default.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp),
                        tint = if (reminderEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderMinutesText,
                        onValueChange = { reminderMinutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes before due") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                    Text("Important")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isRequired, onCheckedChange = { isRequired = it })
                    Text("Required")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminder = if (reminderEnabled) reminderMinutesText.toIntOrNull() ?: 15 else null
                    onSave(taskText, reminder, notesText.takeIf { it.isNotBlank() }, expectedTimeText.toIntOrNull(), isImportant, isRequired)
                },
                enabled = taskText.isNotBlank(),
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
        },
    )
}
