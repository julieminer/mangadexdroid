package com.melonhead.lib_read_status

import android.content.Context
import com.melonhead.data_manga.models.ReadingStatus
import com.melonhead.data_manga.services.MangaService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.SystemLogicEvents
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_database.readmarkers.ReadMarkerDao
import com.melonhead.lib_database.readmarkers.ReadMarkerEntity
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_notifications.NewChapterNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

interface ReadStatus {
    suspend fun refresh(manga: List<MangaEntity>, chapters: List<ChapterEntity>)
    val readMarkers: Flow<List<ReadMarkerEntity>>
    fun isRead(chapter: ChapterEntity): Boolean
}

internal class ReadStatusImpl(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val readMarkerDb: ReadMarkerDao,
    private val appData: AppData,
    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,
    private val mangaService: MangaService,
    private val appEventsRepository: AppEventsRepository,
    private val newChapterNotificationChannel: NewChapterNotificationChannel,
) : ReadStatus {

    private val internalReadMarker = readMarkerDb.getAll()
    override val readMarkers: Flow<List<ReadMarkerEntity>>
        get() = internalReadMarker

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

    // TODO: some of this logic should be separate from readstatus
    override suspend fun refresh(manga: List<MangaEntity>, chapters: List<ChapterEntity>) {
        Clog.i("ReadStatus.refresh: start")

        val readMarkers = chapters.map { ReadMarkerEntity.from(it, null) }
        readMarkerDb.insertAll(*readMarkers.toTypedArray())

        val readChapters = mangaService.getReadChapters(manga.map { it.id })
        val chaptersToUpdate = chapters
            // filter out chapters already marked as read in the db
            .filter {
                val readStatus = readMarkerDb.isRead(it.mangaId, it.chapter)
                readStatus == null && readChapters.contains(it.id)
            }


        if (chaptersToUpdate.isEmpty()) {
            return
        }

        // update the db with the new entities
        chapterDb.update(*chaptersToUpdate.toTypedArray())

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
            Clog.i("ReadStatus.markChapterRead: mangaId: $mangaId, chapterId: $chapterId, read: $read")

            val chapter = chapterDb.getChapterForId(chapterId)
            val chapterTitle = chapter.chapterTitle

            if (chapterTitle != null) {
                val chapters = chapterDb.getChaptersByTitle(chapterTitle)
                chapters.forEach {
                    internalMarkChapterAsRead(it, isDuplicate = true, read = read)
                }
            } else {
                internalMarkChapterAsRead(chapter, isDuplicate = false, read = read)
            }
        }
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

    private suspend fun internalMarkChapterAsRead(chapter: ChapterEntity, isDuplicate: Boolean, read: Boolean) {
        val entity = readMarkerDb.getEntityByChapter(
            mangaId = chapter.mangaId,
            chapter = chapter.chapter
        ) ?: return

        if (read) {
            // TODO: notification event
            newChapterNotificationChannel.dismissNotification(context, chapter.mangaId, chapter.id)
        }

        readMarkerDb.update(entity.copy(readStatus = read))
        if (isDuplicate) return

        // TODO: consider using logic event or creating a lib
        val readingStatus = mangaService.getSeriesReadingStatus(chapter.mangaId) ?: return
        when (readingStatus) {
            ReadingStatus.ReReading,
            ReadingStatus.Reading,
                -> {
                internalMarkSeriesComplete(chapter, read)
            }

            ReadingStatus.OnHold -> {
                internalMarkSeriesReading(chapter, read)
                internalMarkSeriesComplete(chapter, read)
            }

            ReadingStatus.Completed,
            ReadingStatus.PlanToRead,
            ReadingStatus.Dropped -> {
                // no-op
            }
        }
    }

}
