package org.meow.autistic.data.sync

import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.todo.GoogleTasksSyncService

/** Outcome of a single sync run. */
sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data object Retry : SyncOutcome
}

/**
 * Orchestrates the four-step sync pipeline, composing the auth, tasks, and calendar services.
 *
 * Step 1: Verify authentication — returns [SyncOutcome.Retry] if no valid token is available.
 * Step 2: Push pending local task changes to Google Tasks.
 * Step 3: Pull remote task changes and merge into Room.
 * Step 4: Pull Google Calendar events and merge into Room.
 *
 * Any exception from steps 2–4 returns [SyncOutcome.Retry]; the pipeline is aborted on the
 * first failure so a partial sync cannot leave data in an inconsistent state.
 *
 * @param authManager OAuth token management.
 * @param tasksSyncService Bidirectional Google Tasks sync.
 * @param calendarSyncService Read-only Google Calendar sync.
 */
class SyncOrchestrator(
    private val authManager: GoogleAuthManager,
    private val tasksSyncService: GoogleTasksSyncService,
    private val calendarSyncService: CalendarSyncService,
) {

    /**
     * Executes the full sync pipeline.
     * Returns [SyncOutcome.Success] when all steps complete without error.
     * Returns [SyncOutcome.Retry] when not authenticated or any step throws.
     */
    suspend fun sync(): SyncOutcome {
        if (!authManager.isAuthenticated()) {
            return SyncOutcome.Retry
        }
        return try {
            tasksSyncService.pushPending()
            tasksSyncService.pullAndMerge()
            calendarSyncService.pullAndMerge()
            SyncOutcome.Success
        } catch (e: Exception) {
            SyncOutcome.Retry
        }
    }
}
