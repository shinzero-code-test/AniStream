package com.exapps.anistream.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeFeed(
    val featuredEpisodes: List<EpisodeCard> = emptyList(),
    val latestEpisodes: List<EpisodeCard> = emptyList(),
    val latestTitles: List<AnimeCard> = emptyList(),
)

@Immutable
data class PaginatedTitles(
    val items: List<AnimeCard> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
)

@Immutable
data class CatalogFilters(
    val sort: CatalogSort = CatalogSort.ADDITION_DATE,
    val direction: SortDirection = SortDirection.DESC,
    val season: String? = null,
    val year: Int? = null,
    val genreSlug: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
)

enum class CatalogSort(val wireValue: String) {
    ADDITION_DATE("addition_date"),
    NAME("name"),
    RELEASE_DATE("release_date"),
    RATE("rate"),
}

enum class SortDirection(val wireValue: String) {
    ASC("asc"),
    DESC("desc"),
}

@Immutable
data class AnimeCard(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val detailsUrl: String,
    val synopsis: String? = null,
    val rating: String? = null,
    val episodeCount: String? = null,
    val seasonLabel: String? = null,
    val genres: List<String> = emptyList(),
)

@Immutable
data class EpisodeCard(
    val titleSlug: String,
    val episodeNumber: Int,
    val animeTitle: String,
    val posterUrl: String,
    val episodeUrl: String,
    val episodeLabel: String,
    val episodeTitle: String? = null,
)

@Immutable
data class AnimeDetails(
    val slug: String,
    val title: String,
    val typeLabel: String? = null,
    val posterUrl: String,
    val status: String? = null,
    val releaseSeason: String? = null,
    val studio: String? = null,
    val author: String? = null,
    val ageRating: String? = null,
    val score: String? = null,
    val episodeCount: Int? = null,
    val synopsis: String = "",
    val summary: String? = null,
    val alternateTitles: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
    val trailers: List<TrailerItem> = emptyList(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val relatedTitles: List<AnimeCard> = emptyList(),
)

@Immutable
data class EpisodeItem(
    val titleSlug: String,
    val episodeNumber: Int,
    val title: String? = null,
    val duration: String? = null,
    val posterUrl: String,
    val episodeUrl: String,
)

@Immutable
data class VideoSource(
    val id: String,
    val label: String,
    val url: String,
    val type: StreamType,
    val serverId: String? = null,
)

@Immutable
data class DownloadLink(
    val qualityLabel: String,
    val url: String,
)

@Immutable
data class ExternalLink(
    val label: String,
    val url: String,
)

@Immutable
data class TrailerItem(
    val title: String,
    val embedUrl: String,
)

@Immutable
data class EpisodeStream(
    val titleSlug: String,
    val animeTitle: String,
    val posterUrl: String,
    val episodeNumber: Int,
    val episodeTitle: String? = null,
    val refererUrl: String,
    val iframeUrl: String? = null,
    val playbackUrl: String? = null,
    val availableSources: List<VideoSource> = emptyList(),
    val downloadLinks: List<DownloadLink> = emptyList(),
    val views: Long? = null,
    val nextEpisodeNumber: Int? = null,
    val batchDownloadUrl: String? = null,
    val csrfToken: String? = null,
    val livewireComponentId: String? = null,
    val livewireSnapshot: String? = null,
    val selectedServerId: String? = null,
    val attemptedServerIds: List<String> = emptyList(),
)

@Immutable
data class WatchlistAnime(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val synopsis: String? = null,
    val watchStatus: WatchStatus = WatchStatus.PLAN_TO_WATCH,
    val userRating: Int? = null,
    val episodeCount: Int? = null,
    val updatedAt: Long,
)

@Immutable
data class PlaybackHistory(
    val titleSlug: String,
    val animeTitle: String,
    val posterUrl: String,
    val episodeNumber: Int,
    val episodeTitle: String? = null,
    val playbackPositionMs: Long,
    val updatedAt: Long,
)

@Immutable
data class UserPreferences(
    val autoPlayNext: Boolean = true,
    val preferSummary: Boolean = false,
    val cinemaMode: Boolean = false,
    val dynamicColors: Boolean = true,
)

enum class StreamType {
    HLS,
    MP4,
    MKV,
    DOWNLOAD,
    PLAYER_PAGE,
}

enum class WatchStatus(val rawValue: String) {
    WATCHING("watching"),
    COMPLETED("completed"),
    ON_HOLD("on_hold"),
    DROPPED("dropped"),
    PLAN_TO_WATCH("plan_to_watch"),
    ;

    companion object {
        fun fromRawValue(value: String?): WatchStatus {
            return entries.firstOrNull { it.rawValue == value } ?: PLAN_TO_WATCH
        }
    }
}
