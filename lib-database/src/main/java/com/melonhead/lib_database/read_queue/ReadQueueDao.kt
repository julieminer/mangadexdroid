package com.melonhead.lib_database.read_queue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadQueueDao {
    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<ReadQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg readQueueEntity: ReadQueueEntity)

    @Delete
    suspend fun delete(vararg readQueueEntity: ReadQueueEntity)

    @Update
    suspend fun update(vararg readQueueEntity: ReadQueueEntity)

    companion object {
        const val TABLE_NAME = "read_queue"
    }
}