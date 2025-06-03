package com.melonhead.lib_app_events.events

sealed class SystemLogicEvents: AppEvent {
    data class PromptMangaRating(val mangaId: String): SystemLogicEvents()
    data class ChangeMangaReadingStatus(val mangaId: String, val readingStatus: String): SystemLogicEvents()
}
