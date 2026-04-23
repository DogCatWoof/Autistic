package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.debug.DebugSettings
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.diagnostics.QueryLogger

val diagnosticsModule = module {
    single { QueryLogger() }
    single { DebugSettings(androidContext()) }
    single { ExceptionReporter(androidContext(), get()) }
}
