package com.melonhead.lib_database.sync_queue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SyncQueueDao.TABLE_NAME)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "event") val event: SyncQueueEvent,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
)