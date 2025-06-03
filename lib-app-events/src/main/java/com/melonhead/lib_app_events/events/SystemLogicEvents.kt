package com.melonhead.lib_app_events.events

// Internal events that are triggered by the app, rather than by direct user action
sealed class SystemLogicEvents: AppEvent {
    // Prompt users to rate the manga
    data class PromptMangaRating(val mangaId: String): SystemLogicEvents()

    // Changes the reading status for a manga series
    data class ChangeMangaReadingStatus(val mangaId: String, val readingStatus: String): SystemLogicEvents()
}
