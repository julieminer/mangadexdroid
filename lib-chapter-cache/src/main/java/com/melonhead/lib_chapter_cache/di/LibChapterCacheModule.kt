package com.melonhead.lib_chapter_cache.di

import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_chapter_cache.ChapterCache
import com.melonhead.lib_chapter_cache.ChapterCacheImpl
import com.melonhead.lib_database.di.LibDbModule
import org.koin.dsl.module

val LibChapterCacheModule = module {
    includes(LibDbModule)
    includes(LibAppEventsModule)
    single<ChapterCache> {
        ChapterCacheImpl(get(), get(), get(), get(), get(), get())
    }
}
