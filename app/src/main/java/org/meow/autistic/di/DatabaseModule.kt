package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.todo.TaskDatabase

val databaseModule = module {
    single { TaskDatabase.getDatabase(androidContext()) }
    single { get<TaskDatabase>().taskDao() }
    single { get<TaskDatabase>().calendarDao() }
    single { get<TaskDatabase>().productDao() }
    single { get<TaskDatabase>().dailyTaskDao() }
}
