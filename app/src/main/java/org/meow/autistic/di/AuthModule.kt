package org.meow.autistic.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.autistic.BuildConfig
import org.meow.autistic.data.auth.GoogleAuthManager
import org.meow.autistic.data.auth.TokenStore

val authModule = module {
    single { TokenStore.create(androidContext()) }
    single { GoogleAuthManager(androidContext(), get(), BuildConfig.FIREBASE_WEB_CLIENT_ID) }
}
