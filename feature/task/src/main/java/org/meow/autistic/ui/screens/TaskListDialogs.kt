package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.task.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
internal fun AddCalendarEventDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Calendar Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    Text("All day")
                }
                Text("Start: $startDate", style = MaterialTheme.typography.labelMedium)
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = startHour, onValueChange = { startHour = it.take(2) },
                            label = { Text("HH") }, singleLine = true,
                            modifier = Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(
                            value = startMinute, onValueChange = { startMinute = it.take(2) },
                            label = { Text("MM") }, singleLine = true,
                            modifier = Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                Text("End: $endDate", style = MaterialTheme.typography.labelMedium)
                if (!isAllDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = endHour, onValueChange = { endHour = it.take(2) },
                            label = { Text("HH") }, singleLine = true,
                            modifier = Modifier.width(64.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(
                            value = endMinute, onValueChange = { endMinute = it.take(2) },
                            label = { Text("MM") }, singleLine = true,
                            modifier = Modifier.width(64.dp),
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
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Instant?, Int?, String?, Int?, Boolean) -> Unit,
) {
    var taskText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expectedTimeText by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderMinutesText by remember { mutableStateOf("15") }
    var isImportant by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = taskText, onValueChange = { taskText = it },
                    label = { Text("Task description") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notesText, onValueChange = { notesText = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
                OutlinedTextField(
                    value = expectedTimeText,
                    onValueChange = { expectedTimeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected time (minutes)") }, modifier = Modifier.fillMaxWidth(),
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
                        label = { Text("Minutes before due") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                    Text("Important")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminder = if (reminderEnabled) reminderMinutesText.toIntOrNull() ?: 15 else null
                    onConfirm(taskText, null, reminder, notesText.takeIf { it.isNotBlank() }, expectedTimeText.toIntOrNull(), isImportant)
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd · hh:mm a").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Column {
                Text(event.title, style = MaterialTheme.typography.titleLarge)
                Text("Meeting Finished", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (event.isAllDay) {
                    Text("All day", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "${dateFormatter.format(event.startAt)} – ${dateFormatter.format(event.endAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!event.location.isNullOrBlank()) {
                    Text("Location: ${event.location}", style = MaterialTheme.typography.bodyMedium)
                }
                event.reminderMinutes?.let { mins ->
                    Text(
                        "Reminder: $mins min before, then every 5 min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val eventDescription = event.description
                if (!eventDescription.isNullOrBlank()) {
                    HorizontalDivider()
                    Text(
                        "Description:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        eventDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("OK") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
    onSave: (String, Int?, String?, Int?, Boolean) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val dueDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd · HH:mm").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()) }
    var taskText by remember { mutableStateOf(task.task) }
    var notesText by remember { mutableStateOf(task.notes ?: "") }
    var expectedTimeText by remember { mutableStateOf(task.expectedTimeMinutes?.toString() ?: "") }
    var reminderEnabled by remember { mutableStateOf(task.reminderMinutesBefore != null) }
    var reminderMinutesText by remember { mutableStateOf(task.reminderMinutesBefore?.toString() ?: "15") }
    var isImportant by remember { mutableStateOf(task.isImportant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.dueAt != null || task.category != "General") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        task.dueAt?.let { due ->
                            Text(
                                "Due: ${dueDateFormatter.format(due)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (task.category != "General") {
                            Text(
                                "Category: ${task.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
                OutlinedTextField(
                    value = taskText, onValueChange = { taskText = it },
                    label = { Text("Task description") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notesText, onValueChange = { notesText = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
                OutlinedTextField(
                    value = expectedTimeText,
                    onValueChange = { expectedTimeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected time (minutes)") }, modifier = Modifier.fillMaxWidth(),
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
                        label = { Text("Minutes before due") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                    Text("Important")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reminder = if (reminderEnabled) reminderMinutesText.toIntOrNull() ?: 15 else null
                    onSave(taskText, reminder, notesText.takeIf { it.isNotBlank() }, expectedTimeText.toIntOrNull(), isImportant)
                },
                enabled = taskText.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onComplete) { Text(if (task.isCompleted) "Reopen" else "Complete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
