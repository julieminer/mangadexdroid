package com.melonhead.feature_manga_list

import android.content.Context
import com.melonhead.data_at_home.AtHomeService
import com.melonhead.data_shared.models.ui.*
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.*
import com.melonhead.lib_chapter_cache.CachingStatus
import com.melonhead.lib_read_status.ReadStatusRepository
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_chapter_cache.ChapterCacheRepository
import com.melonhead.lib_core.extensions.isNetworkAvailable
import com.melonhead.lib_sync_queue.ReadSyncRepository
import io.ktor.util.Hash.combine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal interface MangaRepository {
    val manga: Flow<List<UIManga>>
    val refreshStatus: Flow<MangaRefreshStatus>
    fun rateManga(mangaId: String, rating: Int)
    suspend fun getChapterData(mangaId: String, chapterId: String): List<String>?
    suspend fun getChapterById(mangaId: String, chapterId: String): Pair<UIManga, UIChapter>?
}

internal class MangaRepositoryImpl(
    private val context: Context,
    externalScope: CoroutineScope,
    private val appData: AppData,
    private val atHomeService: AtHomeService,
    private val chapterDb: ChapterDao,
    private val mangaDb: MangaDao,
    private val readStatusRepository: ReadStatusRepository,
    private val chapterCacheRepository: ChapterCacheRepository,

    private val appEventsRepository: AppEventsRepository,
    private val readSyncRepository: ReadSyncRepository,
): MangaRepository {
    private val scope = externalScope + SupervisorJob()

    override val refreshStatus: Flow<MangaRefreshStatus>
        get() = readSyncRepository.refreshStatus

    // combine all manga series and chapters
    override val manga = combine(mangaDb.allSeries(), chapterDb.allChapters(), readStatusRepository.readMarkers, chapterCacheRepository.cachingStatus) { dbSeries, dbChapters, _, cacheStatus ->
        generateUIManga(dbSeries, dbChapters, cacheStatus)
    }.shareIn(externalScope, replay = 1, started = SharingStarted.WhileSubscribed())
    }.shareIn(scope, replay = 1, started = SharingStarted.WhileSubscribed())

    init {
        Clog.i("MangaRepository init")
        scope.launch {
            combine(mangaDb.allSeries()) { dbSeries ->
                dbSeries
            }.collectLatest {
            }
        }

        scope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is UserEvent.SetChapterBlocked -> {
                                markChapterBlocked(event.chapterId, event.blocked)
                            }
                            is UserEvent.SetUseWebView -> {
                                setUseWebview(event.mangaId, event.useWebView)
                            }
                            is UserEvent.UpdateChosenMangaTitle -> {
                                updateChosenTitle(event.mangaId, event.title)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun generateUIManga(dbSeries: List<MangaEntity>, dbChapters: List<ChapterEntity>, cachingStatus: CachingStatus): List<UIManga> = withContext(Dispatchers.IO) {
        // map the series and chapters into UIManga, sorted from most recent to least
        val mangaJobs = mutableListOf<Deferred<UIManga?>>()
        mangaJobs.addAll(dbSeries.map { manga ->
            async {
                Clog.measure("julie: ui manga") {
                    var hasExternalChapters = false

                    val chapterJobs = mutableListOf<Deferred<UIChapter>>()

                    val chaptersToConsider = Clog.measure("julie: ui chapters to consider") {
                        dbChapters
                            .filter { !it.blockedChapter }
                            .filter { it.mangaId == manga.id }
                    }

                    chapterJobs.addAll(chaptersToConsider
                        .map { chapter ->
                            async {
                                Clog.measure("julie: ui chapter") {
                                    // TODO: check if we're calling mangadb in here
                                    val read = Clog.measure("julie: ui chapter isRead") { readStatusRepository.isRead(chapter) }

                                    // TODO: check if we're calling mangadb in here
                                    val pages = Clog.measure("julie: ui chapter pages") { chapterCacheRepository.getChapterPageCountFromCache(
                                        manga.id,
                                        chapter.id
                                    ) }

                                    hasExternalChapters = hasExternalChapters || chapter.externalUrl != null
                                    UIChapter(
                                        id = chapter.id,
                                        chapter = chapter.chapter,
                                        title = chapter.chapterTitle,
                                        createdDate = chapter.createdAt.epochSeconds,
                                        read = read,
                                        blocked = chapter.blockedChapter,
                                        isDownloadingCache = (cachingStatus as? CachingStatus.Caching)?.chapterId == chapter.id,
                                        externalUrl = chapter.externalUrl,
                                        cachedPages = pages
                                    )
                                }
                            }
                        }
                    )

                    val chapters = chapterJobs.awaitAll()

                    if (chapters.isEmpty()) return@measure null

                    Clog.d("julie: total chapters = ${chapterJobs.count()}")

                    UIManga(
                        id = manga.id,
                        manga.chosenTitle ?: "",
                        chapters = chapters,
                        manga.mangaCoverId,
                        useWebview = hasExternalChapters || manga.useWebview,
                        altTitles = manga.mangaTitles,
                        tags = manga.tags.sortedBy { it.id }.map { it.name },
                        status = manga.status,
                        contentRating = manga.contentRating,
                        lastChapter = manga.lastChapter,
                        description = manga.description,
                        rating = manga.rating,
                    )
                }
            }
        })

        val uiManga = mangaJobs.awaitAll().filterNotNull()
        if (uiManga.isEmpty()) return@withContext emptyList()

        // split into two categories, unread and read
        val hasUnread = uiManga.filter { it.chapters.any { !it.read } }
            .sortedByDescending { it.chapters.first().createdDate }
        val allRead = uiManga.filter { it.chapters.all { it.read } }
            .sortedByDescending { it.chapters.first().createdDate }

        return@withContext hasUnread + allRead
    }

    override suspend fun getChapterData(mangaId: String, chapterId: String): List<String>? {
        val chapterFiles = chapterCacheRepository.getChapterFromCache(mangaId, chapterId)
        if (chapterFiles.isNotEmpty()) return chapterFiles

        Clog.i("Chapter not found in cache: $mangaId, $chapterId")
        Clog.e("Chapter not found in cache", RuntimeException("Chapter not found in cache"))

        // if offline, return null
        if (!context.isNetworkAvailable()) {
            return null
        }

        val chapterData = atHomeService.getChapterData(chapterId)
        return if (appData.useDataSaver.getValue()) {
            chapterData?.pagesDataSaver()
        } else {
            chapterData?.pages()
        }
    }

    override fun rateManga(mangaId: String, rating: Int) {
        appEventsRepository.postEvent(UserEvent.SetMangaRating(mangaId, rating))
    }

    override suspend fun getChapterById(mangaId: String, chapterId: String): Pair<UIManga, UIChapter>? {
        val manga = manga.firstOrNull()?.firstOrNull { it.id == mangaId } ?: return null
        val chapter = manga?.chapters?.firstOrNull { it.id == chapterId } ?: return null
        return manga to chapter
    }

    private fun markChapterBlocked(chapterId: String, blocked: Boolean) {
        scope.launch {
            val chapter = chapterDb.getChapterForId(chapterId).copy(blockedChapter = blocked)
            chapterDb.update(chapter)
        }
    }

    private fun setUseWebview(mangaId: String, useWebView: Boolean) {
        scope.launch(Dispatchers.Main) {
            val entity = mangaDb.mangaByIdAsyncDistinct(mangaId).first() ?: return@launch
            mangaDb.update(entity.copy(useWebview = useWebView))
        }
    }

    private fun updateChosenTitle(mangaId: String, chosenTitle: String) {
        scope.launch(Dispatchers.Main) {
            val entity = mangaDb.mangaByIdAsyncDistinct(mangaId).first() ?: return@launch
            if (!entity.mangaTitles.contains(chosenTitle)) return@launch
            mangaDb.update(entity.copy(chosenTitle = chosenTitle))
        }
    }
}
