package com.exapps.anistream.domain.usecase

import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.UserPreferences
import com.exapps.anistream.domain.model.WatchlistAnime
import com.exapps.anistream.domain.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeFeedUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(): HomeFeed = repository.getHomeFeed()
}

class GetCatalogUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(page: Int = 1): PaginatedTitles = repository.getCatalog(page)
}

class SearchAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(query: String, page: Int = 1): PaginatedTitles = repository.search(query, page)
}

class GetAnimeDetailsUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(slug: String): AnimeDetails = repository.getAnimeDetails(slug)
}

class GetEpisodeStreamUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(titleSlug: String, episodeNumber: Int): EpisodeStream {
        return repository.getEpisodeStream(titleSlug, episodeNumber)
    }
}

class ObserveWatchlistUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(): Flow<List<WatchlistAnime>> = repository.observeWatchlist()
}

class ObserveIsWatchlistedUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(slug: String): Flow<Boolean> = repository.observeIsWatchlisted(slug)
}

class ToggleWatchlistUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(details: AnimeDetails) = repository.toggleWatchlist(details)
}

class ObservePlaybackHistoryUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(): Flow<List<PlaybackHistory>> = repository.observeHistory()
}

class SavePlaybackHistoryUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(stream: EpisodeStream, playbackPositionMs: Long) {
        repository.savePlaybackHistory(stream, playbackPositionMs)
    }
}

class ObservePreferencesUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = repository.observePreferences()
}

class SetPreferSummaryUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setPreferSummary(enabled)
}

class SetCinemaModeUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setCinemaMode(enabled)
}

class SetAutoPlayNextUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAutoPlayNext(enabled)
}
