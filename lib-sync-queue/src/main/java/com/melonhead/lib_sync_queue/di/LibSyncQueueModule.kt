package com.melonhead.lib_sync_queue.di

import com.melonhead.data_manga.di.DataMangaModule
import com.melonhead.data_rating.di.DataRatingModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_sync_queue.SyncQueue
import com.melonhead.lib_sync_queue.SyncQueueImpl
import org.koin.dsl.module

val LibSyncQueueModule = module {
    includes(LibDbModule)
    includes(LibAppDataModule)
    includes(LibAppEventsModule)
    includes(DataMangaModule)
    includes(DataRatingModule)
    single<SyncQueue>(createdAtStart = true) {
        SyncQueueImpl(get(), get(), get(), get(), get())
    }
}
