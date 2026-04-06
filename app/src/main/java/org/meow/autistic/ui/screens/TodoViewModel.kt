package org.meow.autistic.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.sync.IMMEDIATE_WORK_NAME
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.todo.TaskEntity
import org.meow.autistic.data.todo.TaskRepository
import java.time.LocalDate
import java.time.ZoneId

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String?) : SyncState()
    data class LastSynced(val timestamp: Long) : SyncState()
}

/**
 * ViewModel for the task list screen.
 *
 * Combines [TaskRepository] and [CalendarRepository] into a single sorted, sectioned list.
 * Manages Google authentication state and sync scheduling.
 */
class TaskViewModel(
    private val repository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val authManager: GoogleAuthManager,
    private val syncScheduler: SyncScheduler,
    private val workManager: WorkManager,
) : ViewModel() {

    val groupedItems: StateFlow<GroupedTaskItems> = combine(
        repository.allTasks,
        calendarRepository.getAllEvents(),
    ) { tasks, events ->
        val now = System.currentTimeMillis()
        val todayStart = todayStartMs()
        val todayEnd = todayEndMs()

        val taskItems = tasks
            .filter { !it.isCompleted && it.syncStatus != "pending_delete" }
            .map { task -> TaskListItem.Task(task, taskSortKey(task, todayEnd)) }

        // Exclude calendar events that have already ended
        val eventItems = events
            .filter { it.endAt >= now }
            .map { event -> TaskListItem.Event(event, event.endAt) }

        val pastDue = taskItems
            .filter { it.sortKey < now }
            .sortedBy { it.sortKey }

        val upcoming = (taskItems.filter { it.sortKey >= now } + eventItems)
            .sortedBy { it.sortKey }

        GroupedTaskItems(
            pastDue = pastDue,
            today = upcoming.filter { isToday(it, todayStart, todayEnd) },
            later = upcoming.filter { !isToday(it, todayStart, todayEnd) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedTaskItems.EMPTY)

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
        if (isAuthenticated.value) syncScheduler.triggerImmediate()
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

    fun insert(task: TaskEntity) = viewModelScope.launch { repository.insert(task) }

    fun update(task: TaskEntity) = viewModelScope.launch {
        if (task.isCompleted) {
            delete(task)
        } else {
            repository.update(task.copy(syncStatus = "pending_push"))
        }
    }

    fun delete(task: TaskEntity) = viewModelScope.launch {
        if (task.googleTaskId != null) repository.markPendingDelete(task.id)
        else repository.delete(task)
    }

    private fun taskSortKey(entity: TaskEntity, todayEndMs: Long): Long = when {
        entity.expectedTimeMinutes != null && entity.dueAt != null ->
            entity.dueAt + entity.expectedTimeMinutes * 60_000L
        entity.dailyTaskId != null -> todayEndMs
        entity.dueAt != null -> entity.dueAt
        else -> todayEndMs
    }

    private fun isToday(item: TaskListItem, todayStart: Long, todayEnd: Long): Boolean = when (item) {
        is TaskListItem.Task -> item.entity.dueAt?.let { it in todayStart until todayEnd } ?: true
        is TaskListItem.Event -> item.entity.startAt < todayEnd && item.entity.endAt > todayStart
    }

    private fun todayStartMs(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun todayEndMs(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
