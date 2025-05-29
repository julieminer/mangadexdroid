package com.melonhead.lib_database.read_queue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = ReadQueueDao.TABLE_NAME)
data class ReadQueueEntity(
    @PrimaryKey(autoGenerate = false) val chapterId: String,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "retry_count") val retryCount: Int,
)