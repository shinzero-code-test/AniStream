package com.exapps.anistream.data.repository

import com.exapps.anistream.data.local.HistoryDao
import com.exapps.anistream.data.local.HistoryEntity
import com.exapps.anistream.data.local.WatchlistAnimeEntity
import com.exapps.anistream.data.local.WatchlistDao
import com.exapps.anistream.data.preferences.UserPreferencesDataSource
import com.exapps.anistream.data.scraper.AnimeExtractor
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.WatchlistAnime
import com.exapps.anistream.domain.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepositoryImpl @Inject constructor(
    private val extractor: AnimeExtractor,
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao,
    private val preferencesDataSource: UserPreferencesDataSource,
) : AnimeRepository {

    override suspend fun getHomeFeed(): HomeFeed = extractor.getHomeFeed()

    override suspend fun getCatalog(page: Int): PaginatedTitles = extractor.getCatalog(page)

    override suspend fun search(query: String, page: Int): PaginatedTitles = extractor.search(query, page)

    override suspend fun getAnimeDetails(slug: String): AnimeDetails = extractor.getAnimeDetails(slug)

    override suspend fun getEpisodeStream(titleSlug: String, episodeNumber: Int): EpisodeStream {
        return extractor.getEpisodeStream(titleSlug, episodeNumber)
    }

    override fun observeWatchlist(): Flow<List<WatchlistAnime>> {
        return watchlistDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeIsWatchlisted(slug: String): Flow<Boolean> {
        return watchlistDao.observeBySlug(slug).map { it != null }
    }

    override suspend fun toggleWatchlist(details: AnimeDetails) {
        if (watchlistDao.getBySlug(details.slug) != null) {
            watchlistDao.deleteBySlug(details.slug)
        } else {
            watchlistDao.upsert(
                WatchlistAnimeEntity(
                    slug = details.slug,
                    title = details.title,
                    posterUrl = details.posterUrl,
                    synopsis = details.summary ?: details.synopsis,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override fun observeHistory(): Flow<List<PlaybackHistory>> {
        return historyDao.observeAll().map { items ->
            items.map { entity ->
                PlaybackHistory(
                    titleSlug = entity.titleSlug,
                    animeTitle = entity.animeTitle,
                    posterUrl = entity.posterUrl,
                    episodeNumber = entity.episodeNumber,
                    episodeTitle = entity.episodeTitle,
                    playbackPositionMs = entity.playbackPositionMs,
                    updatedAt = entity.updatedAt,
                )
            }
        }
    }

    override suspend fun savePlaybackHistory(stream: EpisodeStream, playbackPositionMs: Long) {
        historyDao.upsert(
            HistoryEntity(
                titleSlug = stream.titleSlug,
                animeTitle = stream.animeTitle,
                posterUrl = stream.posterUrl,
                episodeNumber = stream.episodeNumber,
                episodeTitle = stream.episodeTitle,
                playbackPositionMs = playbackPositionMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override fun observePreferences() = preferencesDataSource.preferences

    override suspend fun setPreferSummary(enabled: Boolean) {
        preferencesDataSource.setPreferSummary(enabled)
    }

    override suspend fun setCinemaMode(enabled: Boolean) {
        preferencesDataSource.setCinemaMode(enabled)
    }

    override suspend fun setAutoPlayNext(enabled: Boolean) {
        preferencesDataSource.setAutoPlayNext(enabled)
    }

    private fun WatchlistAnimeEntity.toDomain(): WatchlistAnime {
        return WatchlistAnime(
            slug = slug,
            title = title,
            posterUrl = posterUrl,
            synopsis = synopsis,
            updatedAt = updatedAt,
        )
    }
}
