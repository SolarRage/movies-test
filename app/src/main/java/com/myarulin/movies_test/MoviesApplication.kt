package com.myarulin.movies_test

import android.app.Application
import com.myarulin.movies_test.di.appModule
import com.myarulin.movies_test.sdk.AdsManager
import com.myarulin.movies_test.sdk.sdkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MoviesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidContext(this@MoviesApplication)
            modules(appModule, sdkModule)
        }
        koinApplication.koin.get<AdsManager>().initialize(this)
    }
}
