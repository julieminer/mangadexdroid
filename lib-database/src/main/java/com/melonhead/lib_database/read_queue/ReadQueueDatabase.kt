package com.melonhead.lib_database.read_queue

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReadQueueEntity::class],
    version = 1
)
internal abstract class ReadQueueDatabase: RoomDatabase() {
    abstract fun readQueueDao(): ReadQueueDao
}