package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.meow.autistic.data.task.DailyTaskEntity

/**
 * Add / edit dialog for a [DailyTaskEntity].
 * Pass [initial] = null to create a new task, or an existing entity to edit it.
 */
@Composable
fun DailyTaskDialog(
    initial: DailyTaskEntity?,
    onSave: (DailyTaskEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var hasTime by remember { mutableStateOf(initial?.timeMinutes != null) }
    var hour by remember { mutableIntStateOf(initial?.timeMinutes?.div(60) ?: 8) }
    var minute by remember { mutableIntStateOf(initial?.timeMinutes?.rem(60) ?: 0) }
    var expectedTimeText by remember { mutableStateOf(initial?.expectedTimeMinutes?.toString() ?: "") }

    val timeMinutes = if (hasTime) hour * 60 + minute else null
    val canSave = title.isNotBlank()
        && hour in 0..23
        && minute in 0..59

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Daily Task" else "Edit Daily Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                    Text("Set time")
                }
                if (hasTime) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = hour.toString(),
                            onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                            label = { Text("HH") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                        Text(":")
                        OutlinedTextField(
                            value = minute.toString().padStart(2, '0'),
                            onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                            label = { Text("MM") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = expectedTimeText,
                    onValueChange = { expectedTimeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected time (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        (initial ?: DailyTaskEntity(title = title)).copy(
                            title = title,
                            timeMinutes = timeMinutes,
                            expectedTimeMinutes = expectedTimeText.toIntOrNull(),
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
