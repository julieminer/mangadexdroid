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
import org.koin.dsl.module

val LibNotificationsModule = module {
    includes(LibNavigationModule)
    includes(LibAppEventsModule)
    includes(LibAppContextModule)
    includes(LibDbModule)
    includes(LibReadStatusRepositoryModule)

    single { NewChapterNotificationChannel(get()) }
    single { AuthFailedNotificationChannel(get()) }

    single<NotificationsRepository>(createdAtStart = true) { NotificationsRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
