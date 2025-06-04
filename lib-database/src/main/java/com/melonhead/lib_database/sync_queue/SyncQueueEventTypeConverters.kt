package com.melonhead.lib_database.sync_queue

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
internal class SyncQueueEventTypeConverters(
    private val serializer: Json,
) {
    @TypeConverter
    fun fromSyncQueueEvent(event: SyncQueueEvent?): String? {
        return event?.let { serializer.encodeToString(SyncQueueEvent.serializer(), it) }
    }

    @TypeConverter
    fun toSyncQueueEvent(jsonString: String?): SyncQueueEvent? {
        return jsonString?.let { serializer.decodeFromString(SyncQueueEvent.serializer(), it) }
    }
}