package org.meow.autistic.data.task

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.meow.autistic.data.calendar.CalendarDao
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.keto.KetoDao
import org.meow.autistic.data.keto.KetoItemDao
import org.meow.autistic.data.keto.KetoItemEntry
import org.meow.autistic.data.keto.KetoLogEntry
import org.meow.autistic.data.mood.MoodDao
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.note.NoteDao
import org.meow.autistic.data.note.NoteEntity
import java.time.Instant

/** Converts [Instant] to/from ISO 8601 TEXT for Room storage. */
class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}

@Database(
    entities = [TaskEntity::class, CalendarEventEntity::class, DailyTaskEntity::class, NoteEntity::class, MoodEntity::class, KetoLogEntry::class, KetoItemEntry::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun calendarDao(): CalendarDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun noteDao(): NoteDao
    abstract fun moodDao(): MoodDao
    abstract fun ketoDao(): KetoDao
    abstract fun ketoItemDao(): KetoItemDao

    /** Flushes WAL to the main database file. Call before reading the raw file for backup. */
    fun checkpoint() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray<Any>()).close()
    }

    companion object {
        @Volatile
        private var Instance: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TaskDatabase::class.java, "autistic_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }

        /**
         * Closes and clears the singleton so the next [getDatabase] call reopens it.
         * Must be called before overwriting the database file on restore.
         */
        fun closeInstance() {
            synchronized(this) {
                Instance?.close()
                Instance = null
            }
        }
    }
}
