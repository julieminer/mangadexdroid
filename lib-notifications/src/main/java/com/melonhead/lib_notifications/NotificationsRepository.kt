package com.melonhead.lib_notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.melonhead.lib_app_context.AppContext
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_notifications.models.ChapterNotification
import com.melonhead.lib_read_status.ReadStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

interface NotificationsRepository

internal class NotificationsRepositoryImpl(
    private val context: Context,
    private val appContext: AppContext,
    externalScope: CoroutineScope,
    private val appData: AppData,

    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,

    private val readStatusRepository: ReadStatusRepository,
    private val appEventsRepository: AppEventsRepository,
    private val authFailedNotificationChannel: AuthFailedNotificationChannel,
    private val newChapterNotificationChannel: NewChapterNotificationChannel,
): NotificationsRepository {
    private var hasLaunched = false
    private val scope = externalScope + SupervisorJob()

    init {
        scope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is AppLifecycleEvent.AppForegrounded -> {
                                hasLaunched = true
                            }

                            AuthenticationEvent.LoggedOut -> {
                                if (!hasLaunched) return@launch
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

        scope.launch {
            combine(mangaDb.allSeries(), chapterDb.allChapters()) { manga, chapters ->
                launch {
                    postNewChapterNotifications(manga, chapters)
                }
            }
        }
    }

    private suspend fun postNewChapterNotifications(
        manga: List<MangaEntity>,
        chapters: List<ChapterEntity>
    ) {
        if (appContext.isInForeground) return
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return
        val installDateSeconds = appData.installDateSeconds.getValue()
        val newChapters = chapters
            .filter { !readStatusRepository.isRead(it) }
            .filter { !it.blockedChapter }
            .filter { it.createdAt.epochSeconds > installDateSeconds }
            .map {
                ChapterNotification(
                    chapterId = it.id,
                    chapterTitle = "${it.chapter}",
                    mangaId = it.mangaId,
                    mangaTitle = manga.find { manga -> manga.id == it.mangaId }?.chosenTitle ?: "",
                )
            }

        newChapterNotificationChannel.post(context, newChapters, installDateSeconds)
    }

}