package com.melonhead.mangadexfollower

import com.melonhead.feature_authentication.navigation.OauthLoginScreenResolver
import com.melonhead.feature_manga_list.navigation.MangaListScreenResolver
import com.melonhead.feature_native_chapter_viewer.navigation.NativeChapterViewerActivityResolver
import com.melonhead.feature_settings.navigation.SettingsActivityResolver
import com.melonhead.feature_webview_chapter_viewer.navigation.WebViewChapterViewerActivityResolver
import com.melonhead.lib_navigation.keys.ActivityKey
import com.melonhead.lib_navigation.keys.ScreenKey
import com.melonhead.lib_navigation.resolvers.ResolverMap
import com.melonhead.mangadexfollower.navigation.MainActivityResolver

class AppNavigationMap(
    resolverMap: ResolverMap,

    mainActivityResolver: MainActivityResolver,
    mangaListScreenResolver: MangaListScreenResolver,
    nativeChapterViewerActivityResolver: NativeChapterViewerActivityResolver,
    webViewActivityResolver: WebViewChapterViewerActivityResolver,
    settingsActivityResolver: SettingsActivityResolver,

    oauthLoginScreenResolver: OauthLoginScreenResolver,
) {
    init {
        // activities
        resolverMap.registerResolver(ActivityKey.WebViewActivity::class.java, webViewActivityResolver)
        resolverMap.registerResolver(ActivityKey.ChapterActivity::class.java, nativeChapterViewerActivityResolver)
        resolverMap.registerResolver(ActivityKey.MainActivity::class.java, mainActivityResolver)
        resolverMap.registerResolver(ActivityKey.SettingsActivity::class.java, settingsActivityResolver)

        // screens
        resolverMap.registerResolver(ScreenKey.OauthLoginScreen::class.java, oauthLoginScreenResolver)
        resolverMap.registerResolver(ScreenKey.MangaListScreen::class.java, mangaListScreenResolver)
    }
}
