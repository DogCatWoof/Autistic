package org.meow.autistic.data.task

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger
import java.time.Instant

/**
 * Persistence layer for [TaskEntity]. Delegates to [TaskDao] and records
 * the duration of every suspend call via [QueryLogger].
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val queryLogger: QueryLogger,
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()

    suspend fun insert(task: TaskEntity) =
        timed("TaskRepository.insert") {
            taskDao.insertTask(task.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun update(task: TaskEntity) =
        timed("TaskRepository.update") {
            taskDao.updateTask(task.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun delete(task: TaskEntity) =
        timed("TaskRepository.delete") { taskDao.deleteTask(task) }

    suspend fun getPendingPush(): List<TaskEntity> =
        timed("TaskRepository.getPendingPush") { taskDao.getPendingPush() }

    suspend fun getPendingDelete(): List<TaskEntity> =
        timed("TaskRepository.getPendingDelete") { taskDao.getPendingDelete() }

    suspend fun getByGoogleTaskId(googleTaskId: String): TaskEntity? =
        timed("TaskRepository.getByGoogleTaskId") { taskDao.getByGoogleTaskId(googleTaskId) }

    suspend fun markPendingPush(id: Long) =
        timed("TaskRepository.markPendingPush") { taskDao.updateSyncStatus(id, "pending_push") }

    suspend fun markPendingDelete(id: Long) =
        timed("TaskRepository.markPendingDelete") { taskDao.markPendingDeleteSync(id, Instant.now()) }

    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Instant) =
        timed("TaskRepository.markSynced") { taskDao.markSynced(id, googleTaskId, lastSyncedAt) }

    suspend fun upsertFromRemote(entity: TaskEntity) =
        timed("TaskRepository.upsertFromRemote") { taskDao.insertTask(entity) }

    suspend fun deleteByGoogleTaskIds(ids: List<String>) =
        timed("TaskRepository.deleteByGoogleTaskIds") { taskDao.deleteByGoogleTaskIds(ids) }

    suspend fun deleteStaleCompleted(cutoff: Instant) =
        timed("TaskRepository.deleteStaleCompleted") { taskDao.deleteStaleCompleted(cutoff) }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
