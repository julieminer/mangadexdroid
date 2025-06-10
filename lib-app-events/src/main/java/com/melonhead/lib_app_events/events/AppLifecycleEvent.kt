package com.melonhead.lib_app_events.events

sealed class AppLifecycleEvent: AppEvent {
    data object AppForegrounded: AppLifecycleEvent()
    data class ConnectionChanged(val connected: Boolean): AppLifecycleEvent()
}
