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
import org.meow.autistic.data.foodlog.FoodCacheDao
import org.meow.autistic.data.foodlog.FoodCacheEntity
import org.meow.autistic.data.foodlog.FoodLogDao
import org.meow.autistic.data.foodlog.FoodLogEntry
import org.meow.autistic.data.foodlog.FoodLogItemDao
import org.meow.autistic.data.foodlog.FoodLogItemEntry
import org.meow.autistic.data.health.HealthSnapshotDao
import org.meow.autistic.data.health.HealthSnapshotEntity
import org.meow.autistic.data.mood.MoodDao
import org.meow.autistic.data.mood.MoodEntity
import org.meow.autistic.data.note.NoteDao
import org.meow.autistic.data.note.NoteEntity
import org.meow.autistic.data.sequence.SequenceDao
import org.meow.autistic.data.sequence.SequenceEntity
import org.meow.autistic.data.sequence.SequenceRunEntity
import org.meow.autistic.data.sequence.SequenceStepEntity
import org.meow.autistic.data.sequence.SequenceStepProgressEntity
import java.time.Instant

/** Converts [Instant] to/from ISO 8601 TEXT for Room storage. */
class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}

internal val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE notes ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE notes ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE moods ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE moods ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE moods ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE moods ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_log_items ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE food_log_items ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE food_log_items ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE food_log_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE health_snapshots ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE health_snapshots ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE sequences ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE sequences ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE sequences ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE sequences ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sequence_steps ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE sequence_steps ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE sequence_steps ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE sequence_steps ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sequence_runs ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE sequence_runs ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE sequence_runs ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE daily_tasks ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE daily_tasks ADD COLUMN lastModifiedAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'")
        db.execSQL("ALTER TABLE daily_tasks ADD COLUMN pendingFirestoreSync INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(
    entities = [TaskEntity::class, CalendarEventEntity::class, DailyTaskEntity::class, NoteEntity::class, MoodEntity::class, FoodLogEntry::class, FoodLogItemEntry::class, HealthSnapshotEntity::class, FoodCacheEntity::class, SequenceEntity::class, SequenceStepEntity::class, SequenceRunEntity::class, SequenceStepProgressEntity::class],
    version = 18,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun calendarDao(): CalendarDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun noteDao(): NoteDao
    abstract fun moodDao(): MoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun foodLogItemDao(): FoodLogItemDao
    abstract fun healthSnapshotDao(): HealthSnapshotDao
    abstract fun foodCacheDao(): FoodCacheDao
    abstract fun sequenceDao(): SequenceDao

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
                    .addMigrations(MIGRATION_17_18)
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
