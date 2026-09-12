package com.melonhead.lib_sync_queue

import android.content.Context
import com.melonhead.data_manga.services.MangaService
import com.melonhead.data_rating.services.RatingService
import com.melonhead.data_shared.models.ui.MangaRefreshStatus
import com.melonhead.data_user.services.UserService
import com.melonhead.lib_app_data.AppData
import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.events.AppEvent
import com.melonhead.lib_app_events.events.AppLifecycleEvent
import com.melonhead.lib_app_events.events.AuthenticationEvent
import com.melonhead.lib_app_events.events.UserEvent
import com.melonhead.lib_core.extensions.throttleLatest
import com.melonhead.lib_database.chapter.ChapterDao
import com.melonhead.lib_database.chapter.ChapterEntity
import com.melonhead.lib_database.extensions.from
import com.melonhead.lib_database.manga.MangaDao
import com.melonhead.lib_database.manga.MangaEntity
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_core.extensions.isNetworkAvailable
import com.melonhead.lib_read_status.ReadStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.concurrent.CompletableFuture

interface ReadSyncRepository {
    val refreshStatus: Flow<MangaRefreshStatus>
    suspend fun pullManga(refreshCompletable: (CompletableFuture<Unit>)?)
}

internal class ReadSyncRepositoryImpl(
    private val context: Context,
    externalScope: CoroutineScope,
    private val appData: AppData,
    private val userService: UserService,
    private val mangaService: MangaService,
    private val ratingService: RatingService,

    private val mangaDb: MangaDao,
    private val chapterDb: ChapterDao,

    private val readStatusRepository: ReadStatusRepository,
    private val appEventsRepository: AppEventsRepository,
): ReadSyncRepository {
    private val scope = externalScope + SupervisorJob()

    private val pullMangaThrottled: (AppEvent) -> Unit = throttleLatest(300L, scope) { event ->
        scope.launch { pullManga((event as? UserEvent.RefreshManga)?.completionJob) }
    }

    private val mutableRefreshStatus = MutableStateFlow<MangaRefreshStatus>(MangaRefreshStatus.None)
    override val refreshStatus = mutableRefreshStatus.shareIn(scope, replay = 0, started = SharingStarted.WhileSubscribed())

    private var hasLaunched = false

    init {
        Clog.i("ReadSyncRepository init")
        scope.launch {
            // refresh manga on login
            try {
                // TODO: it's easy to miss necessary events with this pattern, it would be better to include a way to pass in the list of expected events
                appEventsRepository.events.collectLatest { event ->
                    launch {
                        when (event) {
                            is AuthenticationEvent.LoggedIn -> {
                                Clog.i("Refresh: Logged in")
                                pullMangaThrottled(event)
                            }
                            is AppLifecycleEvent.AppForegrounded -> {
                                if (hasLaunched) {
                                    Clog.i("Refresh: Foregrounded")
                                    pullMangaThrottled(event)
                                } else {
                                    hasLaunched = true
                                }
                            }
                            is UserEvent.RefreshManga -> {
                                Clog.i("Refresh: Refresh event")
                                pullMangaThrottled(event)
                            }
                            is AppLifecycleEvent.ConnectionChanged -> {
                                if (event.connected) {
                                    Clog.i("Refresh: Connected to internet")
                                    pullMangaThrottled(event)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // begin a refresh as soon as the app is started
        pullMangaThrottled(AppLifecycleEvent.AppForegrounded)
    }

    override suspend fun pullManga(refreshCompletable: (CompletableFuture<Unit>)?) {
        if (!context.isNetworkAvailable()) {
            refreshCompletable?.complete(Unit)
            return
        }

        // TODO purge DB of garbage items
//        val deleteChapters = mutableListOf<ChapterEntity>()
//        for (entity in chapterDb.getAllSync()) {
//            if (entity.externalUrl?.contains("global.manga-up") == true) {
//                deleteChapters.add(entity)
//            }
//        }
//        if (deleteChapters.isNotEmpty()) {
//            chapterDb.delete(deleteChapters)
//        }

        // refresh token
        val refreshCompletionJob = CompletableFuture<Unit>()
        appEventsRepository.postEvent(AuthenticationEvent.RefreshToken(completionJob = refreshCompletionJob))
        refreshCompletionJob.await()

        val token = appData.getToken()
        if (token == null) {
            Clog.i("Failed to refresh token or has not logged in previously")
            return
        }

        Clog.i("refreshManga")

        mutableRefreshStatus.value = MangaRefreshStatus.Following

        // get all followed chapters
        val followedChaptersResponse = userService.getFollowedChapters()

        // TODO add ability to block external urls or scanlation groups
        val filteredChaptersResponse = followedChaptersResponse.filter {
            it.attributes.externalUrl?.contains("global.manga-up") ?: true
        }

        // map chapters into the manga ids
        val mangaIdsFromChapters = filteredChaptersResponse.mapNotNull { chapters -> chapters.relationships?.firstOrNull { it.type == "manga" }?.id }.toSet()

        Clog.i("Refreshing manga series count: ${mangaIdsFromChapters.count()}")

        // fetch manga series info
        if (mangaIdsFromChapters.isNotEmpty()) {
            mutableRefreshStatus.value = MangaRefreshStatus.MangaSeries

            val mangaSeries = mangaService.getManga(mangaIdsFromChapters.toList())
            val ratings = ratingService.getRatings(mangaIdsFromChapters.toList())

            val manga = mangaSeries.map {
                // TODO: grab reading status

                // grab the chosen title from the DB
                MangaEntity.from(it, mangaDb.getMangaByIdAsync(it.id).first()?.chosenTitle, ratings[it.id])
            }

            // insert new series into local db
            mangaDb.insertAll(*manga.toTypedArray())
        }

        // convert chapters to DB format
        val chapterEntities = filteredChaptersResponse.map { ChapterEntity.from(it) }

        // find the new chapters
        val newChaptersEntities = chapterEntities.filter { !chapterDb.containsChapter(it.id) }

        Clog.i("New chapters: ${newChaptersEntities.count()}")

        // if there are new chapters
        if (newChaptersEntities.isNotEmpty()) {

            // add chapters to DB
            chapterDb.insertAll(*newChaptersEntities.toTypedArray())
        }

        mutableRefreshStatus.value = MangaRefreshStatus.ReadStatus

        val manga = mangaDb.getAllSync()
        val chapters = chapterDb.getAllSync()
        readStatusRepository.refresh(manga, chapters)

        mutableRefreshStatus.value = MangaRefreshStatus.None
        appData.updateLastRefreshDate()

        // mark refresh as completed
        refreshCompletable?.complete(Unit)
    }
}