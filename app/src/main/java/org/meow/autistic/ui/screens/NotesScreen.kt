package org.meow.autistic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.note.NoteEntity
import java.time.Instant

/**
 * Notes screen — shows a list of notes, supports create, edit, delete, and swipe-to-delete.
 * Navigates between list and editor using local state (no back-stack).
 */
@Composable
fun NotesScreen(modifier: Modifier = Modifier, viewModel: NotesViewModel = koinViewModel()) {
    val notes by viewModel.notes.collectAsState()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isNewNote by remember { mutableStateOf(false) }

    BackHandler(enabled = editingNote != null) { editingNote = null }

    if (editingNote != null) {
        NoteEditorScreen(
            note = editingNote!!,
            isNew = isNewNote,
            onSave = { content ->
                val now = Instant.now()
                if (isNewNote) {
                    viewModel.insert(editingNote!!.copy(content = content, createdAt = now, updatedAt = now))
                } else {
                    viewModel.update(editingNote!!.copy(content = content, updatedAt = now))
                }
                editingNote = null
            },
            onDelete = {
                if (!isNewNote) viewModel.delete(editingNote!!)
                editingNote = null
            },
            onBack = { editingNote = null },
            modifier = modifier,
        )
    } else {
        NoteListScreen(
            notes = notes,
            onNoteClick = { note ->
                isNewNote = false
                editingNote = note
            },
            onDelete = viewModel::delete,
            onCreateClick = {
                isNewNote = true
                editingNote = NoteEntity(content = "", createdAt = Instant.now(), updatedAt = Instant.now())
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListScreen(
    notes: List<NoteEntity>,
    onNoteClick: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Notes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No notes yet. Tap + to create one.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(notes, key = { it.id }) { note ->
                    NoteListItem(note = note, onClick = { onNoteClick(note) }, onDelete = { onDelete(note) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListItem(note: NoteEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .clickable { onClick() }
        ) {
            Text(
                text = note.content.lines().firstOrNull { it.isNotBlank() } ?: "(empty)",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note: NoteEntity,
    isNew: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var content by rememberSaveable { mutableStateOf(note.content) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Note" else "Edit Note") },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { onSave(content) }, enabled = content.isNotBlank()) {
                        Text("Save")
                    }
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            placeholder = { Text("Write your note…") },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}
