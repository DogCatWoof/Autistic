package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.todo.TodoDatabase

val databaseModule = module {
    single { TodoDatabase.getDatabase(androidContext()) }
    single { get<TodoDatabase>().todoDao() }
    single { get<TodoDatabase>().calendarDao() }
    single { get<TodoDatabase>().productDao() }
    single { get<TodoDatabase>().dailyTaskDao() }
}
