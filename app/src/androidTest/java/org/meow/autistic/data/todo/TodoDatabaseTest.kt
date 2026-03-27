package org.meow.autistic.data.todo

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
class TodoDatabaseTest {

    private lateinit var db: TodoDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java)
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
    fun todoDao_isNotNull() {
        assertNotNull(db.todoDao())
    }

    @Test
    fun calendarDao_isNotNull() {
        assertNotNull(db.calendarDao())
    }

    @Test
    fun getDatabase_returnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = TodoDatabase.getDatabase(context)
        val second = TodoDatabase.getDatabase(context)
        assertSame(first, second)
    }
}
