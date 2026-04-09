package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.note.NoteRepository
import org.meow.autistic.data.product.ProductDatabase
import org.meow.autistic.data.task.TaskDatabase

val databaseModule = module {
    single { TaskDatabase.getDatabase(androidContext()) }
    single { get<TaskDatabase>().taskDao() }
    single { get<TaskDatabase>().calendarDao() }
    single { get<TaskDatabase>().dailyTaskDao() }
    single { get<TaskDatabase>().noteDao() }
    single { NoteRepository(get()) }
    single { ProductDatabase.getDatabase(androidContext()) }
    single { get<ProductDatabase>().productDao() }
}
