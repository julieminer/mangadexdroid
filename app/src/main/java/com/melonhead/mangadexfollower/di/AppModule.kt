package com.melonhead.mangadexfollower.di

import com.melonhead.feature_authentication.di.FeatureAuthenticationModule
import com.melonhead.feature_manga_list.di.FeatureMangaListModule
import com.melonhead.feature_native_chapter_viewer.di.FeatureNativeChapterViewerModule
import com.melonhead.feature_settings.di.FeatureSettingsModule
import com.melonhead.feature_webview_chapter_viewer.di.FeatureWebViewChapterViewerModule
import com.melonhead.lib_app_context.di.LibAppContextModule
import com.melonhead.lib_app_data.di.LibAppDataModule
import com.melonhead.lib_app_events.di.LibAppEventsModule
import com.melonhead.lib_navigation.di.LibNavigationModule
import com.melonhead.lib_notifications.di.LibNotificationsModule
import com.melonhead.lib_read_status.di.LibReadStatusRepositoryModule
import com.melonhead.lib_sync_queue.di.LibWriteSyncRepositoryModule
import com.melonhead.mangadexfollower.AppNavigationMap
import com.melonhead.mangadexfollower.navigation.MainActivityResolver
import com.melonhead.mangadexfollower.ui.viewmodels.MainViewModel
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val AppModule = module {
    includes(LibAppEventsModule)
    includes(LibNavigationModule)
    includes(LibNotificationsModule)
    includes(LibAppContextModule)
    includes(LibAppDataModule)
    includes(LibWriteSyncRepositoryModule)
    includes(LibReadStatusRepositoryModule)

    includes(FeatureAuthenticationModule)
    includes(FeatureMangaListModule)
    includes(FeatureNativeChapterViewerModule)
    includes(FeatureWebViewChapterViewerModule)
    includes(FeatureSettingsModule)

    viewModelOf(::MainViewModel)

    singleOf(::MainActivityResolver).withOptions { createdAtStart() }
    singleOf(::AppNavigationMap).withOptions { createdAtStart() }
}
