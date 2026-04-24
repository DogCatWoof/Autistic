package org.meow.autistic.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import org.meow.autistic.data.task.TaskEntity

fun resolveItemIcon(item: TaskListItem): ImageVector = when (item) {
    is TaskListItem.Task -> resolveTaskIcon(item.entity)
    is TaskListItem.Event -> Icons.Default.Event
}

private fun resolveTaskIcon(task: TaskEntity): ImageVector {
    if (task.isImportant) return Icons.Default.Star
    if (task.isRequired) return Icons.Default.Flag
    if (task.dailyTaskId != null) return Icons.Default.Repeat
    return Icons.Default.CheckBoxOutlineBlank
}
