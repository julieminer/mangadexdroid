package com.melonhead.feature_manga_list.di

import com.melonhead.data_at_home.di.DataAtHomeModule
import com.melonhead.data_manga.di.DataMangaModule
import com.melonhead.data_rating.di.DataRatingModule
import com.melonhead.data_user.di.DataUserModule
import com.melonhead.feature_manga_list.MangaRepository
import com.melonhead.feature_manga_list.MangaRepositoryImpl
import com.melonhead.feature_manga_list.navigation.MangaListScreenResolver
import com.melonhead.feature_manga_list.viewmodels.MangaListViewModel
import com.melonhead.lib_app_context.di.LibAppContextModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_chapter_cache.di.LibChapterCacheRepositoryModule
import com.melonhead.lib_read_status.di.LibReadStatusRepositoryModule
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_notifications.di.LibNotificationsModule
import com.melonhead.lib_sync_queue.di.LibWriteSyncRepositoryModule
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val FeatureMangaListModule = module {
    includes(LibAppEventsModule)
    includes(LibNotificationsModule)
    includes(LibAppContextModule)
    includes(LibDbModule)
    includes(LibChapterCacheRepositoryModule)
    includes(LibReadStatusRepositoryModule)
    includes(LibAppDataModule)
    includes(LibWriteSyncRepositoryModule)

    includes(DataUserModule)
    includes(DataAtHomeModule)
    includes(DataMangaModule)
    includes(DataRatingModule)

    singleOf(::MangaRepositoryImpl).bind<MangaRepository>().withOptions { createdAtStart() }
    singleOf(::MangaListScreenResolver).withOptions { createdAtStart() }

    viewModelOf(::MangaListViewModel)
}
