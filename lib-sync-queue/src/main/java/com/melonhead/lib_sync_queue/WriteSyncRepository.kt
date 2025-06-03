package com.melonhead.lib_sync_queue

import com.melonhead.data_manga.models.ReadingStatus
import com.melonhead.data_manga.services.MangaService
import com.melonhead.data_rating.services.RatingService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.SystemLogicEvents
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_core.extensions.throttleLatest
import com.melonhead.lib_database.sync_queue.SyncQueueDao
import com.melonhead.lib_database.sync_queue.SyncQueueEntity
import com.melonhead.lib_database.sync_queue.SyncQueueEvent
import com.melonhead.lib_logging.Clog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

interface WriteSyncRepository

internal class WriteSyncRepositoryImpl(
    private val externalScope: CoroutineScope,
    private val appEventsRepository: AppEventsRepository,
    private val syncQueueDb: SyncQueueDao,
    private val ratingService: RatingService,
    private val mangaService: MangaService,
    private val appData: AppData,
) : WriteSyncRepository {
    private val processQueueThrottled: (Unit) -> Unit = throttleLatest(1000L, externalScope) { event ->
        processQueue()
    }

    init {
        Clog.i("ReadStatus.init")
        externalScope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is UserEvent.SetMarkChapterRead -> {
                                markChapterRead(event.mangaId, event.chapterId, event.read)
                            }

                            is UserEvent.SetMangaRating -> {
                                setMangaRating(event.mangaId, event.rating)
                            }

                            is SystemLogicEvents.ChangeMangaReadingStatus -> {
                                setMangaReadingStatus(event.mangaId, event.readingStatus)
                            }

                            is UserEvent.RefreshManga -> {
                                processQueue()
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

        processQueueThrottled(Unit)
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

    private fun processQueue() {
        externalScope.launch {
            val refreshCompletionJob = CompletableFuture<Unit>()
            appEventsRepository.postEvent(AuthenticationEvent.RefreshToken(completionJob = refreshCompletionJob))
            refreshCompletionJob.await()

            val token = appData.getToken()
            if (token == null) {
                Clog.i("Failed to refresh token")
                return@launch
            }

            val syncItems = syncQueueDb.getAllSync().filter { it.retryCount < 3 }
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
                }
                if (success) {
                    syncQueueDb.delete(item)
                } else {
                    syncQueueDb.update(item.copy(retryCount = item.retryCount + 1))
                }
            }
        }
    }

    private suspend fun changeRating(mangaId: String, rating: Int): Boolean {
        return ratingService.setRating(mangaId, rating)
    }

    private suspend fun changeReadingStatus(mangaId: String, readingStatus: String): Boolean {
        return mangaService.changeSeriesReadingStatus(mangaId, ReadingStatus.from(readingStatus)!!)
    }

    private suspend fun markRead(mangaId: String, chapterId: String, read: Boolean): Boolean {
        return mangaService.changeReadStatus(
            mangaId = mangaId,
            chapterId = chapterId,
            readStatus = read
        )
    }
}