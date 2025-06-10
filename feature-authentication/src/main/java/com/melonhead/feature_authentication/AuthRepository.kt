package com.melonhead.feature_authentication

import android.content.Context
import com.melonhead.data_authentication.models.AuthToken
import com.melonhead.data_authentication.models.OAuthToken
import com.melonhead.data_authentication.services.LoginService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_logging.Clog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

interface AuthRepository {
    suspend fun authenticate(email: String, password: String, clientId: String, clientSecret: String)
}

internal class AuthRepositoryImpl(
    private val context: Context,
    private val appData: AppData,
    private val loginService: LoginService,
    private val appEventsRepository: AppEventsRepository,
    externalScope: CoroutineScope,
) : AuthRepository {
    init {
        externalScope.launch {
            if (appData.token.firstOrNull() != null) {
                appEventsRepository.postEvent(AuthenticationEvent.LoggingIn)
                refreshOAuthToken(logoutOnFail = false)
            } else {
                appEventsRepository.postEvent(AuthenticationEvent.LoggedOut)
            }
        }

        externalScope.launch(context = Dispatchers.IO) {
            appEventsRepository.events.collectLatest {
                launch {
                    if (it is AuthenticationEvent.RefreshToken) {
                        refreshOAuthToken(logoutOnFail = it.logoutOnFail)
                        it.completionJob?.complete(Unit)
                    }
                }
            }
        }
    }

    private suspend fun refreshOAuthToken(logoutOnFail: Boolean): OAuthToken? {
        val (email, id, secret) = appData.getClient() ?: return null
        return refreshOAuthToken(logoutOnFail, email, id, secret)
    }

    private suspend fun refreshOAuthToken(logoutOnFail: Boolean, email: String, apiClient: String, apiSecret: String): OAuthToken? {
        fun signOut() {
            if (logoutOnFail) {
                Clog.e("Signing out, refresh failed", Exception())
            }
            appEventsRepository.postEvent(AuthenticationEvent.LoggedOut)
        }

        val currentToken = appData.token.firstOrNull()
        if (currentToken == null) {
            Clog.w("signOut: Current token null")
            signOut()
            return null
        }

        if (email.isEmpty() || apiClient.isEmpty() || apiSecret.isEmpty()) {
            Clog.w("signOut: missing client info")
            signOut()
            return null
        }

        val newToken = loginService.refreshOAuthToken(logoutOnFail, email, apiClient, apiSecret)
        appData.updateClient(email, apiClient, apiSecret)
        appData.updateToken(session = newToken?.accessToken, refresh = newToken?.refreshToken)
        if (newToken == null) {
            Clog.w("signOut: new token null")
            signOut()
        }
        return newToken
    }

    override suspend fun authenticate(
        email: String,
        password: String,
        clientId: String,
        clientSecret: String
    ) {
        Clog.i("authenticate")
        appEventsRepository.postEvent(AuthenticationEvent.LoggingIn)
        appData.updateClient(email, clientId, clientSecret)
        val token = loginService.authenticateOauth(email, password, clientId, clientSecret)
        appData.updateToken(session = token?.accessToken, refresh = token?.refreshToken)
        if (token != null) {
            appEventsRepository.postEvent(AuthenticationEvent.LoggedIn)
            Clog.i("Refresh: authenticate")
            appEventsRepository.postEvent(UserEvent.RefreshManga())
        }
    }
}
