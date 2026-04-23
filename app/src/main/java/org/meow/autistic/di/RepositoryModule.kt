package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.health.HealthConnectRepository
import org.meow.autistic.data.photo.ClaudeVisionClient
import org.meow.autistic.data.product.OpenFoodFactsApiClient
import org.meow.autistic.data.product.ProductRepository
import org.meow.autistic.data.product.UsdaFdcApiClient
import org.meow.autistic.data.task.DailyTaskRepository
import org.meow.autistic.data.task.TaskRepository

val repositoryModule = module {
    single { TaskRepository(get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { UsdaFdcApiClient() }
    single { OpenFoodFactsApiClient() }
    single { ProductRepository(get(), get(), get(), get()) }
    single { DailyTaskRepository(get(), get()) }
    single { ClaudeVisionClient() }
    single { HealthConnectRepository(androidContext(), get()) }
}
