package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.note.NoteRepository

/**
 * ViewModel for the notes screen.
 * Exposes active or deleted notes based on the [showDeleted] toggle,
 * and provides CRUD operations including soft delete and restore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _showDeleted = MutableStateFlow(false)
    val showDeleted: StateFlow<Boolean> = _showDeleted.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = _showDeleted
        .flatMapLatest { showDeleted ->
            if (showDeleted) repository.getDeletedNotes() else repository.getActiveNotes()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleShowDeleted() { _showDeleted.value = !_showDeleted.value }
    fun insert(note: NoteEntity) = viewModelScope.launch { repository.insert(note) }
    fun update(note: NoteEntity) = viewModelScope.launch { repository.update(note) }
    fun softDelete(note: NoteEntity) = viewModelScope.launch { repository.softDelete(note.id) }
    fun restore(note: NoteEntity) = viewModelScope.launch { repository.restore(note.id) }
    fun hardDelete(note: NoteEntity) = viewModelScope.launch { repository.hardDelete(note) }
}
