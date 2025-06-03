package com.melonhead.lib_database.sync_queue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAllSync(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncQueueEntity: SyncQueueEntity)

    @Delete
    suspend fun deleteAll(vararg syncQueueEntity: SyncQueueEntity)

    @Query("DELETE FROM $TABLE_NAME where id = :id")
    suspend fun delete(id: Long)

    companion object {
        const val TABLE_NAME = "sync_queue"
    }
}