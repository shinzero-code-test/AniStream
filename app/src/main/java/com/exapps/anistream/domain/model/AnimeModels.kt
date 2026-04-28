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
)

@Immutable
data class DownloadLink(
    val qualityLabel: String,
    val url: String,
)

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
)

@Immutable
data class WatchlistAnime(
    val slug: String,
    val title: String,
    val posterUrl: String,
    val synopsis: String? = null,
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
)

enum class StreamType {
    HLS,
    MP4,
    DOWNLOAD,
    PLAYER_PAGE,
}
