package org.meow.autistic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.*
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
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.task.TaskEntity


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
    val undoStack by viewModel.undoStack.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf<AddType?>(null) }
    val fabRotation by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        label = "fab_rotation",
    )
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var selectedEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (undoStack.isNotEmpty()) {
                    IconButton(onClick = { viewModel.undo() }) {
                        BadgedBox(badge = {
                            if (undoStack.size > 1) Badge { Text("${undoStack.size}") }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                    }
                }
                IconButton(onClick = { viewModel.toggleShowCompleted() }) {
                    Icon(
                        if (showCompleted) Icons.Outlined.CheckCircle else Icons.Default.CheckCircle,
                        contentDescription = if (showCompleted) "Show active tasks" else "Show completed tasks",
                    )
                }
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
                        val today = LocalDate.now(ZoneId.systemDefault())
                        SectionHeader(
                            title = "Today",
                            centerText = today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE")),
                            trailingText = today.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")),
                            background = ColorToday,
                            contentColor = Color.White,
                        )
                    }
                    items(grouped.today, key = { it.itemKey }) { item ->
                        TaskListItemRow(item, viewModel, onTaskClick = { selectedTask = it }, onEventClick = { selectedEvent = it })
                    }
                    grouped.later.forEach { (dateLabel, date, sectionItems) ->
                        val weekEnd = LocalDate.now(ZoneId.systemDefault())
                            .with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                        val isAfterThisWeek = date == null || date.isAfter(weekEnd)
                        val isTomorrow = dateLabel == "Tomorrow"
                        val bg = if (isAfterThisWeek) ColorFuture else ColorThisWeek
                        val formattedDate = date?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
                        val formattedDay = date?.format(java.time.format.DateTimeFormatter.ofPattern("EEEE"))
                        stickyHeader(key = "header_later_${date?.toString() ?: dateLabel}") {
                            when {
                                isTomorrow -> SectionHeader(
                                    title = "Tomorrow",
                                    centerText = formattedDay,
                                    trailingText = formattedDate,
                                    background = bg,
                                    contentColor = Color.White,
                                )
                                isAfterThisWeek -> SectionHeader(
                                    dateLabel,
                                    centerTitle = true,
                                    background = bg,
                                    contentColor = ColorFutureContent,
                                )
                                else -> SectionHeader(
                                    title = formattedDay ?: dateLabel,
                                    trailingText = formattedDate,
                                    background = bg,
                                    contentColor = Color.White,
                                )
                            }
                        }
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

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
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
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.rotate(fabRotation))
            }
        }

        when (addType) {
            AddType.Task -> AddTaskDialog(
                onDismiss = { addType = null },
                onConfirm = { task, dueAt, reminderMinutesBefore, notes, expectedTime, isImportant ->
                    viewModel.insert(
                        TaskEntity(
                            task = task,
                            dueAt = dueAt,
                            notes = notes,
                            reminderMinutesBefore = reminderMinutesBefore,
                            createdAt = Instant.now(),
                            syncStatus = "local",
                            expectedTimeMinutes = expectedTime,
                            isImportant = isImportant,
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
                onSave = { taskText, reminderMinutesBefore, notes, expectedTime, isImportant ->
                    viewModel.update(task.copy(
                        task = taskText,
                        reminderMinutesBefore = reminderMinutesBefore,
                        notes = notes,
                        expectedTimeMinutes = expectedTime,
                        isImportant = isImportant,
                    ))
                    selectedTask = null
                },
                onComplete = {
                    viewModel.toggleComplete(task, !task.isCompleted)
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
fun SectionHeader(
    title: String,
    trailingText: String? = null,
    centerText: String? = null,
    centerTitle: Boolean = false,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (centerText != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                modifier = Modifier.align(Alignment.Center),
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                modifier = Modifier.align(if (centerTitle) Alignment.Center else Alignment.CenterStart),
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

private val ColorToday = Color(0xFF4A7C59)
private val ColorThisWeek = Color(0xFF6B8F5E)
private val ColorFuture = Color(0xFFD4C055)
private val ColorFutureContent = Color(0xFF2A1F00)

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
            onToggle = { viewModel.toggleComplete(item.entity, it) },
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()) }
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
        modifier = Modifier.requireHorizontalSwipe(),
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
                Column(modifier = Modifier) {
                    Text(event.title, style = MaterialTheme.typography.bodyLarge)
                    if (event.isAllDay) {
                        Text("All day", style = MaterialTheme.typography.bodySmall, color = dimColor)
                    } else {
                    Text(
                        "${dateFormatter.format(event.startAt)} – ${dateFormatter.format(event.endAt)}",
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()) }
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
        modifier = Modifier.requireHorizontalSwipe(),
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    dateFormatter.format(task.dueAt ?: return@Row),
                    style = MaterialTheme.typography.bodySmall,
                    color = dimColor,
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
                        text = "Due: ${dateFormatter.format(task.dueAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = dimColor
                    )
                    }
                    val taskNotes = task.notes
                    if (!taskNotes.isNullOrEmpty()) {
                        Text(
                            text = taskNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = dimColor
                        )
                    }
                }
                val expectedTimeMinutes = task.expectedTimeMinutes
                if (expectedTimeMinutes != null) {
                    val h = expectedTimeMinutes / 60
                    val m = expectedTimeMinutes % 60
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


