package com.melonhead.lib_chapter_cache.di

import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_chapter_cache.ChapterCacheRepository
import com.melonhead.lib_chapter_cache.ChapterCacheRepositoryImpl
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_read_status.di.LibReadStatusRepositoryModule
import org.koin.dsl.module

val LibChapterCacheRepositoryModule = module {
    includes(LibDbModule)
    includes(LibAppEventsModule)
    includes(LibReadStatusRepositoryModule)
    single<ChapterCacheRepository> {
        ChapterCacheRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
