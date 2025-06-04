package com.melonhead.lib_app_events

import com.melonhead.lib_app_events.events.AppEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface AppEventsRepository {
    val events: Flow<AppEvent>
    fun postEvent(appEvent: AppEvent)
}

internal class AppEventsRepositoryImpl(
    private val coroutineScope: CoroutineScope,
): AppEventsRepository {
    private val mutableEvents = MutableSharedFlow<AppEvent>()
    override val events: Flow<AppEvent> = mutableEvents.asSharedFlow()

    override fun postEvent(appEvent: AppEvent) {
        coroutineScope.launch {
            mutableEvents.emit(appEvent)
        }
    }
}
