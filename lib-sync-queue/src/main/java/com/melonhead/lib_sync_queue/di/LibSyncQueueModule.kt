package com.melonhead.lib_sync_queue.di

import com.melonhead.data_manga.di.DataMangaModule
import com.melonhead.data_rating.di.DataRatingModule
import com.melonhead.data_user.di.DataUserModule
import com.melonhead.lib_app_context.di.LibAppContextModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_chapter_cache.di.LibChapterCacheRepositoryModule
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_read_status.di.LibReadStatusRepositoryModule
import com.melonhead.lib_sync_queue.ReadSyncRepository
import com.melonhead.lib_sync_queue.ReadSyncRepositoryImpl
import com.melonhead.lib_sync_queue.WriteSyncRepository
import com.melonhead.lib_sync_queue.WriteSyncRepositoryImpl
import org.koin.dsl.module

val LibWriteSyncRepositoryModule = module {
    includes(LibDbModule)
    includes(LibAppContextModule)
    includes(LibAppDataModule)
    includes(LibAppEventsModule)
    includes(LibReadStatusRepositoryModule)
    includes(LibChapterCacheRepositoryModule)
    includes(DataUserModule)
    includes(DataMangaModule)
    includes(DataRatingModule)
    single<WriteSyncRepository>(createdAtStart = true) {
        WriteSyncRepositoryImpl(get(), get(), get(), get(), get(), get())
    }
    single<ReadSyncRepository>(createdAtStart = true) {
        ReadSyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
