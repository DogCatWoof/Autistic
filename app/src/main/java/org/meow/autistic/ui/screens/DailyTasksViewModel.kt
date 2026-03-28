package org.meow.autistic.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.meow.autistic.data.todo.DailyTaskEntity
import org.meow.autistic.data.todo.DailyTaskRepository

/**
 * ViewModel for the Daily Tasks settings screen.
 * Exposes the live task list and delegates mutations to [DailyTaskRepository].
 */
class DailyTasksViewModel(
    private val repository: DailyTaskRepository,
) : ViewModel() {

    val allTasks = repository.allTasks

    fun insert(task: DailyTaskEntity) {
        viewModelScope.launch { repository.insert(task) }
    }

    fun update(task: DailyTaskEntity) {
        viewModelScope.launch { repository.update(task) }
    }

    fun delete(task: DailyTaskEntity) {
        viewModelScope.launch { repository.delete(task) }
    }
}
