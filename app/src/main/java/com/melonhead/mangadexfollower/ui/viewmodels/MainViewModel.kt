package com.melonhead.mangadexfollower.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.melonhead.data_shared.models.ui.UIChapter
import com.melonhead.data_shared.models.ui.UIManga
import com.melonhead.feature_authentication.AuthRepository
import com.melonhead.feature_authentication.models.LoginStatus
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_notifications.NewChapterNotificationChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.json.Json

class MainViewModel(
    private val authRepository: AuthRepository,
    private val appEventsRepository: AppEventsRepository,
    private val appData: AppData,
): ViewModel() {
    private val mutableLoginStatus = MutableStateFlow<LoginStatus?>(null)
    val loginStatus = mutableLoginStatus.asLiveData(viewModelScope.coroutineContext)

    init {
        viewModelScope.launch {
            combine(appEventsRepository.events, appData.token) { event, token ->
                when (event) {
                    is AuthenticationEvent.LoggedIn, is AuthenticationEvent.LoggedOut -> {
                        if (token == null) {
                            Clog.w("Setting status to logged out: event = ${event}, token is null")
                            mutableLoginStatus.value = LoginStatus.LoggedOut
                        } else {
                            mutableLoginStatus.value = LoginStatus.LoggedIn
                        }
                    }
                    is AuthenticationEvent.LoggingIn -> {
                        mutableLoginStatus.value = LoginStatus.LoggingIn
                    }
                }
            }
        }

        viewModelScope.launch {
            mutableLoginStatus.value = if (appData.token.firstOrNull() != null) LoginStatus.LoggedIn else LoginStatus.LoggedOut
        }
    }

    val clientDetails = flow<Triple<String, String, String>> {
        appData.getClient()
    }.asLiveData(viewModelScope.coroutineContext)

    @Deprecated("Use oauth variant")
    fun authenticate(email: String, password: String) = viewModelScope.launch {
        // TODO: replace with event?
        authRepository.authenticate(email, password)
    }

    fun authenticate(email: String, password: String, clientId: String, clientSecret: String) = viewModelScope.launch {
        // TODO: replace with event?
        authRepository.authenticate(email, password, clientId, clientSecret)
    }

    fun parseIntent(context: Context, intent: Intent) {
        val mangaId = intent.getStringExtra(NewChapterNotificationChannel.MANGA_ID_EXTRA) ?: return
        val chapterId = intent.getStringExtra(NewChapterNotificationChannel.CHAPTER_ID_EXTRA) ?: return
        appEventsRepository.postEvent(UserEvent.OpenedNotification(context, mangaId, chapterId))
    }
}
