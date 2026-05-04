package org.meow.autistic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.RestoreFromTrash
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.note.displayTitle
import java.time.Instant

/**
 * Notes screen — shows a list of active (or deleted) notes, supports create, edit,
 * soft delete, restore, hard delete, and continuous voice-to-text input.
 * Navigates between list and editor using local state (no back-stack).
 */
@Composable
fun NotesScreen(modifier: Modifier = Modifier, viewModel: NotesViewModel = koinViewModel()) {
    val notes by viewModel.notes.collectAsState()
    val showDeleted by viewModel.showDeleted.collectAsState()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isNewNote by remember { mutableStateOf(false) }

    BackHandler(enabled = editingNote != null) { editingNote = null }

    if (editingNote != null) {
        NoteEditorScreen(
            note = editingNote!!,
            isNew = isNewNote,
            onSave = { title, content ->
                val now = Instant.now()
                if (isNewNote) {
                    viewModel.insert(
                        editingNote!!.copy(title = title, content = content, createdAt = now, updatedAt = now)
                    )
                } else {
                    viewModel.update(editingNote!!.copy(title = title, content = content, updatedAt = now))
                }
                editingNote = null
            },
            onDelete = {
                if (!isNewNote) viewModel.softDelete(editingNote!!)
                editingNote = null
            },
            onBack = { editingNote = null },
            modifier = modifier,
        )
    } else {
        NoteListScreen(
            notes = notes,
            showDeleted = showDeleted,
            onToggleFilter = viewModel::toggleShowDeleted,
            onNoteClick = { note ->
                isNewNote = false
                editingNote = note
            },
            onSoftDelete = viewModel::softDelete,
            onRestore = viewModel::restore,
            onHardDelete = viewModel::hardDelete,
            onCreateClick = {
                isNewNote = true
                editingNote = NoteEntity(
                    content = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListScreen(
    notes: List<NoteEntity>,
    showDeleted: Boolean,
    onToggleFilter: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onSoftDelete: (NoteEntity) -> Unit,
    onRestore: (NoteEntity) -> Unit,
    onHardDelete: (NoteEntity) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (showDeleted) "Deleted Notes" else "Notes") },
                actions = {
                    IconButton(onClick = onToggleFilter) {
                        Icon(
                            if (showDeleted) Icons.Outlined.FilterAltOff else Icons.Outlined.FilterAlt,
                            contentDescription = if (showDeleted) "Show active notes" else "Show deleted notes",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!showDeleted) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (showDeleted) "No deleted notes." else "No notes yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(notes, key = { it.id }) { note ->
                    if (showDeleted) {
                        DeletedNoteListItem(
                            note = note,
                            onRestore = { onRestore(note) },
                            onHardDelete = { onHardDelete(note) },
                        )
                    } else {
                        ActiveNoteListItem(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onSoftDelete = { onSoftDelete(note) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveNoteListItem(note: NoteEntity, onClick: () -> Unit, onSoftDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onSoftDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.requireHorizontalSwipe(),
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteSwipeBackground() },
    ) {
        NoteListItemContent(note = note, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletedNoteListItem(note: NoteEntity, onRestore: () -> Unit, onHardDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onHardDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.requireHorizontalSwipe(),
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteSwipeBackground(contentDescription = "Delete permanently") },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            ) {
                Text(
                    text = note.displayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatInstantForDisplay(note.updatedAt, historicalPattern = "MMM d, yyyy"),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    ),
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Outlined.RestoreFromTrash,
                    contentDescription = "Restore note",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun NoteListItemContent(note: NoteEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
    ) {
        val hasTitle = note.title.isNotBlank()
        val contentPreview = if (hasTitle) note.content.lines().firstOrNull { it.isNotBlank() } else null

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = note.displayTitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contentPreview != null) {
                Text(
                    text = contentPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatInstantForDisplay(note.updatedAt, historicalPattern = "MMM d, yyyy"),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                ),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}


// ---------------------------------------------------------------------------
// Note editor
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note: NoteEntity,
    isNew: Boolean,
    onSave: (title: String, content: String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note.title))
    }
    var content by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note.content))
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Note" else "Edit Note") },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(
                        onClick = { onSave(title.text, content.text) },
                        enabled = content.text.isNotBlank() || title.text.isNotBlank(),
                    ) {
                        Text("Save")
                    }
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
            )

            if (!isNew) {
                Text(
                    text = "Created ${formatInstantForDisplay(note.createdAt, "MMM d, yyyy")}  •  Updated ${formatInstantForDisplay(note.updatedAt, "MMM d, yyyy")}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                placeholder = { Text("Write your note…") },
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}
