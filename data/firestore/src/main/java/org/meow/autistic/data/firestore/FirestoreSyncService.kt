package org.meow.autistic.data.firestore

import org.meow.autistic.data.foodlog.FoodLogItemDao
import org.meow.autistic.data.mood.MoodDao
import org.meow.autistic.data.note.NoteDao
import org.meow.autistic.data.sequence.SequenceDao
import org.meow.autistic.data.task.DailyTaskDao
import org.meow.autistic.data.task.TaskDao
import java.time.Instant
import java.util.UUID

/** Bundles the DAOs required by [FirestoreSyncService]. */
data class FirestoreDaos(
    val task: TaskDao,
    val note: NoteDao,
    val mood: MoodDao,
    val foodLogItem: FoodLogItemDao,
    val sequence: SequenceDao,
    val dailyTask: DailyTaskDao,
)

/**
 * Handles bidirectional Room ↔ Firestore sync for all user data collections.
 * Room is the source of truth; Firestore propagates changes across devices.
 *
 * Push: reads records with `pendingFirestoreSync = true`, upserts/deletes in Firestore,
 * then clears the flag locally on success.
 * Pull: fetches Firestore documents modified since [since], merges into Room using
 * last-write-wins (local wins ties).
 */
class FirestoreSyncService(
    private val source: FirestoreSource,
    private val daos: FirestoreDaos,
) {

    suspend fun pushPending(uid: String) {
        pushTasks(uid)
        pushNotes(uid)
        pushMoods(uid)
        pushFoodLogItems(uid)
        pushSequences(uid)
        pushSequenceSteps(uid)
        pushSequenceRuns(uid)
        pushDailyTasks(uid)
    }

    /** Returns true if any local records are pending Firestore sync or delete. */
    suspend fun hasPendingItems(): Boolean =
        daos.task.getPendingFirestoreSync().isNotEmpty() ||
            daos.task.getPendingFirestoreDelete().isNotEmpty() ||
            daos.note.getPendingFirestoreSync().isNotEmpty() ||
            daos.note.getPendingFirestoreDelete().isNotEmpty() ||
            daos.mood.getPendingFirestoreSync().isNotEmpty() ||
            daos.mood.getPendingFirestoreDelete().isNotEmpty() ||
            daos.foodLogItem.getPendingFirestoreSync().isNotEmpty() ||
            daos.foodLogItem.getPendingFirestoreDelete().isNotEmpty() ||
            daos.sequence.getPendingFirestoreSync().isNotEmpty() ||
            daos.sequence.getPendingFirestoreDelete().isNotEmpty() ||
            daos.sequence.getPendingFirestoreStepSync().isNotEmpty() ||
            daos.sequence.getPendingFirestoreStepDelete().isNotEmpty() ||
            daos.sequence.getPendingFirestoreRunSync().isNotEmpty() ||
            daos.dailyTask.getPendingFirestoreSync().isNotEmpty()

    suspend fun pullAndMerge(uid: String, since: Instant?) {
        pullTasks(uid, since)
        pullNotes(uid, since)
        pullMoods(uid, since)
        pullFoodLogItems(uid, since)
        pullSequences(uid, since)
        pullSequenceSteps(uid, since)
        pullSequenceRuns(uid, since)
        pullDailyTasks(uid, since)
    }

    // ── Push ─────────────────────────────────────────────────────────────────

    private suspend fun pushTasks(uid: String) {
        for (t in daos.task.getPendingFirestoreSync()) {
            val id = t.firestoreId ?: newId()
            source.upsert(uid, "tasks", id, t.toDocument().toMap())
            daos.task.markFirestoreSynced(t.id, id)
        }
        for (t in daos.task.getPendingFirestoreDelete()) {
            t.firestoreId?.let { source.delete(uid, "tasks", it) }
            daos.task.deleteTask(t)
        }
    }

    private suspend fun pushNotes(uid: String) {
        for (n in daos.note.getPendingFirestoreSync() + daos.note.getPendingFirestoreDelete()) {
            val id = n.firestoreId ?: newId()
            source.upsert(uid, "notes", id, n.toDocument().toMap())
            daos.note.markFirestoreSynced(n.id, id)
        }
    }

    private suspend fun pushMoods(uid: String) {
        for (m in daos.mood.getPendingFirestoreSync() + daos.mood.getPendingFirestoreDelete()) {
            val id = m.firestoreId ?: newId()
            source.upsert(uid, "moods", id, m.toDocument().toMap())
            daos.mood.markFirestoreSynced(m.id, id)
        }
    }

    private suspend fun pushFoodLogItems(uid: String) {
        for (f in daos.foodLogItem.getPendingFirestoreSync() + daos.foodLogItem.getPendingFirestoreDelete()) {
            val id = f.firestoreId ?: newId()
            source.upsert(uid, "foodLogItems", id, f.toDocument().toMap())
            daos.foodLogItem.markFirestoreSynced(f.id, id)
        }
    }

    private suspend fun pushSequences(uid: String) {
        for (s in daos.sequence.getPendingFirestoreSync() + daos.sequence.getPendingFirestoreDelete()) {
            val id = s.firestoreId ?: newId()
            source.upsert(uid, "sequences", id, s.toDocument().toMap())
            daos.sequence.markSequenceFirestoreSynced(s.id, id)
        }
    }

    private suspend fun pushSequenceSteps(uid: String) {
        for (step in daos.sequence.getPendingFirestoreStepSync() + daos.sequence.getPendingFirestoreStepDelete()) {
            val seqFsId = daos.sequence.getById(step.sequenceId)?.firestoreId ?: continue
            val id = step.firestoreId ?: newId()
            source.upsert(uid, "sequenceSteps", id, step.toDocument(seqFsId).toMap())
            daos.sequence.markStepFirestoreSynced(step.id, id)
        }
    }

    private suspend fun pushSequenceRuns(uid: String) {
        for (run in daos.sequence.getPendingFirestoreRunSync()) {
            val seqFsId = daos.sequence.getById(run.sequenceId)?.firestoreId ?: continue
            val id = run.firestoreId ?: newId()
            source.upsert(uid, "sequenceRuns", id, run.toDocument(seqFsId).toMap())
            daos.sequence.markRunFirestoreSynced(run.id, id)
        }
    }

    private suspend fun pushDailyTasks(uid: String) {
        for (t in daos.dailyTask.getPendingFirestoreSync()) {
            val id = t.firestoreId ?: newId()
            source.upsert(uid, "dailyTasks", id, t.toDocument().toMap())
            daos.dailyTask.markFirestoreSynced(t.id, id)
        }
    }

    // ── Pull ─────────────────────────────────────────────────────────────────

    private suspend fun pullTasks(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "tasks", since)) {
            val remote = TaskDocument.fromSnapshot(doc)
            val local = daos.task.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                val entity = remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0L)
                    .let { if (it.googleTaskId != null) it.copy(syncStatus = "synced") else it }
                daos.task.insertTask(entity)
            }
        }
    }

    private suspend fun pullNotes(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "notes", since)) {
            val remote = NoteDocument.fromSnapshot(doc)
            val local = daos.note.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.updatedAt) {
                daos.note.insert(remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0))
            }
        }
    }

    private suspend fun pullMoods(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "moods", since)) {
            val remote = MoodDocument.fromSnapshot(doc)
            val local = daos.mood.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.mood.upsert(remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0))
            }
        }
    }

    private suspend fun pullFoodLogItems(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "foodLogItems", since)) {
            val remote = FoodLogItemDocument.fromSnapshot(doc)
            val local = daos.foodLogItem.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.foodLogItem.upsert(remote.toEntry(firestoreId = doc.id, localId = local?.id ?: 0L))
            }
        }
    }

    private suspend fun pullSequences(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequences", since)) {
            val remote = SequenceDocument.fromSnapshot(doc)
            val local = daos.sequence.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.sequence.upsertSequence(remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0L))
            }
        }
    }

    private suspend fun pullSequenceSteps(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequenceSteps", since)) {
            val remote = SequenceStepDocument.fromSnapshot(doc)
            val parent = daos.sequence.getByFirestoreId(remote.sequenceFirestoreId) ?: continue
            val local = daos.sequence.getStepByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.sequence.upsertStep(
                    remote.toEntity(firestoreId = doc.id, sequenceLocalId = parent.id, localId = local?.id ?: 0L)
                )
            }
        }
    }

    private suspend fun pullSequenceRuns(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequenceRuns", since)) {
            val remote = SequenceRunDocument.fromSnapshot(doc)
            val parent = daos.sequence.getByFirestoreId(remote.sequenceFirestoreId) ?: continue
            val local = daos.sequence.getRunByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.sequence.upsertRun(
                    remote.toEntity(firestoreId = doc.id, sequenceLocalId = parent.id, localId = local?.id ?: 0L)
                )
            }
        }
    }

    private suspend fun pullDailyTasks(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "dailyTasks", since)) {
            val remote = DailyTaskDocument.fromSnapshot(doc)
            val local = daos.dailyTask.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                daos.dailyTask.upsert(remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0L))
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun newId() = UUID.randomUUID().toString()

    private suspend fun fetchDocs(uid: String, collection: String, since: Instant?) =
        (if (since != null) source.fetchSince(uid, collection, since) else source.fetchAll(uid, collection)).documents
}
