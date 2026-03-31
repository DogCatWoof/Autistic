package org.meow.autistic.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.sync.IMMEDIATE_WORK_NAME
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.todo.TodoEntity
import org.meow.autistic.data.todo.TodoRepository

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String?) : SyncState()
    data class LastSynced(val timestamp: Long) : SyncState()
}

/**
 * ViewModel for the todo list screen.
 *
 * Manages [TodoRepository] operations, Google authentication state, and sync scheduling.
 */
class TodoViewModel(
    private val repository: TodoRepository,
    private val authManager: GoogleAuthManager,
    private val syncScheduler: SyncScheduler,
    private val workManager: WorkManager,
) : ViewModel() {

    val allTodos: Flow<List<TodoEntity>> =
        repository.allTodos.map { todos -> todos.filter { !it.isCompleted } }

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val syncState: StateFlow<SyncState>

    init {
        _isAuthenticated.value = authManager.isAuthenticated()

        syncState = workManager.getWorkInfosForUniqueWorkFlow(IMMEDIATE_WORK_NAME)
            .map { workInfos ->
                val info = workInfos.firstOrNull()
                when (info?.state) {
                    WorkInfo.State.RUNNING -> SyncState.Syncing
                    WorkInfo.State.FAILED -> SyncState.Error(info.outputData.getString("error"))
                    WorkInfo.State.SUCCEEDED -> SyncState.LastSynced(System.currentTimeMillis())
                    else -> SyncState.Idle
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)
    }

    fun triggerSync() {
        if (isAuthenticated.value) {
            syncScheduler.triggerImmediate()
        }
    }

    fun updateAuthStatus() {
        _isAuthenticated.value = authManager.isAuthenticated()
    }

    fun signOut() = viewModelScope.launch {
        authManager.signOut()
        updateAuthStatus()
    }

    fun getSignInIntent() = authManager.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        if (authManager.handleSignInResult(data)) {
            updateAuthStatus()
            triggerSync()
        }
    }

    fun insert(todo: TodoEntity) = viewModelScope.launch {
        repository.insert(todo)
    }

    fun update(todo: TodoEntity) = viewModelScope.launch {
        repository.update(todo.copy(syncStatus = "pending_push"))
    }

    fun delete(todo: TodoEntity) = viewModelScope.launch {
        repository.delete(todo)
    }
}
