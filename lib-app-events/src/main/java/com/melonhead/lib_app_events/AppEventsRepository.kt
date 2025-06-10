package com.melonhead.lib_app_events

import android.content.Context
import com.melonhead.lib_app_events.events.AppEvent
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_core.extensions.networkAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface AppEventsRepository {
    val events: Flow<AppEvent>
    fun postEvent(appEvent: AppEvent)
}

internal class AppEventsRepositoryImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
): AppEventsRepository {
    private val mutableEvents = MutableSharedFlow<AppEvent>()
    override val events: Flow<AppEvent> = mutableEvents.asSharedFlow()

    init {
        coroutineScope.launch {
            context.networkAvailability().collectLatest {
                postEvent(AppLifecycleEvent.ConnectionChanged(it))
            }
        }
    }

    override fun postEvent(appEvent: AppEvent) {
        coroutineScope.launch {
            mutableEvents.emit(appEvent)
        }
    }
}
