package com.melonhead.lib_database.sync_queue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAllSync(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncQueueEntity: SyncQueueEntity)

    @Delete
    suspend fun delete(syncQueueEntity: SyncQueueEntity)

    @Update
    suspend fun update(vararg syncQueueEntity: SyncQueueEntity)

    companion object {
        const val TABLE_NAME = "sync_queue"
    }
}