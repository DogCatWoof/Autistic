package org.meow.autistic.data.todo

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GoogleTasksSyncServiceTest {

    private val remoteSource = mockk<GoogleTasksRemoteSource>()
    private val repository = mockk<TaskRepository>(relaxed = true)
    private val token = "test-token"
    private val service = GoogleTasksSyncService(remoteSource, repository) { token }

    private val localNew = TaskEntity(
        id = 1L, task = "New task", createdAt = java.time.Instant.ofEpochMilli(1000L), syncStatus = "pending_push",
    )
    private val localExisting = TaskEntity(
        id = 2L, task = "Existing", createdAt = java.time.Instant.ofEpochMilli(1000L),
        googleTaskId = "remote1", syncStatus = "pending_push",
    )
    private val localPendingDelete = TaskEntity(
        id = 3L, task = "Delete me", createdAt = java.time.Instant.ofEpochMilli(1000L),
        googleTaskId = "remote2", syncStatus = "pending_delete",
    )
    private val remoteTask = RemoteTask(
        id = "remote1", title = "Remote title", notes = null,
        status = "needsAction", due = null, completed = null, deleted = false,
    )

    @Before
    fun setUp() {
        coEvery { repository.getPendingPush() } returns emptyList()
        coEvery { repository.getPendingDelete() } returns emptyList()
        coEvery { remoteSource.fetchTasks(token) } returns emptyList()
    }

    // region pushPending — creates

    @Test
    fun `pushPending creates task on remote when googleTaskId is null`() = runTest {
        val created = remoteTask.copy(id = "new-remote-id")
        coEvery { repository.getPendingPush() } returns listOf(localNew)
        coEvery { remoteSource.createTask(token, any()) } returns created

        service.pushPending()

        coVerify { remoteSource.createTask(token, any()) }
        coVerify { repository.markSynced(localNew.id, "new-remote-id", any()) }
    }

    @Test
    fun `pushPending updates task on remote when googleTaskId exists`() = runTest {
        coEvery { repository.getPendingPush() } returns listOf(localExisting)
        coEvery { remoteSource.updateTask(token, any()) } returns remoteTask

        service.pushPending()

        coVerify { remoteSource.updateTask(token, any()) }
        coVerify { repository.markSynced(localExisting.id, "remote1", any()) }
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `pushPending deletes completed task locally after pushing to remote`() = runTest {
        val completedTask = localExisting.copy(isCompleted = true)
        coEvery { repository.getPendingPush() } returns listOf(completedTask)
        coEvery { remoteSource.updateTask(token, any()) } returns remoteTask

        service.pushPending()

        coVerify { repository.markSynced(completedTask.id, "remote1", any()) }
        coVerify { repository.delete(completedTask) }
    }

    // endregion

    // region pushPending — deletes

    @Test
    fun `pushPending deletes remote task and local record`() = runTest {
        coEvery { repository.getPendingDelete() } returns listOf(localPendingDelete)
        coEvery { remoteSource.deleteTask(token, "remote2") } returns Unit

        service.pushPending()

        coVerify { remoteSource.deleteTask(token, "remote2") }
        coVerify { repository.delete(localPendingDelete) }
    }

    @Test
    fun `pushPending deletes local record without remote call when googleTaskId is null`() = runTest {
        val neverSynced = localPendingDelete.copy(googleTaskId = null)
        coEvery { repository.getPendingDelete() } returns listOf(neverSynced)

        service.pushPending()

        coVerify(exactly = 0) { remoteSource.deleteTask(any(), any()) }
        coVerify { repository.delete(neverSynced) }
    }

    // endregion

    // region pullAndMerge — deletions

    @Test
    fun `pullAndMerge removes locally tasks deleted on remote`() = runTest {
        val deleted = remoteTask.copy(id = "gone", deleted = true)
        coEvery { remoteSource.fetchTasks(token) } returns listOf(deleted)

        service.pullAndMerge()

        coVerify { repository.deleteByGoogleTaskIds(listOf("gone")) }
    }

    @Test
    fun `pullAndMerge does not call deleteByGoogleTaskIds when no remote deletions`() = runTest {
        coEvery { remoteSource.fetchTasks(token) } returns listOf(remoteTask)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify(exactly = 0) { repository.deleteByGoogleTaskIds(any()) }
    }

    @Test
    fun `pullAndMerge calls deleteAllCompleted after merging`() = runTest {
        coEvery { remoteSource.fetchTasks(token) } returns listOf(remoteTask)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify { repository.deleteAllCompleted() }
    }

    // endregion

    // region pullAndMerge — upserts

    @Test
    fun `pullAndMerge inserts new entity when no local match`() = runTest {
        coEvery { remoteSource.fetchTasks(token) } returns listOf(remoteTask)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify { repository.upsertFromRemote(match { it.googleTaskId == "remote1" && it.syncStatus == "synced" }) }
    }

    @Test
    fun `pullAndMerge merges remote title into existing entity`() = runTest {
        val existing = localExisting.copy(syncStatus = "synced")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(remoteTask.copy(title = "Updated title"))
        coEvery { repository.getByGoogleTaskId("remote1") } returns existing

        service.pullAndMerge()

        coVerify { repository.upsertFromRemote(match { it.task == "Updated title" && it.id == existing.id }) }
    }

    @Test
    fun `pullAndMerge preserves local-only fields on merge`() = runTest {
        val existing = localExisting.copy(category = "Work", reminderSet = true, syncStatus = "synced")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(remoteTask)
        coEvery { repository.getByGoogleTaskId("remote1") } returns existing

        service.pullAndMerge()

        coVerify {
            repository.upsertFromRemote(match {
                it.category == "Work" && it.reminderSet && it.syncStatus == "synced"
            })
        }
    }

    @Test
    fun `pullAndMerge maps completed status correctly`() = runTest {
        val completed = remoteTask.copy(status = "completed")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(completed)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify { repository.upsertFromRemote(match { it.isCompleted }) }
    }

    // endregion

    // region notes encoding/decoding

    @Test
    fun `pullAndMerge maps plain remote notes to entity notes`() = runTest {
        val withNotes = remoteTask.copy(notes = "Buy milk")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(withNotes)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify { repository.upsertFromRemote(match { it.notes == "Buy milk" && it.extraPropertiesJson == null }) }
    }

    @Test
    fun `pullAndMerge splits encoded notes and props`() = runTest {
        val encoded = remoteTask.copy(notes = "My note\n---autistic-props---\n{\"key\":\"val\"}")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(encoded)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify {
            repository.upsertFromRemote(match {
                it.notes == "My note" && it.extraPropertiesJson == "{\"key\":\"val\"}"
            })
        }
    }

    @Test
    fun `pullAndMerge handles props-only encoded notes`() = runTest {
        val encoded = remoteTask.copy(notes = "---autistic-props---\n{\"key\":\"val\"}")
        coEvery { remoteSource.fetchTasks(token) } returns listOf(encoded)
        coEvery { repository.getByGoogleTaskId("remote1") } returns null

        service.pullAndMerge()

        coVerify {
            repository.upsertFromRemote(match {
                it.notes == null && it.extraPropertiesJson == "{\"key\":\"val\"}"
            })
        }
    }

    @Test
    fun `pushPending encodes notes and props into remote notes field`() = runTest {
        val withNotes = localNew.copy(notes = "My note", extraPropertiesJson = "{\"k\":\"v\"}")
        val created = remoteTask.copy(id = "new-id")
        coEvery { repository.getPendingPush() } returns listOf(withNotes)
        coEvery { remoteSource.createTask(token, any()) } returns created

        service.pushPending()

        coVerify {
            remoteSource.createTask(token, match {
                it.notes == "My note\n---autistic-props---\n{\"k\":\"v\"}"
            })
        }
    }

    // endregion

    // region error propagation

    @Test(expected = RuntimeException::class)
    fun `pushPending propagates exception from createTask`() = runTest {
        coEvery { repository.getPendingPush() } returns listOf(localNew)
        coEvery { remoteSource.createTask(token, any()) } throws RuntimeException("API error")
        service.pushPending()
    }

    @Test(expected = RuntimeException::class)
    fun `pushPending propagates exception from updateTask`() = runTest {
        coEvery { repository.getPendingPush() } returns listOf(localExisting)
        coEvery { remoteSource.updateTask(token, any()) } throws RuntimeException("API error")
        service.pushPending()
    }

    @Test(expected = RuntimeException::class)
    fun `pushPending propagates exception from deleteTask`() = runTest {
        coEvery { repository.getPendingDelete() } returns listOf(localPendingDelete)
        coEvery { remoteSource.deleteTask(token, "remote2") } throws RuntimeException("API error")
        service.pushPending()
    }

    @Test(expected = RuntimeException::class)
    fun `pullAndMerge propagates exception from fetchTasks`() = runTest {
        coEvery { remoteSource.fetchTasks(token) } throws RuntimeException("API error")
        service.pullAndMerge()
    }

    @Test
    fun `pullAndMerge skips getByGoogleTaskId when remote task id is null`() = runTest {
        val noIdTask = remoteTask.copy(id = null, deleted = false)
        coEvery { remoteSource.fetchTasks(token) } returns listOf(noIdTask)
        service.pullAndMerge()
        coVerify(exactly = 0) { repository.getByGoogleTaskId(any()) }
    }

    // endregion
}
