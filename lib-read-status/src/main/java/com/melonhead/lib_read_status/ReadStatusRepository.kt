package com.melonhead.lib_read_status

import com.melonhead.data_manga.services.MangaService
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.SystemLogicEvents
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_database.readmarkers.ReadMarkerDao
import com.melonhead.lib_database.readmarkers.ReadMarkerEntity
import com.melonhead.lib_logging.Clog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

interface ReadStatusRepository {
    suspend fun refresh(manga: List<MangaEntity>, chapters: List<ChapterEntity>)
    val readMarkers: Flow<List<ReadMarkerEntity>>
    fun isRead(chapter: ChapterEntity): Boolean
}

internal class ReadStatusRepositoryImpl(
    externalScope: CoroutineScope,
    private val readMarkerDb: ReadMarkerDao,
    private val chapterDb: ChapterDao,
    private val mangaService: MangaService,
    private val appEventsRepository: AppEventsRepository,
) : ReadStatusRepository {
    private val scope = externalScope + SupervisorJob()

    private val internalReadMarker = readMarkerDb.getAll().distinctUntilChanged()
    override val readMarkers: Flow<List<ReadMarkerEntity>>
        get() = internalReadMarker

    init {
        Clog.i("ReadStatus.init")
        scope.launch {
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

    // should only be called when online
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
        scope.launch {
            Clog.i("ReadStatus.markChapterRead: mangaId: $mangaId, chapterId: $chapterId, read: $read")

            val chapter = chapterDb.getChapterForId(chapterId)
            val chapterNumber = chapter.chapter

            if (chapterNumber != null) {
                val chapters = chapterDb.getChaptersForChapter(chapter.mangaId, chapterNumber)
                Clog.i("ReadStatus.markChapterRead: Marking other chapters with the same number as read chapter as read")
                chapters.forEach {
                    Clog.i("ReadStatus.markChapterRead: mangaId: ${chapter.mangaId}, chapterId: ${chapter.id}")
                    internalMarkChapterAsRead(it, isDuplicate = true, read = read)
                }
            } else {
                internalMarkChapterAsRead(chapter, isDuplicate = false, read = read)
            }
        }
    }

    private suspend fun internalMarkChapterAsRead(chapter: ChapterEntity, isDuplicate: Boolean, read: Boolean) {
        val entity = readMarkerDb.getEntityByChapter(
            mangaId = chapter.mangaId,
            chapter = chapter.chapter
        ) ?: return

        readMarkerDb.update(entity.copy(readStatus = read))
        if (isDuplicate) return

        appEventsRepository.postEvent(SystemLogicEvents.UpdateMangaReadingStatus(chapter.mangaId, chapter.id, read))
    }

}
