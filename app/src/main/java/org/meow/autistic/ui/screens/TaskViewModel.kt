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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.sync.IMMEDIATE_WORK_NAME
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.task.TaskEntity
import org.meow.autistic.data.task.TaskReminderWorker
import org.meow.autistic.data.task.TaskRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class UndoableAction {
    data class TaskDeleted(val task: TaskEntity) : UndoableAction()
    data class TaskToggled(val previousTask: TaskEntity) : UndoableAction()
    data class EventHidden(val event: CalendarEventEntity) : UndoableAction()
    data class EventDeleted(val event: CalendarEventEntity) : UndoableAction()
}

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

    private val _showCompleted = MutableStateFlow(false)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    private val _undoStack = MutableStateFlow<List<UndoableAction>>(emptyList())
    val undoStack: StateFlow<List<UndoableAction>> = _undoStack.asStateFlow()

    private fun pushUndo(action: UndoableAction) {
        _undoStack.update { current -> (listOf(action) + current).take(3) }
    }

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
            .filter { it.endAt.toEpochMilli() >= now }
            .map { event -> TaskListItem.Event(event, event.endAt.toEpochMilli()) }

        val pastDue = taskItems
            .filter { it.sortKey < now }
            .sortedBy { it.sortKey }

        val upcoming = (taskItems.filter { it.sortKey >= now } + eventItems)
            .sortedBy { it.sortKey }

        val todayDate = LocalDate.now()
        val laterByDate = upcoming
            .filter { !isToday(it, todayStart, todayEnd) }
            .groupBy { dateLabelWithDate(it, todayDate) }
            .entries
            .sortedBy { (_, items) -> items.minOf { it.sortKey } }
            .map { (pair, items) -> Triple(pair.first, pair.second, items) }

        GroupedTaskItems(
            pastDue = pastDue,
            today = upcoming.filter { isToday(it, todayStart, todayEnd) },
            later = laterByDate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedTaskItems.EMPTY)

    val completedItems: StateFlow<List<TaskListItem.Task>> = repository.completedTasks
        .map { tasks ->
            val todayEnd = todayEndMs()
            tasks.map { task -> TaskListItem.Task(task, taskSortKey(task, todayEnd)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            _isAuthenticated.value = true
            triggerSync()
        }
    }

    fun insert(task: TaskEntity) = viewModelScope.launch {
        val id = repository.insert(task)
        TaskReminderWorker.scheduleFor(workManager, task.copy(id = id))
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    fun update(task: TaskEntity) = viewModelScope.launch {
        if (task.isCompleted && task.googleTaskId == null) {
            repository.update(task)
            TaskReminderWorker.cancel(workManager, task.id)
        } else {
            repository.update(task.copy(syncStatus = "pending_push"))
            syncReminder(task)
        }
    }

    private fun syncReminder(task: TaskEntity) {
        if (task.reminderMinutesBefore != null) {
            TaskReminderWorker.scheduleFor(workManager, task)
        } else {
            TaskReminderWorker.cancel(workManager, task.id)
        }
    }

    fun toggleComplete(task: TaskEntity, isCompleted: Boolean) {
        pushUndo(UndoableAction.TaskToggled(task))
        val completedAt = if (isCompleted) Instant.now() else null
        update(task.copy(completedAt = completedAt))
    }

    fun delete(task: TaskEntity) = viewModelScope.launch {
        pushUndo(UndoableAction.TaskDeleted(task))
        TaskReminderWorker.cancel(workManager, task.id)
        if (task.googleTaskId != null) repository.markPendingDelete(task.id)
        else repository.delete(task)
    }

    fun undo() = viewModelScope.launch {
        val action = _undoStack.value.firstOrNull() ?: return@launch
        _undoStack.update { it.drop(1) }
        when (action) {
            is UndoableAction.TaskDeleted -> {
                val task = action.task
                if (task.googleTaskId != null) {
                    repository.update(task)
                } else {
                    repository.insert(task)
                    syncReminder(task)
                }
            }
            is UndoableAction.TaskToggled -> {
                val prev = action.previousTask
                val syncStatus = if (prev.googleTaskId != null) "pending_push" else prev.syncStatus
                repository.update(prev.copy(syncStatus = syncStatus))
                if (!prev.isCompleted) syncReminder(prev) else TaskReminderWorker.cancel(workManager, prev.id)
            }
            is UndoableAction.EventHidden -> calendarRepository.markVisible(action.event.googleEventId)
            is UndoableAction.EventDeleted -> calendarRepository.upsertEvents(listOf(action.event))
        }
    }

    fun insertCalendarEvent(event: org.meow.autistic.data.calendar.CalendarEventEntity) = viewModelScope.launch {
        calendarRepository.upsertEvents(listOf(event))
    }

    fun completeEvent(event: CalendarEventEntity) = viewModelScope.launch {
        pushUndo(UndoableAction.EventHidden(event))
        calendarRepository.markHidden(event.googleEventId)
    }

    fun deleteEvent(event: CalendarEventEntity) = viewModelScope.launch {
        pushUndo(UndoableAction.EventDeleted(event))
        calendarRepository.markPendingDelete(event.googleEventId)
    }

    private fun taskSortKey(entity: TaskEntity, todayEndMs: Long): Long = when {
        entity.expectedTimeMinutes != null && entity.dueAt != null ->
            entity.dueAt.toEpochMilli() + entity.expectedTimeMinutes * 60_000L
        entity.dailyTaskId != null -> todayEndMs
        entity.dueAt != null -> entity.dueAt.toEpochMilli()
        else -> todayEndMs
    }

    private fun isToday(item: TaskListItem, todayStart: Long, todayEnd: Long): Boolean = when (item) {
        is TaskListItem.Task -> item.entity.dueAt?.toEpochMilli()?.let { it in todayStart until todayEnd } ?: true
        is TaskListItem.Event -> item.entity.startAt.toEpochMilli() < todayEnd && item.entity.endAt.toEpochMilli() > todayStart
    }

    private fun dateLabelWithDate(item: TaskListItem, todayDate: LocalDate): Pair<String, LocalDate?> {
        val instant = when (item) {
            is TaskListItem.Task -> item.entity.dueAt
            is TaskListItem.Event -> item.entity.startAt
        } ?: return "Later" to null
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val thisWeekEnd = todayDate.with(DayOfWeek.SUNDAY)
        return when {
            date == todayDate.plusDays(1) -> "Tomorrow" to date
            !date.isAfter(thisWeekEnd) -> date.format(DateTimeFormatter.ofPattern("EEEE")) to date
            else -> {
                val weekStart = date.with(DayOfWeek.MONDAY)
                val weekEnd = weekStart.plusDays(6)
                val label = if (weekStart.month == weekEnd.month) {
                    "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekEnd.format(DateTimeFormatter.ofPattern("d"))}"
                } else {
                    "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d"))}"
                }
                label to weekStart
            }
        }
    }

    private fun todayStartMs(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun todayEndMs(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
