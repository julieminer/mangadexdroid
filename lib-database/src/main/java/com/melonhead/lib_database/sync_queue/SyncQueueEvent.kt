package com.melonhead.lib_database.sync_queue

import kotlinx.serialization.Serializable

@Serializable
sealed class SyncQueueEvent {
    @Serializable
    data class MarkRead(val mangaId: String, val chapterId: String, val read: Boolean) : SyncQueueEvent()
    @Serializable
    data class ChangeRating(val mangaId: String, val rating: Int) : SyncQueueEvent()
    @Serializable
    data class ChangeSeriesReadingStatus(val mangaId: String, val readingStatus: String) : SyncQueueEvent()
    @Serializable
    data class UpdateMangaReadingStatus(val mangaId: String, val chapterId: String, val readPostedChapter: Boolean) : SyncQueueEvent()
}