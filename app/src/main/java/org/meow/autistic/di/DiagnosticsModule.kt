package org.meow.autistic.di

import org.koin.dsl.module
import org.meow.autistic.data.diagnostics.QueryLogger

val diagnosticsModule = module {
    single { QueryLogger() }
}
