package org.meow.autistic.data.health

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.Instant

class HealthConnectSyncWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val repository: HealthConnectRepository = mockk()

    @Before
    fun setUp() {
        startKoin { modules(module { single { repository } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `doWork calls refreshTodaySnapshot and returns success`() = runTest {
        coEvery { repository.refreshTodaySnapshot() } returns HealthSnapshotEntity(
            date = "2026-04-27",
            steps = 5000,
            lastUpdatedAt = Instant.now(),
        )

        val result = HealthConnectSyncWorker(context, params).doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 1) { repository.refreshTodaySnapshot() }
    }

    @Test
    fun `doWork returns success when refreshTodaySnapshot returns null`() = runTest {
        coEvery { repository.refreshTodaySnapshot() } returns null

        val result = HealthConnectSyncWorker(context, params).doWork()

        assertEquals(Result.success(), result)
    }
}
