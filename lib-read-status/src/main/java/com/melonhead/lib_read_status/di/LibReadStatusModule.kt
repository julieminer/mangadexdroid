package com.melonhead.lib_read_status.di

import com.melonhead.data_manga.di.DataMangaModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_read_status.ReadStatusRepository
import com.melonhead.lib_read_status.ReadStatusRepositoryImpl
import com.melonhead.lib_database.di.LibDbModule
import org.koin.dsl.module

val LibReadStatusRepositoryModule = module {
    includes(LibDbModule)
    includes(LibAppDataModule)
    includes(DataMangaModule)
    single<ReadStatusRepository>(createdAtStart = true) {
        ReadStatusRepositoryImpl(get(), get(), get(), get(), get())
    }
}
