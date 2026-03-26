package org.meow.autistic.data.todo

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()

    suspend fun insert(todo: TodoEntity) = todoDao.insertTodo(todo)
    suspend fun update(todo: TodoEntity) = todoDao.updateTodo(todo)
    suspend fun delete(todo: TodoEntity) = todoDao.deleteTodo(todo)
}
