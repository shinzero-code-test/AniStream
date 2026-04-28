package com.exapps.anistream.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WatchlistAnimeEntity>>

    @Query("SELECT * FROM watchlist WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<WatchlistAnimeEntity?>

    @Query("SELECT * FROM watchlist WHERE slug = :slug LIMIT 1")
    suspend fun getBySlug(slug: String): WatchlistAnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchlistAnimeEntity)

    @Query("DELETE FROM watchlist WHERE slug = :slug")
    suspend fun deleteBySlug(slug: String)
}
