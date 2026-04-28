package com.exapps.anistream.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistAnimeEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val posterUrl: String,
    val synopsis: String?,
    val updatedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val titleSlug: String,
    val animeTitle: String,
    val posterUrl: String,
    val episodeNumber: Int,
    val episodeTitle: String?,
    val playbackPositionMs: Long,
    val updatedAt: Long,
)
