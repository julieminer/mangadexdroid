package com.melonhead.lib_notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.melonhead.lib_app_context.AppContext
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_read_status.ReadStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

interface NotificationsRepository

internal class NotificationsRepositoryImpl(
    private val context: Context,
    private val appContext: AppContext,
    private val externalScope: CoroutineScope,
    private val appData: AppData,

    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,

    private val readStatusRepository: ReadStatusRepository,
    private val appEventsRepository: AppEventsRepository,
    private val authFailedNotificationChannel: AuthFailedNotificationChannel,
    private val newChapterNotificationChannel: NewChapterNotificationChannel,
): NotificationsRepository {
    init {
        externalScope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            AuthenticationEvent.LoggedOut -> {
                                if (appContext.isInForeground) return@launch
                                val notificationManager = NotificationManagerCompat.from(context)
                                if (!notificationManager.areNotificationsEnabled()) return@launch
                                authFailedNotificationChannel.postAuthFailed(context)
                            }

                            is UserEvent.SetMarkChapterRead -> {
                                if (event.read) {
                                    newChapterNotificationChannel.dismissNotification(context, event.mangaId, event.chapterId)
                                }
                            }

                            else -> {
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        externalScope.launch {
            chapterDb.getAll().collectLatest {
                launch {
                    if (appContext.isInForeground) return@launch
                    val notificationManager = NotificationManagerCompat.from(context)
                    if (!notificationManager.areNotificationsEnabled()) return@launch
                    val newChapters = chapterDb.getAllSync()
                        .filter { !readStatusRepository.isRead(it) }
                        .filter { !it.blockedChapter }

                    val installDateSeconds = appData.installDateSeconds.firstOrNull() ?: 0L
                    // TODO: finish this
//                newChapterNotificationChannel.post(context, newChapters, installDateSeconds)
                }
            }
        }
    }


}