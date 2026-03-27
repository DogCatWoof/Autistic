package org.meow.autistic.data.sync

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSchedulerTest {

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val scheduler = SyncScheduler(workManager)

    // region schedulePeriodicSync

    @Test
    fun `schedulePeriodicSync enqueues wifi worker with correct name and UPDATE policy`() {
        scheduler.schedulePeriodicSync()

        verify {
            workManager.enqueueUniquePeriodicWork(
                WIFI_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>(),
            )
        }
    }

    @Test
    fun `schedulePeriodicSync enqueues cellular worker with correct name and UPDATE policy`() {
        scheduler.schedulePeriodicSync()

        verify {
            workManager.enqueueUniquePeriodicWork(
                CELLULAR_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>(),
            )
        }
    }

    @Test
    fun `schedulePeriodicSync enqueues two distinct periodic workers`() {
        val nameSlot = mutableListOf<String>()

        scheduler.schedulePeriodicSync()

        verify(exactly = 2) {
            workManager.enqueueUniquePeriodicWork(
                capture(nameSlot),
                any(),
                any<PeriodicWorkRequest>(),
            )
        }
        assertEquals(setOf(WIFI_WORK_NAME, CELLULAR_WORK_NAME), nameSlot.toSet())
    }

    // endregion

    // region triggerImmediate

    @Test
    fun `triggerImmediate enqueues one-time work with correct name and REPLACE policy`() {
        scheduler.triggerImmediate()

        verify {
            workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `triggerImmediate does not schedule periodic work`() {
        scheduler.triggerImmediate()

        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `schedulePeriodicSync does not call enqueueUniqueWork`() {
        scheduler.schedulePeriodicSync()

        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    // endregion

    // region error propagation

    @Test(expected = RuntimeException::class)
    fun `schedulePeriodicSync propagates WorkManager exception`() {
        every {
            workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>())
        } throws RuntimeException("WorkManager unavailable")
        scheduler.schedulePeriodicSync()
    }

    @Test(expected = RuntimeException::class)
    fun `triggerImmediate propagates WorkManager exception`() {
        every {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        } throws RuntimeException("WorkManager unavailable")
        scheduler.triggerImmediate()
    }

    // endregion
}
