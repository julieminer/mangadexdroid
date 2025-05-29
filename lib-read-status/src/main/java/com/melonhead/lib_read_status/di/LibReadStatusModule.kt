package com.melonhead.lib_read_status.di

import com.melonhead.lib_chapter_cache.ReadStatus
import com.melonhead.lib_chapter_cache.ReadStatusImpl
import com.melonhead.lib_database.di.LibDbModule
import org.koin.dsl.module

val LibReadStatusModule = module {
    includes(LibDbModule)
    single<ReadStatus> {
        ReadStatusImpl()
    }
}
