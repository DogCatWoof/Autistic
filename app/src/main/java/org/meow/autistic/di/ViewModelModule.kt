package org.meow.autistic.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.meow.autistic.ui.screens.DailyTasksViewModel
import org.meow.autistic.ui.screens.ConversationViewModel
import org.meow.autistic.ui.screens.MoodViewModel
import org.meow.autistic.ui.screens.NotesViewModel
import org.meow.autistic.ui.screens.TaskViewModel

val viewModelModule = module {
    viewModel { TaskViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DailyTasksViewModel(get()) }
    viewModel { NotesViewModel(get(), get(), androidApplication()) }
    viewModel { MoodViewModel(get()) }
    viewModel { ConversationViewModel(get(), androidApplication()) }
}
