package com.melonhead.lib_database.manga

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Dao
interface MangaDao {
    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): Flow<List<MangaEntity>>

    @Query("SELECT * from $TABLE_NAME")
    suspend fun getAllSync(): List<MangaEntity>

    fun allSeries() = getAll().distinctUntilChanged()

    @Query("SELECT * FROM $TABLE_NAME WHERE id IS :mangaId")
    fun getMangaByIdAsync(mangaId: String): Flow<MangaEntity?>

    fun mangaByIdAsyncDistinct(mangaId: String) = getMangaByIdAsync(mangaId).distinctUntilChanged()

    @Query("SELECT * FROM $TABLE_NAME WHERE id IS :mangaId")
    fun getMangaById(mangaId: String): MangaEntity?

    @Query("SELECT EXISTS(SELECT * FROM $TABLE_NAME WHERE id = :mangaId)")
    suspend fun containsManga(mangaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg chapters: MangaEntity)

    @Delete
    suspend fun delete(chapters: MangaEntity)

    @Update
    suspend fun update(manga: MangaEntity)

    companion object {
        const val TABLE_NAME = "manga"
    }
}
