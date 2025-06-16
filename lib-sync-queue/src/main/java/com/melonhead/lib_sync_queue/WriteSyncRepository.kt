package com.melonhead.lib_sync_queue

import android.content.Context
import com.melonhead.data_manga.models.ReadingStatus
import com.melonhead.data_manga.services.MangaService
import com.melonhead.data_rating.services.RatingService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.SystemLogicEvents
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_core.extensions.isNetworkAvailable
import com.melonhead.lib_core.extensions.throttleLatest
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.sync_queue.SyncQueueDao
import com.melonhead.lib_database.sync_queue.SyncQueueEntity
import com.melonhead.lib_database.sync_queue.SyncQueueEvent
import com.melonhead.lib_logging.Clog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

interface WriteSyncRepository

internal class WriteSyncRepositoryImpl(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val appEventsRepository: AppEventsRepository,
    private val syncQueueDb: SyncQueueDao,
    private val ratingService: RatingService,
    private val mangaService: MangaService,
    private val appData: AppData,
    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,
) : WriteSyncRepository {
    private val processQueueThrottled: (Unit) -> Unit = throttleLatest(1000L, externalScope) { _ ->
        processQueue()
    }

    init {
        Clog.i("WriteSyncRepository.init")
        externalScope.launch {
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is AuthenticationEvent.LoggedIn -> {
                                processQueueThrottled(Unit)
                            }

                            is UserEvent.SetMarkChapterRead -> {
                                markChapterRead(event.mangaId, event.chapterId, event.read)
                            }

                            is UserEvent.SetMangaRating -> {
                                setMangaRating(event.mangaId, event.rating)
                            }

                            is SystemLogicEvents.ChangeMangaReadingStatus -> {
                                setMangaReadingStatus(event.mangaId, event.readingStatus)
                            }

                            is SystemLogicEvents.UpdateMangaReadingStatus -> {
                                setUpdateMangaReadingStatus(event.mangaId, event.chapterId, event.readPostedChapter)
                            }

                            is UserEvent.RefreshManga -> {
                                processQueueThrottled(Unit)
                            }

                            is AppLifecycleEvent.ConnectionChanged -> {
                                if (event.connected) {
                                    processQueueThrottled(Unit)
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
    }

    private fun setMangaRating(mangaId: String, rating: Int) = externalScope.launch {
        syncQueueDb.insert(SyncQueueEntity(event = SyncQueueEvent.ChangeRating(mangaId = mangaId, rating)))
        processQueueThrottled(Unit)
    }

    private fun markChapterRead(mangaId: String, chapterId: String, read: Boolean) = externalScope.launch {
        syncQueueDb.insert(SyncQueueEntity(event = SyncQueueEvent.MarkRead(mangaId = mangaId, chapterId = chapterId, read = read)))
        processQueueThrottled(Unit)
    }

    private fun setMangaReadingStatus(mangaId: String, readingStatus: String) = externalScope.launch {
        syncQueueDb.insert(SyncQueueEntity(event = SyncQueueEvent.ChangeSeriesReadingStatus(mangaId = mangaId, readingStatus = readingStatus)))
        processQueueThrottled(Unit)
    }

    private fun setUpdateMangaReadingStatus(mangaId: String, chapterId: String, readPostedChapter: Boolean) = externalScope.launch {
        syncQueueDb.insert(SyncQueueEntity(event = SyncQueueEvent.UpdateMangaReadingStatus(mangaId = mangaId, chapterId = chapterId, readPostedChapter = readPostedChapter)))
        processQueueThrottled(Unit)
    }

    private fun processQueue() {
        if (!context.isNetworkAvailable()) return

        externalScope.launch {
            val refreshCompletionJob = CompletableFuture<Unit>()
            appEventsRepository.postEvent(AuthenticationEvent.RefreshToken(completionJob = refreshCompletionJob))
            refreshCompletionJob.await()

            val token = appData.getToken()
            if (token == null) {
                Clog.i("Failed to refresh token or has not logged in previously")
                return@launch
            }

            val syncItems = syncQueueDb.getAllSync()
            for (item in syncItems) {
                val success = when (item.event) {
                    is SyncQueueEvent.ChangeRating -> {
                        val event = item.event as SyncQueueEvent.ChangeRating
                        changeRating(event.mangaId, event.rating)
                    }
                    is SyncQueueEvent.ChangeSeriesReadingStatus -> {
                        val event = item.event as SyncQueueEvent.ChangeSeriesReadingStatus
                        changeReadingStatus(event.mangaId, event.readingStatus)
                    }
                    is SyncQueueEvent.MarkRead -> {
                        val event = item.event as SyncQueueEvent.MarkRead
                        markRead(event.mangaId, event.chapterId, event.read)
                    }
                    is SyncQueueEvent.UpdateMangaReadingStatus -> {
                        val event = item.event as SyncQueueEvent.UpdateMangaReadingStatus
                        updateReadingStatus(event.mangaId, event.chapterId, event.readPostedChapter)
                    }
                }
                if (success) {
                    syncQueueDb.delete(item)
                } else {
                    syncQueueDb.update(item.copy(retryCount = item.retryCount + 1))
                }
            }
        }
    }

    private suspend fun updateReadingStatus(
        mangaId: String,
        chapterId: String,
        readPostedChapter: Boolean
    ): Boolean {
        if (!context.isNetworkAvailable()) { return false }
        val chapter = chapterDb.getChapterForId(chapterId)
        val readingStatus = mangaService.getSeriesReadingStatus(mangaId) ?: return false
        when (readingStatus) {
            ReadingStatus.ReReading,
            ReadingStatus.Reading,
                -> {
                internalMarkSeriesComplete(chapter, readPostedChapter)
            }

            ReadingStatus.OnHold -> {
                internalMarkSeriesReading(chapter, readPostedChapter)
                internalMarkSeriesComplete(chapter, readPostedChapter)
            }

            ReadingStatus.Completed,
            ReadingStatus.PlanToRead,
            ReadingStatus.Dropped -> {
                // no-op
            }
        }
        return true
    }

    private suspend fun changeRating(mangaId: String, rating: Int): Boolean {
        if (!context.isNetworkAvailable()) return false
        val manga = mangaDb.getMangaById(mangaId) ?: return false
        mangaDb.update(manga.copy(rating = rating))
        return ratingService.setRating(mangaId, rating)
    }

    private suspend fun changeReadingStatus(mangaId: String, readingStatus: String): Boolean {
        if (!context.isNetworkAvailable()) return false
        return mangaService.changeSeriesReadingStatus(mangaId, ReadingStatus.from(readingStatus)!!)
    }

    private suspend fun markRead(mangaId: String, chapterId: String, read: Boolean): Boolean {
        if (!context.isNetworkAvailable()) return false
        return mangaService.changeReadStatus(
            mangaId = mangaId,
            chapterId = chapterId,
            readStatus = read
        )
    }

    private suspend fun internalMarkSeriesReading(chapter: ChapterEntity, read: Boolean) {
        if (!read) return
        if (appData.autoMarkMangaReading.firstOrNull() != true) return
        appEventsRepository.postEvent(SystemLogicEvents.ChangeMangaReadingStatus(chapter.mangaId, ReadingStatus.Reading.serialized()))
    }

    private suspend fun internalMarkSeriesComplete(chapter: ChapterEntity, read: Boolean) {
        if (!read) return
        if (appData.autoMarkMangaCompleted.firstOrNull() != true) return

        val manga = mangaDb.getMangaById(chapter.mangaId)
        if (manga == null) {
            Clog.e("ReadStatus.internalMarkSeriesComplete: manga not found", NullPointerException("ReadStatus.internalMarkSeriesComplete: manga not found"))
            return
        }

        if (manga.lastChapter != chapter.chapter) return

        appEventsRepository.postEvent(SystemLogicEvents.ChangeMangaReadingStatus(chapter.mangaId, ReadingStatus.Completed.serialized()))
        appEventsRepository.postEvent(SystemLogicEvents.PromptMangaRating(chapter.mangaId))
    }
}