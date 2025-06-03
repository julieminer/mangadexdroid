package com.melonhead.lib_app_events.events

import android.content.Context
import com.melonhead.data_shared.models.ui.UIChapter
import com.melonhead.data_shared.models.ui.UIManga
import java.util.concurrent.CompletableFuture

// These are events that are triggered directly from user action
sealed class UserEvent: AppEvent {
    // on refresh manga list
    data class RefreshManga(val completionJob: CompletableFuture<Unit>? = null): UserEvent()

    // on change manga reader type
    data class SetUseWebView(val mangaId: String, val useWebView: Boolean): UserEvent()

    // on mark chapter as read
    data class SetMarkChapterRead(val chapterId: String, val mangaId: String, val read: Boolean): UserEvent()

    // on blocking a chapter
    data class SetChapterBlocked(val mangaId: String, val chapterId: String, val blocked: Boolean): UserEvent()

    // on updating the local manga title
    data class UpdateChosenMangaTitle(val mangaId: String, val title: String): UserEvent()

    // on opening chapter from a notification
    // TODO: this isn't working properly
    data class OpenedNotification(val context: Context, val manga: UIManga, val chapter: UIChapter): UserEvent()

    // on setting the manga rating
    data class SetMangaRating(val mangaId: String, val rating: Int): UserEvent()
}
