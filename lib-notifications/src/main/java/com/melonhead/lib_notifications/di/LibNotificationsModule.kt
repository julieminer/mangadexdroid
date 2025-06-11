package com.melonhead.lib_notifications.di

import com.melonhead.lib_app_context.di.LibAppContextModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_database.di.LibDbModule
import com.melonhead.lib_navigation.di.LibNavigationModule
import com.melonhead.lib_notifications.AuthFailedNotificationChannel
import com.melonhead.lib_notifications.NewChapterNotificationChannel
import com.melonhead.lib_notifications.NotificationsRepository
import com.melonhead.lib_notifications.NotificationsRepositoryImpl
import com.melonhead.lib_read_status.di.LibReadStatusRepositoryModule
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val LibNotificationsModule = module {
    includes(LibNavigationModule)
    includes(LibAppEventsModule)
    includes(LibAppContextModule)
    includes(LibDbModule)
    includes(LibReadStatusRepositoryModule)

    singleOf(::NewChapterNotificationChannel)
    singleOf(::AuthFailedNotificationChannel)

    singleOf(::NotificationsRepositoryImpl).bind<NotificationsRepository>().withOptions { createdAtStart() }
}
