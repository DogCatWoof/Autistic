package org.meow.autistic.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseTest {

    private lateinit var db: TaskDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun database_buildsSuccessfully() {
        assertNotNull(db)
    }

    @Test
    fun taskDao_isNotNull() {
        assertNotNull(db.taskDao())
    }

    @Test
    fun calendarDao_isNotNull() {
        assertNotNull(db.calendarDao())
    }

    @Test
    fun getDatabase_returnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = TaskDatabase.getDatabase(context)
        val second = TaskDatabase.getDatabase(context)
        assertSame(first, second)
    }
}
