package com.melonhead.lib_app_data

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.melonhead.lib_app_data.models.RenderStyle
import com.melonhead.lib_app_data.extensions.dataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

interface AppData {
    val token: Flow<Pair<String, String>?>
    val installDateSeconds: Flow<Long?>
    val lastRefreshDateSeconds: Flow<Long?>
    val autoMarkMangaCompleted: Flow<Boolean>
    val autoMarkMangaReading: Flow<Boolean>

    val renderStyle: RenderStyle
    val useDataSaver: Boolean
    val chapterTapAreaSize: Dp
    val showReadChapterCount: Int

    suspend fun updateToken(session: String?, refresh: String?)
    suspend fun updateClient(email: String?, apiClient: String?, apiSecret: String?)
    suspend fun updateInstallTime()
    suspend fun updateLastRefreshDate()
    suspend fun updateRenderStyle(renderStyle: RenderStyle)
    suspend fun updateAutoMarkMangaCompleted(autoMarkMangaCompleted: Boolean)
    suspend fun updateAutoMarkMangaReading(autoMarkMangaReading: Boolean)
    suspend fun setUseDataSaver(useDataSaver: Boolean)
    suspend fun setShowReadChapterCount(readChapterCount: Int)
    suspend fun getToken(): Pair<String, String>?
    suspend fun getSession(): String?
    suspend fun getRefresh(): String?
    suspend fun getClient(): Triple<String, String, String>?
}

internal class AppDataImpl(
    private val appContext: Context,
): AppData {
    private val CLIENT_ID = stringPreferencesKey("client_id")
    private val CLIENT_SECRET = stringPreferencesKey("client_secret")
    private val CLIENT_EMAIL = stringPreferencesKey("client_email")
    private val AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val INSTALL_DATE = longPreferencesKey("install_date")
    private val REFRESH_TIME = longPreferencesKey("refresh_time")
    private val AUTO_MARK_MANGA_COMPLETED = booleanPreferencesKey("auto_mark_manga_completed")
    private val AUTO_MARK_MANGA_READING = booleanPreferencesKey("auto_mark_manga_reading")

    private val tokenMutex = Mutex()
    private val clientMutex = Mutex()

    private val clientEmail: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[CLIENT_EMAIL] ?: ""
    }.distinctUntilChanged()

    private val clientId: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[CLIENT_ID] ?: ""
    }.distinctUntilChanged()

    private val clientSecret: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[CLIENT_SECRET] ?: ""
    }.distinctUntilChanged()

    private val authTokenFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        // No type safety.
        preferences[AUTH_TOKEN] ?: ""
    }.distinctUntilChanged()

    private val refreshTokenFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        // No type safety.
        preferences[REFRESH_TOKEN] ?: ""
    }.distinctUntilChanged()

    override val installDateSeconds: Flow<Long?> = appContext.dataStore.data.map { preferences ->
        preferences[INSTALL_DATE] ?: 0L
    }.distinctUntilChanged()

    override val lastRefreshDateSeconds: Flow<Long?> = appContext.dataStore.data.map { preferences ->
        preferences[REFRESH_TIME]
    }.distinctUntilChanged()

    override val autoMarkMangaCompleted: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[AUTO_MARK_MANGA_COMPLETED] ?: true
    }.distinctUntilChanged()

    override val autoMarkMangaReading: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[AUTO_MARK_MANGA_READING] ?: true
    }.distinctUntilChanged()

    override var token: Flow<Pair<String, String>?> = authTokenFlow.combine(refreshTokenFlow) { auth, refresh ->
        if (auth.isBlank() || refresh.isBlank()) return@combine null
        auth to refresh
    }.distinctUntilChanged()

    override val renderStyle: RenderStyle
        get() = RenderStyle.Native

    override val useDataSaver: Boolean
        get() = false

    override val chapterTapAreaSize: Dp
        get() = 60.dp

    override val showReadChapterCount: Int
        get() = 1

    override suspend fun updateClient(email: String?, apiClient: String?, apiSecret: String?) {
        clientMutex.withLock {
            appContext.dataStore.edit { settings ->
                if (settings[CLIENT_EMAIL] != email)
                    settings[CLIENT_EMAIL] = email ?: ""

                if (settings[CLIENT_ID] != apiClient)
                    settings[CLIENT_ID] = apiClient ?: ""

                if (settings[CLIENT_SECRET] != apiSecret)
                    settings[CLIENT_SECRET] = apiSecret ?: ""
            }
        }
    }

    override suspend fun updateToken(session: String?, refresh: String?) {
        tokenMutex.withLock {
            appContext.dataStore.edit { settings ->
                if (settings[AUTH_TOKEN] != session)
                    settings[AUTH_TOKEN] = session ?: ""

                if (settings[REFRESH_TOKEN] != refresh)
                    settings[REFRESH_TOKEN] = refresh ?: ""
            }
        }
    }

    override suspend fun updateInstallTime() {
        appContext.dataStore.edit { settings ->
            settings[INSTALL_DATE] = Clock.System.now().epochSeconds
        }
    }
    override suspend fun updateLastRefreshDate() {
        appContext.dataStore.edit { settings ->
            settings[REFRESH_TIME] = Clock.System.now().epochSeconds
        }
    }

    override suspend fun updateAutoMarkMangaCompleted(autoMarkMangaCompleted: Boolean) {
        appContext.dataStore.edit { settings ->
            settings[AUTO_MARK_MANGA_COMPLETED] = autoMarkMangaCompleted
        }
    }

    override suspend fun updateAutoMarkMangaReading(autoMarkMangaReading: Boolean) {
        appContext.dataStore.edit { settings ->
            settings[AUTO_MARK_MANGA_READING] = autoMarkMangaReading
        }
    }

    override suspend fun getToken(): Pair<String, String>? {
        tokenMutex.withLock {
            return token.first()
        }
    }

    override suspend fun getSession(): String? {
        tokenMutex.withLock {
            return token.first()?.first
        }
    }

    override suspend fun getRefresh(): String? {
        tokenMutex.withLock {
            return token.first()?.second
        }
    }

    override suspend fun getClient(): Triple<String, String, String>? {
        clientMutex.withLock {
            val email = clientEmail.firstOrNull() ?: return null
            val id = clientId.firstOrNull() ?: return null
            val secret = clientSecret.firstOrNull() ?: return null
            return Triple(email, id, secret)
        }
    }

    override suspend fun updateRenderStyle(renderStyle: RenderStyle) {
        TODO("Not yet implemented")
    }

    override suspend fun setUseDataSaver(useDataSaver: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun setShowReadChapterCount(readChapterCount: Int) {
        TODO("Not yet implemented")
    }
}
