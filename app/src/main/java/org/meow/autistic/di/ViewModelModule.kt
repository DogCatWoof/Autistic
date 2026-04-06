package org.meow.autistic.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.meow.autistic.ui.screens.DailyTasksViewModel
import org.meow.autistic.ui.screens.ScanViewModel
import org.meow.autistic.ui.screens.TodoViewModel

val viewModelModule = module {
    viewModel { TodoViewModel(get(), get(), get(), get(), get()) }
    viewModel { ScanViewModel(get()) }
    viewModel { DailyTasksViewModel(get()) }
}
