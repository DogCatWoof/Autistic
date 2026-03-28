package org.meow.autistic.data.todo

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger

/**
 * Persistence layer for [TodoEntity]. Delegates to [TodoDao] and records
 * the duration of every suspend call via [QueryLogger].
 */
class TodoRepository(
    private val todoDao: TodoDao,
    private val queryLogger: QueryLogger,
) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    suspend fun insert(todo: TodoEntity) =
        timed("TodoRepository.insert") { todoDao.insertTodo(todo) }

    suspend fun update(todo: TodoEntity) =
        timed("TodoRepository.update") { todoDao.updateTodo(todo) }

    suspend fun delete(todo: TodoEntity) =
        timed("TodoRepository.delete") { todoDao.deleteTodo(todo) }

    suspend fun getPendingPush(): List<TodoEntity> =
        timed("TodoRepository.getPendingPush") { todoDao.getPendingPush() }

    suspend fun getPendingDelete(): List<TodoEntity> =
        timed("TodoRepository.getPendingDelete") { todoDao.getPendingDelete() }

    suspend fun getByGoogleTaskId(googleTaskId: String): TodoEntity? =
        timed("TodoRepository.getByGoogleTaskId") { todoDao.getByGoogleTaskId(googleTaskId) }

    suspend fun markPendingPush(id: Long) =
        timed("TodoRepository.markPendingPush") { todoDao.updateSyncStatus(id, "pending_push") }

    suspend fun markPendingDelete(id: Long) =
        timed("TodoRepository.markPendingDelete") { todoDao.updateSyncStatus(id, "pending_delete") }

    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Long) =
        timed("TodoRepository.markSynced") { todoDao.markSynced(id, googleTaskId, lastSyncedAt) }

    suspend fun upsertFromRemote(entity: TodoEntity) =
        timed("TodoRepository.upsertFromRemote") { todoDao.insertTodo(entity) }

    suspend fun deleteByGoogleTaskIds(ids: List<String>) =
        timed("TodoRepository.deleteByGoogleTaskIds") { todoDao.deleteByGoogleTaskIds(ids) }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
