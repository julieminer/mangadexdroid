package com.melonhead.lib_read_status.di

import com.melonhead.data_manga.di.DataMangaModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_read_status.ReadStatus
import com.melonhead.lib_read_status.ReadStatusImpl
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_notifications.di.LibNotificationsModule
import org.koin.dsl.module

val LibReadStatusModule = module {
    includes(LibDbModule)
    includes(LibAppDataModule)
    includes(LibNotificationsModule)
    includes(DataMangaModule)
    single<ReadStatus>(createdAtStart = true) {
        ReadStatusImpl(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
