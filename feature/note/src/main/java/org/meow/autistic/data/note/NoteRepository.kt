package org.meow.autistic.data.note

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Single source of truth for [NoteEntity] data.
 * Delegates all persistence to [NoteDao].
 */
class NoteRepository(private val dao: NoteDao) {
    fun getActiveNotes(): Flow<List<NoteEntity>> = dao.getActiveNotes()
    fun getDeletedNotes(): Flow<List<NoteEntity>> = dao.getDeletedNotes()
    suspend fun insert(note: NoteEntity) =
        dao.insert(note.copy(updatedAt = Instant.now(), pendingFirestoreSync = true))

    suspend fun update(note: NoteEntity) =
        dao.update(note.copy(updatedAt = Instant.now(), pendingFirestoreSync = true))
    suspend fun softDelete(id: Int) = dao.softDelete(id, Instant.now())
    suspend fun restore(id: Int) = dao.restore(id, Instant.now())
    suspend fun hardDelete(note: NoteEntity) = dao.delete(note)
}
