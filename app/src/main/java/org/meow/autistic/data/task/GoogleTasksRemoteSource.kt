package org.meow.autistic.data.task

import android.util.Log
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Raw fields from the Google Tasks API for a single task.
 *
 * @param id Google-assigned task ID; null for tasks not yet created remotely.
 * @param title Task title.
 * @param notes Raw notes field — may contain an embedded extraPropertiesJson blob.
 * @param status "needsAction" or "completed".
 * @param due RFC 3339 date string (date portion only); null if no due date.
 * @param completed RFC 3339 timestamp when the task was completed; null if not completed.
 * @param deleted True if the task was deleted on the remote.
 */
data class RemoteTask(
    val id: String?,
    val title: String,
    val notes: String?,
    val status: String,
    val due: String?,
    val completed: String?,
    val deleted: Boolean,
)

private const val APP_NAME = "Autistic"
private const val DEFAULT_TASK_LIST = "@default"

/**
 * Raw HTTP client for the Google Tasks API.
 *
 * All methods require a valid OAuth access [token] and run on [Dispatchers.IO].
 * No business logic — mapping, conflict resolution, and sync orchestration
 * belong in GoogleTasksSyncService.
 *
 * @param clientFactory Builds the [Tasks] service for a given token.
 *   Override in tests to inject a mock client.
 */
class GoogleTasksRemoteSource(
    private val clientFactory: (token: String) -> Tasks = { token ->
        Tasks.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
            },
        ).setApplicationName(APP_NAME).build()
    },
) {

    /**
     * Returns all tasks in the default task list, including deleted and hidden tasks.
     * Follows Google's pagination until all pages are consumed.
     */
    suspend fun fetchTasks(token: String): List<RemoteTask> = withContext(Dispatchers.IO) {
        val client = clientFactory(token)
        val result = mutableListOf<RemoteTask>()
        var pageToken: String? = null
        do {
            val request = client.tasks().list(DEFAULT_TASK_LIST)
                .setShowDeleted(true)
                .setShowHidden(true)
                .setShowCompleted(false)
                .setFields("items(id,title,notes,status,due,completed,updated,deleted),nextPageToken")
            if (pageToken != null) {
                request.setPageToken(pageToken)
            }
            val response = request.execute()
            response.items?.forEach { task ->
                Log.d("GoogleTasksRemoteSource", "Fetched task raw: ${task}")
                result.add(task.toRemoteTask())
            }
            pageToken = response.nextPageToken
        } while (pageToken != null)
        result
    }

    /** Inserts [task] into the default task list and returns the created [RemoteTask]. */
    suspend fun createTask(token: String, task: RemoteTask): RemoteTask =
        withContext(Dispatchers.IO) {
            clientFactory(token).tasks()
                .insert(DEFAULT_TASK_LIST, task.toApiTask())
                .execute()
                .toRemoteTask()
        }

    /** Replaces the remote task matching [task.id] and returns the updated [RemoteTask]. */
    suspend fun updateTask(token: String, task: RemoteTask): RemoteTask =
        withContext(Dispatchers.IO) {
            val id = requireNotNull(task.id) { "Cannot update a task without an ID" }
            clientFactory(token).tasks()
                .update(DEFAULT_TASK_LIST, id, task.toApiTask())
                .execute()
                .toRemoteTask()
        }

    /** Hard-deletes the task identified by [googleTaskId] from the default task list. */
    suspend fun deleteTask(token: String, googleTaskId: String): Unit =
        withContext(Dispatchers.IO) {
            clientFactory(token).tasks()
                .delete(DEFAULT_TASK_LIST, googleTaskId)
                .execute()
        }
}

private fun Task.toRemoteTask() = RemoteTask(
    id = id,
    title = title ?: "",
    notes = notes,
    status = status ?: "needsAction",
    due = due,
    completed = completed,
    deleted = deleted ?: false,
)

private fun RemoteTask.toApiTask() = Task().also {
    it.id = id
    it.title = title
    it.notes = notes
    it.status = status
    it.due = due
    it.completed = completed
}
