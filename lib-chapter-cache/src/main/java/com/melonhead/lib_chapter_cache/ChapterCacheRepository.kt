package com.melonhead.lib_chapter_cache

import android.content.Context
import com.melonhead.data_at_home.AtHomeService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_core.extensions.isNetworkAvailable
import com.melonhead.lib_core.extensions.throttleLatest
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_networking.extensions.downloadFile
import com.melonhead.lib_read_status.ReadStatusRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter

sealed class CachingStatus {
    data object None: CachingStatus()
    data object StartedCacheOperation: CachingStatus()
    data class Caching(val chapterId: String): CachingStatus()
    data object FinishedCacheOperation: CachingStatus()
}

interface ChapterCacheRepository {
    val cachingStatus: Flow<CachingStatus>
    fun getChapterFromCache(mangaId: String, chapterId: String): List<String>
    fun getChapterPageCountFromCache(mangaId: String, chapterId: String): Int?
    fun clearChapterFromCache(mangaId: String, chapterId: String)
    fun clearCacheForManga(mangaId: String)
}

internal class ChapterCacheRepositoryImpl(
    private val context: Context,
    private val appData: AppData,
    private val atHomeService: AtHomeService,
    private val appContext: Context,
    private val httpClient: HttpClient,
    private val externalScope: CoroutineScope,
    private val appEventsRepository: AppEventsRepository,

    private val chapterDb: ChapterDao,
    private val mangaDb: MangaDao,
    private val readStatusRepository: ReadStatusRepository,
) : ChapterCacheRepository {

    private val updateChapterCacheThrottled: (Pair<List<MangaEntity>, List<ChapterEntity>>) -> Unit = throttleLatest(1000L, externalScope) { pair ->
        externalScope.launch { updateChapterCache(pair.first, pair.second) }
    }

    private val mutableCachingStatus = MutableStateFlow<CachingStatus>(CachingStatus.None)
    override val cachingStatus: Flow<CachingStatus>
        get() = mutableCachingStatus

    private val cacheWriteLock = Mutex()

    init {
        Clog.i("ChapterCache init")
        externalScope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is UserEvent.SetMarkChapterRead -> {
                                if (event.read) {
                                    clearChapterFromCache(
                                        mangaId = event.mangaId,
                                        chapterId = event.chapterId
                                    )
                                }
                            }

                            is UserEvent.SetChapterBlocked -> {
                                if (event.blocked) {
                                    clearChapterFromCache(
                                        mangaId = event.mangaId,
                                        chapterId = event.chapterId
                                    )
                                }
                            }

                            is UserEvent.RefreshManga -> {
                                launch {
                                    event.completionJob?.await()
                                    updateChapterCacheThrottled(mangaDb.getAllSync() to chapterDb.getAllSync())
                                }
                            }

                            is AppLifecycleEvent.ConnectionChanged -> {
                                if (event.connected) {
                                    launch {
                                        updateChapterCacheThrottled(mangaDb.getAllSync() to chapterDb.getAllSync())
                                    }
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
            combine(mangaDb.allSeries(), chapterDb.allChapters(), readStatusRepository.readMarkers) { manga, chapters, _ ->
                manga to chapters
            }.collectLatest { event ->
                launch { updateChapterCacheThrottled(event) }
            }
        }
    }

    /* Wraps cache operation with status updates */
    private suspend fun cacheOperation(operation: suspend () -> Unit) {
        cacheWriteLock.withLock {
            mutableCachingStatus.value = CachingStatus.StartedCacheOperation
            operation()
            mutableCachingStatus.value = CachingStatus.FinishedCacheOperation
        }
    }

    private suspend fun getChapterData(chapterId: String): List<String>? {
        val chapterData = atHomeService.getChapterData(chapterId)
        return if (appData.useDataSaver.getValue()) {
            chapterData?.pagesDataSaver()
        } else {
            chapterData?.pages()
        }
    }

    override fun getChapterFromCache(mangaId: String, chapterId: String): List<String> {
        val cacheDirectory = appContext.cacheDir
        val mangaDirectory = File(cacheDirectory, mangaId)
        val chapterDirectory = File(mangaDirectory, chapterId)
        val successFiles = chapterDirectory.listFiles(FileFilter { it.extension == "pages" }) ?: emptyArray()
        return if (chapterDirectory.exists() && successFiles.size == 1) {
            chapterDirectory.listFiles( FileFilter { it.extension != "pages" } ) ?.map { it.absolutePath } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun getChapterPageCountFromCache(mangaId: String, chapterId: String): Int? {
        val cacheDirectory = appContext.cacheDir
        val mangaDirectory = File(cacheDirectory, mangaId)
        val chapterDirectory = File(mangaDirectory, chapterId)
        val successFiles = chapterDirectory.listFiles(FileFilter { it.extension == "pages" }) ?: return null
        if (successFiles.isEmpty()) return null
        return successFiles.first().nameWithoutExtension.toInt()
    }

    private fun updateChapterCache(manga: List<MangaEntity>, chapters: List<ChapterEntity>) {
        if (!context.isNetworkAvailable()) return

        externalScope.launch(Dispatchers.IO) {
            cacheOperation {
                val newChapters = chapters
                    .filter { !readStatusRepository.isRead(it) }
                    .filter { !it.blockedChapter }
                    .filter { (getChapterPageCountFromCache(it.mangaId, it.id) ?: 0) == 0 }
                cacheImagesForChapters(manga, newChapters)

                Clog.i("Finished downloading images for ${newChapters.count()} new chapters")

                val readChapters = chapters
                    .filter { readStatusRepository.isRead(it) }
                    .filter { (getChapterPageCountFromCache(it.mangaId, it.id) ?: 0) > 0 }
                clearImagesForChapters(readChapters)

                Clog.i("Finished removing images for ${readChapters.count()} chapters")
            }
        }
    }

    private suspend fun cacheImagesForChapters(manga: List<MangaEntity>, chapters: List<ChapterEntity>) {
        val cacheDirectory = appContext.cacheDir
        for (chapter in chapters) {
            mutableCachingStatus.value = CachingStatus.Caching(chapter.id)
            if (readStatusRepository.isRead(chapter)) continue
            val mangaForChapter = manga.find { it.id == chapter.mangaId } ?: continue
            if (mangaForChapter.useWebview) continue
            val mangaDirectory = File(cacheDirectory, mangaForChapter.id)

            if (!mangaDirectory.exists()) {
                mangaDirectory.mkdir()
            }

            val chapterDirectory = File(mangaDirectory, chapter.id)
            if (!chapterDirectory.exists()) {
                chapterDirectory.mkdir()
            }

            if (chapterDirectory.listFiles(FileFilter { it.extension == "pages" })?.isNotEmpty() == true) continue
            val chapterData = getChapterData(chapter.id)
            if (chapterData.isNullOrEmpty()) continue

            Clog.i("Caching images for manga ${mangaForChapter.chosenTitle} chapter ${chapter.chapterTitle}")

            val oldFiles = chapterDirectory.listFiles() ?: arrayOf()
            if (oldFiles.none { it.extension == "pages" } && oldFiles.count() != chapterData.count()) {
                Clog.w("Found bad file count for ${mangaForChapter.chosenTitle} chapter ${chapter.chapterTitle}")
                for (file in chapterDirectory.listFiles()!!) {
                    file.delete()
                }
            }

            val chapterTitle = chapter.chapterTitle ?: chapter.chapter
            Clog.i("Downloading images to cache for ${mangaForChapter.chosenTitle} chapter $chapterTitle")
            val jobsList = mutableListOf<Deferred<Boolean>>()
            withContext(Dispatchers.IO) {
                for ((i, page) in chapterData.withIndex()) {
                    Clog.i("Downloading page $i for ${mangaForChapter.chosenTitle} chapter $chapterTitle - $page")
                    val fileExtension = page.substringAfterLast(".")
                    val pageFile = File(chapterDirectory, "$i.$fileExtension")
                    if (pageFile.exists()) {
                        pageFile.delete()
                    } else {
                        pageFile.createNewFile()
                    }
                    val downloadJob = async {
                        try {
                            val result = httpClient.downloadFile(pageFile, page)
                            if (result) {
                                Clog.i("Finished downloading page $i for ${mangaForChapter.chosenTitle} chapter $chapterTitle - $page")
                                true
                            } else {
                                pageFile.delete()
                                Clog.w("Failed to download page $i for ${mangaForChapter.chosenTitle} chapter ${chapter.chapterTitle} - $page")
                                false
                            }
                        } catch (e: Exception) {
                            pageFile.delete()
                            Clog.w("Error downloading page $i for ${mangaForChapter.chosenTitle} chapter ${chapter.chapterTitle} - $page")
                            Clog.e("Error downloading page", e)
                            false
                        }
                    }
                    jobsList.add(downloadJob)
                }
            }
            if (jobsList.awaitAll().any { false } || jobsList.size != chapterData.count()) {
                chapterDirectory.deleteRecursively()
                Clog.w("Failed to download images to cache for ${mangaForChapter.chosenTitle} chapter ${chapter.chapterTitle}")
                continue
            }

            val newFiles = chapterDirectory.listFiles() ?: continue
            if (newFiles.count() == chapterData.count()) {
                val successFile = File(chapterDirectory, "${chapterData.count()}.pages")
                successFile.createNewFile()
                Clog.i("Finished downloading images to cache for ${mangaForChapter.chosenTitle} chapter $chapterTitle")
            }
        }
    }

    private fun clearImagesForChapters(chapters: List<ChapterEntity>) {
        for (chapter in chapters) {
            clearChapterFromCacheInternal(chapter.mangaId, chapter.id)
        }
    }

    private fun clearChapterFromCacheInternal(mangaId: String, chapterId: String) {
        try {
            Clog.i("Clearing cache for manga $mangaId chapter $chapterId")
            val cacheDirectory = appContext.cacheDir
            val mangaDirectory = File(cacheDirectory, mangaId)
            val chapterDirectory = File(mangaDirectory, chapterId)
            if (chapterDirectory.exists()) {
                val result = chapterDirectory.deleteRecursively()
                Clog.i("Cleared cache for manga $mangaId chapter $chapterId - $result")
            }
        } catch (e: Exception) {
            Clog.w("Error clearing cache for manga $mangaId chapter $chapterId")
            Clog.e("Error clearing cache for manga", e)
        }
    }

    override fun clearChapterFromCache(mangaId: String, chapterId: String) {
        externalScope.launch {
            cacheOperation {
                clearChapterFromCacheInternal(mangaId, chapterId)
            }
        }
    }

    override fun clearCacheForManga(mangaId: String) {
        externalScope.launch  {
            cacheOperation {
                try {
                    Clog.i("Clearing cache for manga $mangaId")
                    val cacheDirectory = appContext.cacheDir
                    val mangaDirectory = File(cacheDirectory, mangaId)
                    if (mangaDirectory.exists()) {
                        val result = mangaDirectory.deleteRecursively()
                        Clog.i("Cleared cache for manga $mangaId - $result")
                    }
                } catch (e: Exception) {
                    Clog.w("Error clearing cache for manga $mangaId")
                    Clog.e("Error clearing cache for manga", e)
                }
            }
        }
    }
}
