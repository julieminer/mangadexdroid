package com.melonhead.lib_database.read_queue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = ReadQueueDao.TABLE_NAME)
data class ReadQueueEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    @ColumnInfo(name = "retry_count") val retryCount: Int,
)