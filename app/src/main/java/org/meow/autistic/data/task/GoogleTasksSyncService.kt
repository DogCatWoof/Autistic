package org.meow.autistic.data.task

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DEFAULT_TASK_LIST = "@default"
private const val RFC3339_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
private const val PROPS_SEPARATOR = "---autistic-props---"

/**
 * Orchestrates bidirectional sync between the local Room database and the Google Tasks API.
 *
 * Conflict rule: remote wins on pull. Local edits are queued as "pending_push" and
 * flushed to the remote before each pull.
 *
 * @param remoteSource Raw HTTP calls to the Google Tasks API.
 * @param repository Local persistence for task entities.
 * @param tokenProvider Suspending supplier of a valid OAuth access token.
 */
class GoogleTasksSyncService(
    private val remoteSource: GoogleTasksRemoteSource,
    private val repository: TaskRepository,
    private val tokenProvider: suspend () -> String,
) {

    /**
     * Pushes all locally queued changes to Google Tasks.
     * "pending_push" items are created or updated; "pending_delete" items are deleted.
     */
    suspend fun pushPending() {
        val token = tokenProvider()
        pushCreatesAndUpdates(token)
        pushDeletes(token)
    }

    /**
     * Fetches incomplete remote tasks and merges them into the local database.
     * Remote-deleted tasks are removed locally; active tasks are upserted with remote data winning.
     * Completed tasks are cleaned from the local database after merging.
     */
    suspend fun pullAndMerge() {
        val token = tokenProvider()
        val remoteTasks = remoteSource.fetchTasks(token)
        applyRemoteDeletions(remoteTasks.filter { it.deleted })
        mergeActiveTasks(remoteTasks.filter { !it.deleted })
        repository.deleteStaleCompleted(Instant.now().minus(7, ChronoUnit.DAYS))
    }

    private suspend fun pushCreatesAndUpdates(token: String) {
        repository.getPendingPush().forEach { local ->
            val remote = if (local.googleTaskId == null) {
                remoteSource.createTask(token, local.toRemoteTask())
            } else {
                remoteSource.updateTask(token, local.toRemoteTask())
            }
            val googleTaskId = requireNotNull(remote.id)
            repository.markSynced(local.id, googleTaskId, Instant.now())
        }
    }

    private suspend fun pushDeletes(token: String) {
        repository.getPendingDelete().forEach { local ->
            if (local.googleTaskId != null) {
                remoteSource.deleteTask(token, local.googleTaskId)
            }
            repository.delete(local)
        }
    }

    private suspend fun applyRemoteDeletions(deleted: List<RemoteTask>) {
        val ids = deleted.mapNotNull { it.id }
        if (ids.isNotEmpty()) {
            repository.deleteByGoogleTaskIds(ids)
        }
    }

    private suspend fun mergeActiveTasks(active: List<RemoteTask>) {
        val now = Instant.now()
        active.forEach { remote ->
            val existing = remote.id?.let { repository.getByGoogleTaskId(it) }
            val entity = if (existing != null) {
                mergeIntoExisting(existing, remote, now)
            } else {
                remoteToNewEntity(remote, now)
            }
            repository.upsertFromRemote(entity)
        }
    }

    private fun mergeIntoExisting(existing: TaskEntity, remote: RemoteTask, now: Instant): TaskEntity {
        val (parsedNotes, parsedProps) = parseNotesField(remote.notes)
        return existing.copy(
            task = remote.title,
            completedAt = if (remote.status == "completed") existing.completedAt ?: now else null,
            dueAt = remote.due?.fromRfc3339(),
            notes = parsedNotes,
            extraPropertiesJson = parsedProps,
            googleTaskId = remote.id,
            googleTaskListId = DEFAULT_TASK_LIST,
            lastSyncedAt = now,
            syncStatus = "synced",
        )
    }

    private fun remoteToNewEntity(remote: RemoteTask, now: Instant): TaskEntity {
        val (parsedNotes, parsedProps) = parseNotesField(remote.notes)
        return TaskEntity(
            task = remote.title,
            completedAt = if (remote.status == "completed") now else null,
            createdAt = now,
            dueAt = remote.due?.fromRfc3339(),
            notes = parsedNotes,
            extraPropertiesJson = parsedProps,
            googleTaskId = remote.id,
            googleTaskListId = DEFAULT_TASK_LIST,
            lastSyncedAt = now,
            syncStatus = "synced",
        )
    }
}

private fun TaskEntity.toRemoteTask() = RemoteTask(
    id = googleTaskId,
    title = task,
    notes = encodeNotesField(notes, extraPropertiesJson),
    status = if (isCompleted) "completed" else "needsAction",
    due = dueAt?.toRfc3339(),
    completed = null,
    deleted = false,
)

private fun encodeNotesField(notes: String?, extraPropertiesJson: String?): String? = when {
    notes != null && extraPropertiesJson != null -> "$notes\n$PROPS_SEPARATOR\n$extraPropertiesJson"
    notes != null -> notes
    extraPropertiesJson != null -> "$PROPS_SEPARATOR\n$extraPropertiesJson"
    else -> null
}

private fun parseNotesField(raw: String?): Pair<String?, String?> {
    if (raw == null) return null to null
    val separatorPattern = "\n$PROPS_SEPARATOR\n"
    val idx = raw.indexOf(separatorPattern)
    return when {
        idx >= 0 -> raw.substring(0, idx).takeIf { it.isNotEmpty() } to
            raw.substring(idx + separatorPattern.length).takeIf { it.isNotEmpty() }
        raw.startsWith("$PROPS_SEPARATOR\n") ->
            null to raw.removePrefix("$PROPS_SEPARATOR\n").takeIf { it.isNotEmpty() }
        else -> raw.takeIf { it.isNotEmpty() } to null
    }
}

private fun Instant.toRfc3339(): String =
    SimpleDateFormat(RFC3339_PATTERN, Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date.from(this))

private fun String.fromRfc3339(): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()
