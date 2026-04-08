package org.meow.autistic.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.data.backup.DriveBackupService
import org.meow.autistic.data.calendar.CalendarRemoteSource
import org.meow.autistic.data.calendar.CalendarSyncService
import org.meow.autistic.data.calendar.CalendarSyncTokenStore
import org.meow.autistic.data.sync.SyncOrchestrator
import org.meow.autistic.data.sync.SyncScheduler
import org.meow.autistic.data.task.GoogleTasksRemoteSource
import org.meow.autistic.data.task.GoogleTasksSyncService

val syncModule = module {
    single { WorkManager.getInstance(androidContext()) }
    single { SyncScheduler(get()) }
    single { CalendarSyncTokenStore(androidContext()) }
    single { GoogleTasksRemoteSource() }
    single { CalendarRemoteSource() }
    single {
        val authManager = get<org.meow.autistic.data.auth.GoogleAuthManager>()
        GoogleTasksSyncService(
            remoteSource = get(),
            repository = get(),
            tokenProvider = { authManager.getValidToken() },
        )
    }
    single {
        val authManager = get<org.meow.autistic.data.auth.GoogleAuthManager>()
        CalendarSyncService(
            remoteSource = get(),
            repository = get(),
            syncTokenStore = get(),
            tokenProvider = { authManager.getValidToken() },
        )
    }
    single {
        val authManager = get<org.meow.autistic.data.auth.GoogleAuthManager>()
        DriveBackupService(
            context = androidContext(),
            tokenProvider = { authManager.getValidToken() },
        )
    }
    single { SyncOrchestrator(get(), get(), get()) }
}
