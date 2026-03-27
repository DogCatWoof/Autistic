package org.meow.autistic.data.todo

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    suspend fun insert(todo: TodoEntity) = todoDao.insertTodo(todo)
    suspend fun update(todo: TodoEntity) = todoDao.updateTodo(todo)
    suspend fun delete(todo: TodoEntity) = todoDao.deleteTodo(todo)

    suspend fun getPendingPush(): List<TodoEntity> = todoDao.getPendingPush()
    suspend fun getPendingDelete(): List<TodoEntity> = todoDao.getPendingDelete()
    suspend fun getByGoogleTaskId(googleTaskId: String): TodoEntity? = todoDao.getByGoogleTaskId(googleTaskId)
    suspend fun markPendingPush(id: Long) = todoDao.updateSyncStatus(id, "pending_push")
    suspend fun markPendingDelete(id: Long) = todoDao.updateSyncStatus(id, "pending_delete")
    suspend fun markSynced(id: Long, googleTaskId: String, lastSyncedAt: Long) =
        todoDao.markSynced(id, googleTaskId, lastSyncedAt)
    suspend fun upsertFromRemote(entity: TodoEntity) = todoDao.insertTodo(entity)
    suspend fun deleteByGoogleTaskIds(ids: List<String>) = todoDao.deleteByGoogleTaskIds(ids)
}
