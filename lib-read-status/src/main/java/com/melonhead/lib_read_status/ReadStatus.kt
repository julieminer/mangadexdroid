package com.melonhead.lib_read_status

import android.content.Context
import com.melonhead.data_manga.models.ReadingStatus
import com.melonhead.data_manga.services.MangaService
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.SystemLogicEvents
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_chapter_cache.ChapterCache
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_database.read_queue.ReadQueueDao
import com.melonhead.lib_database.read_queue.ReadQueueEntity
import com.melonhead.lib_database.readmarkers.ReadMarkerDao
import com.melonhead.lib_database.readmarkers.ReadMarkerEntity
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_notifications.NewChapterNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.melonhead.lib_app_data.AppData

interface ReadStatus {
    suspend fun refresh(manga: List<MangaEntity>, chapters: List<ChapterEntity>)
    val readMarkers: Flow<List<ReadQueueEntity>>
    fun isRead(chapter: ChapterEntity): Boolean
}

internal class ReadStatusImpl(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val readStatusDb: ReadQueueDao,
    private val readMarkerDb: ReadMarkerDao,
    private val appData: AppData,
    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,
    private val mangaService: MangaService,
    private val appEventsRepository: AppEventsRepository,
    private val newChapterNotificationChannel: NewChapterNotificationChannel,
    private val chapterCache: ChapterCache,
) : ReadStatus {

    private val internalReadMarker = readStatusDb.getAll()
    override val readMarkers: Flow<List<ReadQueueEntity>>
        get() = internalReadMarker

    init {
        Clog.i("MangaRepository init")
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

    override suspend fun refresh(manga: List<MangaEntity>, chapters: List<ChapterEntity>) {
        Clog.i("refresh")

        val readMarkers = chapters.map { ReadMarkerEntity.from(it, null) }
        readMarkerDb.insertAll(*readMarkers.toTypedArray())

        val readChapters = mangaService.getReadChapters(manga.map { it.id })
        val chaptersToUpdate = chapters
            // filter out chapters already marked as read in the db
            .filter {
                val readStatus = readMarkerDb.isRead(it.mangaId, it.chapter)
                readStatus == null && readChapters.contains(it.id)
            }

        val readMarkersToUpdate = chaptersToUpdate
            .filter {
                val readStatus = readMarkerDb.isRead(it.mangaId, it.chapter)
                readStatus == null
            }
            .map { ReadMarkerEntity.from(it, true) }
        readMarkerDb.update(*readMarkersToUpdate.toTypedArray())
    }

    override fun isRead(chapter: ChapterEntity): Boolean {
        return readMarkerDb.isRead(chapter.mangaId, chapter.chapter) == true
    }

    private fun markChapterRead(mangaId: String, chapterId: String, read: Boolean) {
        externalScope.launch {
            val manga = mangaDb.getMangaById(mangaId)

            suspend fun internalMarkChapterAsRead(chapter: ChapterEntity, isDuplicate: Boolean) {
                val entity = readMarkerDb.getEntityByChapter(
                    mangaId = mangaId,
                    chapter = chapter.chapter
                ) ?: return

                // TODO: notification event

                if (read) {
                    newChapterNotificationChannel.dismissNotification(context, mangaId, chapterId)
                }

                readMarkerDb.update(entity.copy(readStatus = read))
                mangaService.changeReadStatus(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    readStatus = read
                )

                if (isDuplicate) return

                // TODO: consider using logic event

                val readingStatus = mangaService.getSeriesReadingStatus(mangaId) ?: return
                when (readingStatus) {
                    ReadingStatus.ReReading,
                    ReadingStatus.Reading,
                        -> {
                        if (read && manga?.lastChapter == chapter.chapter && appData.autoMarkMangaCompleted.firstOrNull() == true) {
                            mangaService.changeSeriesReadingStatus(mangaId, ReadingStatus.Completed)
                            appEventsRepository.postEvent(
                                SystemLogicEvents.PromptMangaRating(
                                    mangaId
                                )
                            )
                        }
                    }

                    ReadingStatus.OnHold -> {
                        if (read && appData.autoMarkMangaReading.firstOrNull() == true) {
                            mangaService.changeSeriesReadingStatus(mangaId, ReadingStatus.Reading)
                        }
                    }

                    ReadingStatus.Completed,
                    ReadingStatus.PlanToRead,
                    ReadingStatus.Dropped -> {
                        // no-op
                    }
                }
            }

            val chapter = chapterDb.getChapterForId(chapterId)
            val chapterTitle = chapter.chapterTitle

            if (chapterTitle != null) {
                val chapters = chapterDb.getChaptersByTitle(chapterTitle)
                chapters.forEach {
                    internalMarkChapterAsRead(it, isDuplicate = true)
                }
            } else {
                internalMarkChapterAsRead(chapter, isDuplicate = false)
            }
        }
    }
}
