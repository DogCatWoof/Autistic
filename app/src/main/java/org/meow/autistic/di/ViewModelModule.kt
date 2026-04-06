package org.meow.autistic.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.meow.autistic.ui.screens.DailyTasksViewModel
import org.meow.autistic.ui.screens.ScanViewModel
import org.meow.autistic.ui.screens.TaskViewModel

val viewModelModule = module {
    viewModel { TaskViewModel(get(), get(), get(), get(), get()) }
    viewModel { ScanViewModel(get()) }
    viewModel { DailyTasksViewModel(get()) }
}
