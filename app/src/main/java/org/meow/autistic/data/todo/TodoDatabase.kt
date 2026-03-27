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
    entities = [TodoEntity::class, CalendarEventEntity::class, ProductEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun calendarDao(): CalendarDao
    abstract fun productDao(): ProductDao

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

        fun getDatabase(context: Context): TodoDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TodoDatabase::class.java, "todo_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
