package org.meow.autistic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.task.DailyTaskEntity

/**
 * Settings sub-screen for managing recurring daily task templates.
 * Tap an item to edit; the FAB opens the add dialog.
 */
@Composable
fun DailyTasksSettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: DailyTasksViewModel = koinViewModel()
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var editingTask by remember { mutableStateOf<DailyTaskEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                ListItem(
                    headlineContent = { Text(task.title) },
                    supportingContent = {
                        val time = task.timeMinutes?.let { formatTime(it) }
                        val duration = task.expectedTimeMinutes?.let {
                            if (it < 60) "~$it min"
                            else { val h = it / 60; val m = it % 60; if (m == 0) "~${h}h" else "~${h}h ${m}m" }
                        }
                        Text(listOfNotNull(time, duration).ifEmpty { listOf("No time") }.joinToString(" · "))
                    },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { viewModel.delete(task) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${task.title}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add daily task")
        }
    }

    if (showAddDialog) {
        DailyTaskDialog(
            initial = null,
            onSave = { task ->
                scope.launch { viewModel.insert(task) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingTask?.let { task ->
        DailyTaskDialog(
            initial = task,
            onSave = { updated ->
                scope.launch { viewModel.update(updated) }
                editingTask = null
            },
            onDismiss = { editingTask = null },
        )
    }
}

/** Formats minutes-since-midnight as "H:MM AM/PM". */
internal fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val amPm = if (h < 12) "AM" else "PM"
    val displayH = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$displayH:${m.toString().padStart(2, '0')} $amPm"
}
