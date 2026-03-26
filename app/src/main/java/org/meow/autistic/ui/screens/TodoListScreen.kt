package org.meow.autistic.ui.screens

/*
 * TODO: Google Tasks & Calendar Integration Plan
 * 1. [ ] Setup Google Cloud Console project & enable Tasks API and Calendar API.
 * 2. [ ] Add dependencies: Play Services Auth, Google API Client (Tasks & Calendar), and Security Crypto.
 * 3. [ ] Update TodoEntity: Add 'remoteId: String?' and 'lastSynced: Long' for syncing.
 * 4. [ ] Authentication: Implement OAuth2 popup and securely store tokens in EncryptedSharedPreferences.
 *    - Ensure scopes include: https://www.googleapis.com/auth/tasks and https://www.googleapis.com/auth/calendar.events.
 * 5. [ ] Sync Logic: Create a repository service to reconcile local Room DB with Google Tasks API.
 *    - Note: Google Tasks API has no custom fields. Use the 'notes' field to store/retrieve 
 *      a JSON string containing local-only properties (category, reminderSet, etc.).
 *    - The 'notes' content should not be displayed directly to the user.
 * 6. [ ] Calendar Integration: Implement logic to sync tasks with due dates to Google Calendar as events.
 * 7. [ ] Background Sync: Implement WorkManager with constraints:
 *    - PeriodicWork (15 min) when on WiFi.
 *    - PeriodicWork (1 hour) when on Cellular.
 * 8. [ ] UI: Add a manual sync button and a "Connect Google Account" setup flow.
 */

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.meow.autistic.data.todo.TodoEntity
import java.text.SimpleDateFormat
import java.util.*

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
                        color = if (todo.isCompleted) Color.Gray else Color.Unspecified
                    )
                    if (todo.dueAt != null) {
                        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Text(
                            text = "Due: ${sdf.format(Date(todo.dueAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    if (todo.category.isNotEmpty()) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(todo.category) }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
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
    var selectedDueAt by remember { mutableStateOf<Long?>(null) }

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
                        tint = if (reminderSet) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                val categories = listOf("General", "Work", "Personal", "Health")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { cat ->
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
                onClick = { onConfirm(taskText, selectedDueAt, category, reminderSet) },
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
