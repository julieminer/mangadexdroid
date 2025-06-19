package com.melonhead.feature_settings.viewmodels

import androidx.lifecycle.ViewModel
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent

internal class SettingsViewModel(
    internal val appData: AppData,
    private val appEventsRepository: AppEventsRepository,
): ViewModel() {

    fun logout() {
        appEventsRepository.postEvent(UserEvent.LogOut)
    }
}
