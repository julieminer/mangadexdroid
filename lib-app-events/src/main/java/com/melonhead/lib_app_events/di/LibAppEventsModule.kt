package com.melonhead.lib_app_events.di

import com.melonhead.lib_app_events.AppEventsRepository
import com.melonhead.lib_app_events.AppEventsRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val LibAppEventsModule = module {
    singleOf(::AppEventsRepositoryImpl).bind<AppEventsRepository>()
}
