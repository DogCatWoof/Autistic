package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.conversation.ResponseTemplateRepository
import org.meow.autistic.data.task.DailyTaskRepository
import org.meow.autistic.data.task.TaskRepository

val repositoryModule = module {
    single { TaskRepository(get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { DailyTaskRepository(get(), get()) }
    single { ResponseTemplateRepository(androidContext()) }
}
