package com.melonhead.lib_database.sync_queue

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SyncQueueEntity::class],
    version = 1
)
@TypeConverters(SyncQueueEventTypeConverters::class)
internal abstract class SyncQueueDatabase: RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
}