package org.meow.autistic.di

import org.koin.dsl.module
import org.meow.autistic.data.calendar.CalendarRepository
import org.meow.autistic.data.product.ProductRepository
import org.meow.autistic.data.todo.TodoRepository

val repositoryModule = module {
    single { TodoRepository(get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { ProductRepository(get(), get()) }
}
