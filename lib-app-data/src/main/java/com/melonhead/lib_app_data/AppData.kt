package com.melonhead.lib_app_data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.melonhead.lib_app_data.models.RenderStyle
import com.melonhead.lib_app_data.extensions.dataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

interface AppData {
    val token: Flow<Pair<String, String>?>
    val installDateSeconds: PreferenceValue<Long>
    val lastRefreshDateSeconds: PreferenceValue<Long>
    val autoMarkMangaCompleted: PreferenceValue<Boolean>
    val autoMarkMangaReading: PreferenceValue<Boolean>

    val renderStyle: PreferenceValueMapper<RenderStyle, String>
    val useDataSaver: PreferenceValue<Boolean>
    val chapterTapAreaSize: PreferenceValueMapper<Dp, Int>
    val showReadChapterCount: PreferenceValue<Int>

    suspend fun updateToken(session: String?, refresh: String?)
    suspend fun updateClient(email: String?, apiClient: String?, apiSecret: String?)
    suspend fun updateInstallTime()
    suspend fun updateLastRefreshDate()
    suspend fun getToken(): Pair<String, String>?
    suspend fun getSession(): String?
    suspend fun getRefresh(): String?
    suspend fun getClient(): Triple<String, String, String>?
}

class PreferenceValue<T>(
    private val dataStore: DataStore<Preferences>,
    private val preferenceKey: Preferences.Key<T>,
    internal val defaultValue: T,
) {
    val flow = dataStore.data.map { preferences ->
        preferences[preferenceKey] ?: defaultValue
    }.distinctUntilChanged()

    suspend fun setValue(value: T?) {
        dataStore.edit { settings ->
            if (value == null) {
                settings.remove(preferenceKey)
            } else {
                settings[preferenceKey] = value
            }
        }
    }

    suspend fun getValue(): T {
        return flow.firstOrNull() ?: defaultValue
    }

    @Composable
    fun collectAsState(): State<T> {
        return flow.collectAsState(initial = defaultValue)
    }
}

internal class AppDataImpl(
    appContext: Context,
): AppData {
    private val dataStore = appContext.dataStore

    private val tokenMutex = Mutex()
    private val clientMutex = Mutex()

    private val clientEmail = PreferenceValue(dataStore, stringPreferencesKey("client_email"), defaultValue = "")
    private val clientId = PreferenceValue(dataStore, stringPreferencesKey("client_id"), defaultValue = "")
    private val clientSecret = PreferenceValue(dataStore, stringPreferencesKey("client_secret"), defaultValue = "")

    private val authToken = PreferenceValue(dataStore, stringPreferencesKey("auth_token"), defaultValue = "")
    private val refreshToken = PreferenceValue(dataStore, stringPreferencesKey("refresh_token"), defaultValue = "")

    override val installDateSeconds = PreferenceValue(dataStore, longPreferencesKey("install_date"), defaultValue = 0L)
    override val lastRefreshDateSeconds = PreferenceValue(dataStore, longPreferencesKey("refresh_time"), defaultValue = 0L)

    override val autoMarkMangaCompleted = PreferenceValue(dataStore, booleanPreferencesKey("auto_mark_manga_completed"), defaultValue = true)
    override val autoMarkMangaReading = PreferenceValue(dataStore, booleanPreferencesKey("auto_mark_manga_reading"), defaultValue = true)

    override var token: Flow<Pair<String, String>?> = authToken.flow.combine(refreshToken.flow) { auth, refresh ->
        if (auth.isNullOrBlank() || refresh.isNullOrBlank()) return@combine null
        auth to refresh
    }.distinctUntilChanged()

    override val renderStyle = PreferenceValueMapper(
        PreferenceValue(dataStore, stringPreferencesKey("render_style"), defaultValue = ""),
        object: PreferenceValueMapping<RenderStyle, String> {
            override fun toValue(arg: RenderStyle): String {
                return arg.toString()
            }

            override fun fromValue(arg: String): RenderStyle {
                return RenderStyle.fromString(arg) ?: RenderStyle.Native
            }
        }
    )
    override val useDataSaver = PreferenceValue(dataStore, booleanPreferencesKey("data_saver"), defaultValue = false)

    override val chapterTapAreaSize = PreferenceValueMapper(
        PreferenceValue(dataStore, intPreferencesKey("chapter_tap_area_size"), defaultValue = 60),
        object: PreferenceValueMapping<Dp, Int> {
            override fun toValue(arg: Dp): Int {
                return arg.value.toInt()
            }

            override fun fromValue(arg: Int): Dp {
                return arg.dp
            }
        }
    )

    override val showReadChapterCount = PreferenceValue(dataStore, intPreferencesKey("show_read_chapter_count"), defaultValue = 1)

    override suspend fun updateClient(email: String?, apiClient: String?, apiSecret: String?) {
        clientMutex.withLock {
            clientEmail.setValue(email ?: "")
            clientId.setValue(apiClient ?: "")
            clientSecret.setValue(apiSecret ?: "")
        }
    }

    override suspend fun updateToken(session: String?, refresh: String?) {
        tokenMutex.withLock {
            authToken.setValue(session ?: "")
            refreshToken.setValue(refresh ?: "")
        }
    }

    override suspend fun updateInstallTime() {
        installDateSeconds.setValue(Clock.System.now().epochSeconds)
    }

    override suspend fun updateLastRefreshDate() {
        lastRefreshDateSeconds.setValue(Clock.System.now().epochSeconds)
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
            val email = clientEmail.getValue() ?: return null
            val id = clientId.getValue() ?: return null
            val secret = clientSecret.getValue() ?: return null
            return Triple(email, id, secret)
        }
    }
}
