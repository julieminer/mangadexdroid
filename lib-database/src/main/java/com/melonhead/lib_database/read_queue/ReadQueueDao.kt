package com.melonhead.lib_database.read_queue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReadQueueDao {
    @Query("SELECT * FROM $TABLE_NAME")
    suspend fun getAllSync(): List<ReadQueueEntity>

    @Query("SELECT * FROM $TABLE_NAME where chapterId = :chapterId")
    suspend fun get(chapterId: String): ReadQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(readQueueEntity: ReadQueueEntity)

    @Delete
    suspend fun deleteAll(vararg readQueueEntity: ReadQueueEntity)

    @Query("DELETE FROM $TABLE_NAME where chapterId = :chapterId")
    suspend fun delete(chapterId: String)

    @Update
    suspend fun update(vararg readQueueEntity: ReadQueueEntity)

    companion object {
        const val TABLE_NAME = "read_queue"
    }
}