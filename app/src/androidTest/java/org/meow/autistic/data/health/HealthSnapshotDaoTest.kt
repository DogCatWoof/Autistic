package org.meow.autistic.data.health

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HealthSnapshotDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: HealthSnapshotDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.healthSnapshotDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getByDate_returnsNullWhenEmpty() = runTest {
        val result = dao.getByDate("2025-01-01").first()
        assertNull(result)
    }

    @Test
    fun upsert_insertsAndRetrievesSnapshot() = runTest {
        val snapshot = HealthSnapshotEntity(
            date = "2025-01-01",
            steps = 8000,
            sleepMinutes = 450,
            avgHeartRateBpm = 72.5,
            weightKg = 75.0,
            caloriesBurned = 500.0,
            bloodGlucoseMmol = 5.2,
            lastUpdatedAt = Instant.now(),
        )
        dao.upsert(snapshot)

        val result = dao.getByDate("2025-01-01").first()
        assertEquals(8000L, result?.steps)
        assertEquals(450L, result?.sleepMinutes)
        assertEquals(72.5, result?.avgHeartRateBpm)
        assertEquals(75.0, result?.weightKg)
        assertEquals(500.0, result?.caloriesBurned)
        assertEquals(5.2, result?.bloodGlucoseMmol)
    }

    @Test
    fun upsert_updatesExistingSnapshot() = runTest {
        dao.upsert(HealthSnapshotEntity(date = "2025-01-01", steps = 3000))
        dao.upsert(HealthSnapshotEntity(date = "2025-01-01", steps = 9000))

        val result = dao.getByDate("2025-01-01").first()
        assertEquals(9000L, result?.steps)
    }

    @Test
    fun getLatest_returnsNewestByDate() = runTest {
        dao.upsert(HealthSnapshotEntity(date = "2025-01-01", steps = 1000))
        dao.upsert(HealthSnapshotEntity(date = "2025-01-03", steps = 3000))
        dao.upsert(HealthSnapshotEntity(date = "2025-01-02", steps = 2000))

        val result = dao.getLatest()
        assertEquals("2025-01-03", result?.date)
        assertEquals(3000L, result?.steps)
    }

    @Test
    fun getLatest_returnsNullWhenEmpty() = runTest {
        assertNull(dao.getLatest())
    }

    @Test
    fun upsert_allowsNullFields() = runTest {
        dao.upsert(HealthSnapshotEntity(date = "2025-01-01"))

        val result = dao.getByDate("2025-01-01").first()
        assertNull(result?.steps)
        assertNull(result?.sleepMinutes)
        assertNull(result?.avgHeartRateBpm)
    }
}
