package org.meow.autistic

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.meow.autistic.di.authModule
import org.meow.autistic.di.databaseModule
import org.meow.autistic.di.repositoryModule
import org.meow.autistic.di.syncModule
import org.meow.autistic.di.viewModelModule

/**
 * Application entry point that initialises the Koin dependency-injection container.
 */
class AutisticApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AutisticApp)
            modules(
                databaseModule,
                authModule,
                repositoryModule,
                syncModule,
                viewModelModule,
            )
        }
    }
}
