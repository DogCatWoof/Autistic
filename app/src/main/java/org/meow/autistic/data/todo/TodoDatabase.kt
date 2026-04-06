package org.meow.autistic.data.todo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.meow.autistic.data.calendar.CalendarDao
import org.meow.autistic.data.calendar.CalendarEventEntity
import org.meow.autistic.data.product.ProductDao
import org.meow.autistic.data.product.ProductEntity

@Database(
    entities = [TodoEntity::class, CalendarEventEntity::class, ProductEntity::class, DailyTaskEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun calendarDao(): CalendarDao
    abstract fun productDao(): ProductDao
    abstract fun dailyTaskDao(): DailyTaskDao

    companion object {
        @Volatile
        private var Instance: TodoDatabase? = null

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

        fun getDatabase(context: Context): TodoDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TodoDatabase::class.java, "autistic_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
