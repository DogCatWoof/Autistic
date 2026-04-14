package org.meow.autistic.data.product

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson

class NutrimentsConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromNutriments(value: Nutriments?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toNutriments(value: String?): Nutriments? =
        value?.let { gson.fromJson(it, Nutriments::class.java) }
}

/** Standalone Room database for barcode → product lookups. */
@Database(entities = [ProductEntity::class], version = 4, exportSchema = false)
@TypeConverters(NutrimentsConverter::class)
abstract class ProductDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile private var Instance: ProductDatabase? = null

        fun getDatabase(context: Context): ProductDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ProductDatabase::class.java, "product_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
