package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.BuildConfig
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.health.HealthConnectRepository
import org.meow.autistic.data.photo.ClaudeVisionClient
import org.meow.autistic.data.photo.FoodProductLookupService
import org.meow.autistic.data.product.OpenFoodFactsApiClient
import org.meow.autistic.data.product.ProductRepository
import org.meow.autistic.data.product.UsdaFdcApiClient
import org.meow.autistic.data.conversation.ResponseTemplateRepository
import org.meow.autistic.data.sequence.SequenceRepository
import org.meow.autistic.data.task.DailyTaskRepository
import org.meow.autistic.data.task.TaskRepository

val repositoryModule = module {
    single { TaskRepository(get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { UsdaFdcApiClient(apiKey = BuildConfig.USDA_API_KEY) }
    single { OpenFoodFactsApiClient() }
    single { ProductRepository(get(), get(), get(), get()) }
    single { DailyTaskRepository(get(), get()) }
    single { ClaudeVisionClient(apiKey = BuildConfig.ANTHROPIC_API_KEY) }
    single { FoodProductLookupService(get(), get()) }
    single { HealthConnectRepository(androidContext(), get()) }
    single { SequenceRepository(get(), get()) }
    single { ResponseTemplateRepository(androidContext()) }
}
