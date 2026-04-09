package org.meow.autistic.data.note

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for [NoteEntity] data.
 * Delegates all persistence to [NoteDao].
 */
class NoteRepository(private val dao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    suspend fun insert(note: NoteEntity) = dao.insert(note)
    suspend fun update(note: NoteEntity) = dao.update(note)
    suspend fun delete(note: NoteEntity) = dao.delete(note)
}
