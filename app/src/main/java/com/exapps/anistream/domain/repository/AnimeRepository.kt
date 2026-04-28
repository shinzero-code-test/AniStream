package com.exapps.anistream.domain.repository

import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.UserPreferences
import com.exapps.anistream.domain.model.WatchlistAnime
import com.exapps.anistream.domain.model.EpisodeStream
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {
    suspend fun getHomeFeed(): HomeFeed
    suspend fun getCatalog(page: Int = 1): PaginatedTitles
    suspend fun search(query: String, page: Int = 1): PaginatedTitles
    suspend fun getAnimeDetails(slug: String): AnimeDetails
    suspend fun getEpisodeStream(titleSlug: String, episodeNumber: Int): EpisodeStream

    fun observeWatchlist(): Flow<List<WatchlistAnime>>
    fun observeIsWatchlisted(slug: String): Flow<Boolean>
    suspend fun toggleWatchlist(details: AnimeDetails)

    fun observeHistory(): Flow<List<PlaybackHistory>>
    suspend fun savePlaybackHistory(stream: EpisodeStream, playbackPositionMs: Long)

    fun observePreferences(): Flow<UserPreferences>
    suspend fun setPreferSummary(enabled: Boolean)
    suspend fun setCinemaMode(enabled: Boolean)
    suspend fun setAutoPlayNext(enabled: Boolean)
}
