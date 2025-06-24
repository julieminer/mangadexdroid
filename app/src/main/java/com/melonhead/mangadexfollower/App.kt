package com.melonhead.mangadexfollower

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.melonhead.lib_app_context.AppContext
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_networking.extensions.error401Callback
import com.melonhead.mangadexfollower.di.AppModule
import com.melonhead.mangadexfollower.work_manager.RefreshWorker
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class App: Application() {
    private val appEventsRepository: AppEventsRepository by inject()
    private val appNavigationMap: AppNavigationMap by inject()
    private val appContext: AppContext by inject()

    override fun onCreate() {
        super.onCreate()

        instance = this

        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@App)
            // Load modules
            modules(AppModule)
        }

        // force navigation initialization
        appNavigationMap

        error401Callback = {
            appEventsRepository.postEvent(AuthenticationEvent.RefreshToken(logoutOnFail = true))
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                appContext.isInForeground = true
                appEventsRepository.postEvent(AppLifecycleEvent.AppForegrounded)
                RefreshWorker.scheduleDebugTask(this@App)
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                appContext.isInForeground = false
            }
        })
    }

    companion object {
        private lateinit var instance: App
    }
}
