package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.energy.EnergyRepository
import org.meow.autistic.data.foodlog.FoodCacheRepository
import org.meow.autistic.data.foodlog.FoodLogRepository
import org.meow.autistic.data.mood.MoodRepository
import org.meow.autistic.data.note.NoteRepository
import org.meow.autistic.data.product.ProductDatabase
import org.meow.autistic.data.task.TaskDatabase

val databaseModule = module {
    single { TaskDatabase.getDatabase(androidContext()) }
    single { get<TaskDatabase>().taskDao() }
    single { get<TaskDatabase>().calendarDao() }
    single { get<TaskDatabase>().dailyTaskDao() }
    single { get<TaskDatabase>().noteDao() }
    single { get<TaskDatabase>().moodDao() }
    single { get<TaskDatabase>().foodLogDao() }
    single { get<TaskDatabase>().foodLogItemDao() }
    single { get<TaskDatabase>().healthSnapshotDao() }
    single { get<TaskDatabase>().foodCacheDao() }
    single { get<TaskDatabase>().energyDao() }
    single { NoteRepository(get()) }
    single { MoodRepository(get()) }
    single { FoodLogRepository(get(), get()) }
    single { FoodCacheRepository(get()) }
    single { EnergyRepository(get()) }
    single { ProductDatabase.getDatabase(androidContext()) }
    single { get<ProductDatabase>().productDao() }
}
