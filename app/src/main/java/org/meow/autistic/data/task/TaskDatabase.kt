package org.meow.autistic.data.task

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.meow.autistic.data.calendar.CalendarDao
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.product.ProductDao
import org.meow.autistic.data.product.ProductEntity
import java.time.Instant

/** Converts [Instant] to/from ISO 8601 TEXT for Room storage. */
class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}

@Database(
    entities = [TaskEntity::class, CalendarEventEntity::class, ProductEntity::class, DailyTaskEntity::class],
    version = 11,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun calendarDao(): CalendarDao
    abstract fun productDao(): ProductDao
    abstract fun dailyTaskDao(): DailyTaskDao

    companion object {
        @Volatile
        private var Instance: TaskDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN googleTaskId TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN googleTaskListId TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN extraPropertiesJson TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN lastSyncedAt INTEGER")
                db.execSQL("ALTER TABLE todos ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'local'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        googleEventId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        startAt INTEGER NOT NULL,
                        endAt INTEGER NOT NULL,
                        isAllDay INTEGER NOT NULL,
                        calendarId TEXT NOT NULL,
                        lastSyncedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS products " +
                        "(barcode TEXT NOT NULL PRIMARY KEY, productJson TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN dailyTaskId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_tasks (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'General',
                        timeMinutes INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN expectedTimeMinutes INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN expectedTimeMinutes INTEGER")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos RENAME TO tasks")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate tasks with TEXT timestamp columns
                db.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        task TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL,
                        dueAt TEXT,
                        notes TEXT,
                        category TEXT NOT NULL DEFAULT 'General',
                        reminderSet INTEGER NOT NULL DEFAULT 0,
                        googleTaskId TEXT,
                        googleTaskListId TEXT,
                        extraPropertiesJson TEXT,
                        lastSyncedAt TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'local',
                        dailyTaskId INTEGER,
                        expectedTimeMinutes INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO tasks_new SELECT
                        id, task, isCompleted,
                        strftime('%Y-%m-%dT%H:%M:%SZ', createdAt / 1000, 'unixepoch'),
                        CASE WHEN dueAt IS NULL THEN NULL
                             ELSE strftime('%Y-%m-%dT%H:%M:%SZ', dueAt / 1000, 'unixepoch') END,
                        notes, category, reminderSet, googleTaskId, googleTaskListId,
                        extraPropertiesJson,
                        CASE WHEN lastSyncedAt IS NULL THEN NULL
                             ELSE strftime('%Y-%m-%dT%H:%M:%SZ', lastSyncedAt / 1000, 'unixepoch') END,
                        syncStatus, dailyTaskId, expectedTimeMinutes
                    FROM tasks
                """.trimIndent())
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

                // Recreate calendar_events with TEXT timestamp columns
                db.execSQL("""
                    CREATE TABLE calendar_events_new (
                        googleEventId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        startAt TEXT NOT NULL,
                        endAt TEXT NOT NULL,
                        isAllDay INTEGER NOT NULL,
                        calendarId TEXT NOT NULL,
                        lastSyncedAt TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO calendar_events_new SELECT
                        googleEventId, title,
                        strftime('%Y-%m-%dT%H:%M:%SZ', startAt / 1000, 'unixepoch'),
                        strftime('%Y-%m-%dT%H:%M:%SZ', endAt / 1000, 'unixepoch'),
                        isAllDay, calendarId,
                        strftime('%Y-%m-%dT%H:%M:%SZ', lastSyncedAt / 1000, 'unixepoch')
                    FROM calendar_events
                """.trimIndent())
                db.execSQL("DROP TABLE calendar_events")
                db.execSQL("ALTER TABLE calendar_events_new RENAME TO calendar_events")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'synced'")
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TaskDatabase::class.java, "autistic_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
