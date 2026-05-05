package org.meow.autistic.data.sync

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.firestore.FirestoreSyncService
import org.meow.autistic.data.task.GoogleTasksSyncService
import java.time.Instant

/** Outcome of a sync run. */
sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data object Retry : SyncOutcome
    data class Error(val message: String) : SyncOutcome
}

private const val TAG = "SyncOrchestrator"
private const val HTTP_FORBIDDEN = 403

/**
 * Orchestrates the six-step sync pipeline, composing the auth, tasks, calendar, and Firestore services.
 *
 * Step 1: Verify authentication — returns [SyncOutcome.Retry] if no valid token is available.
 * Step 2: Push pending local task changes to Google Tasks.
 * Step 3: Pull remote task changes and merge into Room.
 * Step 4: Pull Google Calendar events and merge into Room.
 * Step 5: Push pending local changes to Firestore (non-fatal; skipped if no Firebase user).
 * Step 6: Pull incremental Firestore changes and merge into Room (non-fatal; skipped if no Firebase user).
 *
 * Any exception from steps 2–4 returns [SyncOutcome.Retry]; the pipeline is aborted on the
 * first failure so a partial sync cannot leave data in an inconsistent state.
 * Exceptions from steps 5–6 are caught and logged — they do not affect the outcome of steps 1–4.
 *
 * @param authManager OAuth token management and Firebase UID provider.
 * @param tasksSyncService Bidirectional Google Tasks sync.
 * @param calendarSyncService Read-only Google Calendar sync.
 * @param firestoreSyncService Firestore push/pull sync.
 * @param firebaseUidProvider Returns the Firebase UID if signed in, or null.
 * @param lastFirestorePullAt Provides the timestamp of the last successful Firestore pull.
 * @param recordFirestorePullAt Records the timestamp after a successful Firestore pull.
 */
class SyncOrchestrator(
    private val authManager: GoogleAuthManager,
    private val tasksSyncService: GoogleTasksSyncService,
    private val calendarSyncService: CalendarSyncService,
    private val firestoreSyncService: FirestoreSyncService,
    private val firebaseUidProvider: () -> String?,
    private val lastFirestorePullAt: suspend () -> Instant?,
    private val recordFirestorePullAt: suspend (Instant) -> Unit,
) {

    /**
     * Executes the full sync pipeline.
     * Returns [SyncOutcome.Success] when steps 1–4 complete without error (Firestore failures are non-fatal).
     * Returns [SyncOutcome.Retry] when not authenticated or any of steps 2–4 throws.
     */
    suspend fun sync(): SyncOutcome {
        if (!authManager.isAuthenticated()) {
            return SyncOutcome.Retry
        }
        return try {
            tasksSyncService.pushPending()
            tasksSyncService.pullAndMerge()
            calendarSyncService.pullAndMerge()
            runFirestoreSync()
            SyncOutcome.Success
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Sync HTTP ${e.statusCode}: ${e.message}")
            if (e.statusCode == HTTP_FORBIDDEN) {
                authManager.invalidateTokenCache()
                SyncOutcome.Error("Calendar access denied (403) — please re-authenticate")
            } else {
                SyncOutcome.Retry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            SyncOutcome.Error(e.message ?: "Sync failed")
        }
    }

    private suspend fun runFirestoreSync() {
        val uid = firebaseUidProvider()
        if (uid == null) {
            Log.d(TAG, "Firestore sync skipped: no Firebase user")
            return
        }
        try {
            firestoreSyncService.pushPending(uid)
            val since = lastFirestorePullAt()
            firestoreSyncService.pullAndMerge(uid, since)
            recordFirestorePullAt(Instant.now())
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed (non-fatal): ${e.message}", e)
        }
    }
}
