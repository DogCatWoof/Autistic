package org.meow.autistic.di

import org.koin.dsl.module
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.product.OpenFoodFactsApiClient
import org.meow.autistic.data.product.ProductRepository
import org.meow.autistic.data.task.DailyTaskRepository
import org.meow.autistic.data.task.TaskRepository

val repositoryModule = module {
    single { TaskRepository(get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { OpenFoodFactsApiClient() }
    single { ProductRepository(get(), get(), get()) }
    single { DailyTaskRepository(get(), get()) }
}
