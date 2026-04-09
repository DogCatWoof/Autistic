package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.note.NoteRepository

/**
 * ViewModel for the notes screen.
 * Exposes the full list of notes and CRUD operations.
 */
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(note: NoteEntity) = viewModelScope.launch { repository.insert(note) }
    fun update(note: NoteEntity) = viewModelScope.launch { repository.update(note) }
    fun delete(note: NoteEntity) = viewModelScope.launch { repository.delete(note) }
}
