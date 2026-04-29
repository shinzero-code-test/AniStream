package com.exapps.anistream.domain.usecase

import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.CatalogCategory
import com.exapps.anistream.domain.model.CatalogFilters
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.UserPreferences
import com.exapps.anistream.domain.model.WatchStatus
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
    suspend operator fun invoke(
        category: CatalogCategory,
        page: Int = 1,
        filters: CatalogFilters = CatalogFilters(),
    ): PaginatedTitles = repository.getCatalog(category, page, filters)

    suspend operator fun invoke(
        page: Int = 1,
        filters: CatalogFilters = CatalogFilters(),
    ): PaginatedTitles = repository.getCatalog(page, filters)
}

class SearchAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 1,
        filters: CatalogFilters = CatalogFilters(),
    ): PaginatedTitles = repository.search(query, page, filters)
}

class GetAnimeDetailsUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(slug: String): AnimeDetails = repository.getAnimeDetails(slug)
}

class GetEpisodeStreamUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(
        titleSlug: String,
        episodeNumber: Int,
        preferredServerId: String? = null,
        excludedServerIds: Set<String> = emptySet(),
    ): EpisodeStream {
        return repository.getEpisodeStream(
            titleSlug = titleSlug,
            episodeNumber = episodeNumber,
            preferredServerId = preferredServerId,
            excludedServerIds = excludedServerIds,
        )
    }
}

class ObserveWatchlistUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(): Flow<List<WatchlistAnime>> = repository.observeWatchlist()
}

class ObserveWatchEntryUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    operator fun invoke(slug: String): Flow<WatchlistAnime?> = repository.observeWatchEntry(slug)
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

class SetWatchStatusUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(details: AnimeDetails, status: WatchStatus) {
        repository.setWatchStatus(details, status)
    }
}

class SetAnimeRatingUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(details: AnimeDetails, rating: Int?) {
        repository.setAnimeRating(details, rating)
    }
}

class ClearWatchlistUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke() = repository.clearWatchlist()
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

class ClearHistoryUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke() = repository.clearHistory()
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

class SetDynamicColorsUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColors(enabled)
}

class SetSkipIntroSecondsUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(seconds: Int) = repository.setSkipIntroSeconds(seconds)
}
