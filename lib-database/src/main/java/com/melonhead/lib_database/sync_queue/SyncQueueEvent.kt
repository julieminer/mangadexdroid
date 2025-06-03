package com.melonhead.lib_database.sync_queue

import kotlinx.serialization.Serializable

@Serializable
sealed class SyncQueueEvent {
    @Serializable
    data class MarkRead(val chapterId: String, val read: Boolean) : SyncQueueEvent()
    @Serializable
    data class ChangeRating(val mangaId: String, val rating: Int) : SyncQueueEvent()
    @Serializable
    data class ChangeSeriesReadingStatus(val mangaId: String, val reading: Int) : SyncQueueEvent()
}