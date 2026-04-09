package org.meow.autistic.data.calendar

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meow.autistic.data.task.TaskDatabase
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CalendarDaoTest {

    private lateinit var db: TaskDatabase
    private lateinit var dao: CalendarDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.calendarDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun event(
        id: String,
        startAt: Instant = Instant.ofEpochMilli(1000L),
        endAt: Instant = Instant.ofEpochMilli(2000L),
    ) = CalendarEventEntity(
        googleEventId = id,
        title = "Event $id",
        startAt = startAt,
        endAt = endAt,
        isAllDay = false,
        calendarId = "primary",
        lastSyncedAt = Instant.now(),
    )

    @Test
    fun getAllEvents_returnsEmptyInitially() = runTest {
        assertTrue(dao.getAllEvents().first().isEmpty())
    }

    @Test
    fun upsertEvents_insertsAndReturnsAll() = runTest {
        dao.upsertEvents(listOf(event("e1"), event("e2")))
        val events = dao.getAllEvents().first()
        assertEquals(2, events.size)
    }

    @Test
    fun upsertEvents_replacesOnConflict() = runTest {
        dao.upsertEvents(listOf(event("e1").copy(title = "Old")))
        dao.upsertEvents(listOf(event("e1").copy(title = "New")))
        val events = dao.getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("New", events[0].title)
    }

    @Test
    fun getEventsInRange_filtersCorrectly() = runTest {
        dao.upsertEvents(listOf(
            event("in", startAt = Instant.ofEpochMilli(500L), endAt = Instant.ofEpochMilli(900L)),
            event("out", startAt = Instant.ofEpochMilli(1500L), endAt = Instant.ofEpochMilli(2500L)),
        ))
        val events = dao.getEventsInRange(Instant.ofEpochMilli(100L), Instant.ofEpochMilli(1000L)).first()
        assertEquals(1, events.size)
        assertEquals("in", events[0].googleEventId)
    }

    @Test
    fun deleteByIds_removesOnlyTargets() = runTest {
        dao.upsertEvents(listOf(event("keep"), event("gone")))
        dao.deleteByIds(listOf("gone"))
        val events = dao.getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("keep", events[0].googleEventId)
    }

    @Test
    fun deleteAll_clearsTable() = runTest {
        dao.upsertEvents(listOf(event("e1"), event("e2")))
        dao.deleteAll()
        assertTrue(dao.getAllEvents().first().isEmpty())
    }

    @Test
    fun markHidden_excludesEventFromGetAllEvents() = runTest {
        dao.upsertEvents(listOf(event("visible"), event("hidden")))
        dao.markHidden("hidden")
        val events = dao.getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("visible", events[0].googleEventId)
    }

    @Test
    fun markPendingDelete_excludesEventFromGetAllEvents() = runTest {
        dao.upsertEvents(listOf(event("keep"), event("delete")))
        dao.markPendingDelete("delete")
        val events = dao.getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("keep", events[0].googleEventId)
    }

    @Test
    fun getPendingDeletes_returnsOnlyPendingDeleteEvents() = runTest {
        dao.upsertEvents(listOf(event("synced"), event("pending")))
        dao.markPendingDelete("pending")
        val pending = dao.getPendingDeletes()
        assertEquals(1, pending.size)
        assertEquals("pending", pending[0].googleEventId)
    }
}
