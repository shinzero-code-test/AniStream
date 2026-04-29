package com.exapps.anistream.data.scraper

import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.core.webview.CloudflareChallengeSolver
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.CatalogFilters
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.HomeFeed
import com.exapps.anistream.domain.model.PaginatedTitles
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.model.VideoSource
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Anime3rbExtractor @Inject constructor(
    private val client: OkHttpClient,
    private val parser: Anime3rbHtmlParser,
    private val videoStreamResolver: VideoStreamResolver,
    private val autoFailoverPlaybackResolver: AutoFailoverPlaybackResolver,
    private val cloudflareChallengeSolver: CloudflareChallengeSolver,
    private val dispatchers: DispatcherProvider,
) : AnimeExtractor {

    override suspend fun getHomeFeed(): HomeFeed {
        return parser.parseHome(fetchDocument(BASE_URL))
    }

    override suspend fun getCatalog(page: Int, filters: CatalogFilters): PaginatedTitles {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("titles/list")
            .addQueryParameter("sort_by", filters.sort.wireValue)
            .addQueryParameter("sort_dir", filters.direction.wireValue)
            .apply { filters.season?.let { addQueryParameter("season", it) } }
            .apply { filters.year?.let { addQueryParameter("year", it.toString()) } }
            .apply { filters.genreSlug?.let { addQueryParameter("genres", it) } }
            .apply { filters.status?.let { addQueryParameter("status", it) } }
            .apply { filters.ageRating?.let { addQueryParameter("age", it) } }
            .apply { if (page > 1) addQueryParameter("page", page.toString()) }
            .build()
        return parser.parseCatalog(fetchDocument(url.toString()), page)
    }

    override suspend fun search(query: String, page: Int, filters: CatalogFilters): PaginatedTitles {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("sort_by", filters.sort.wireValue)
            .addQueryParameter("sort_dir", filters.direction.wireValue)
            .apply { filters.season?.let { addQueryParameter("season", it) } }
            .apply { filters.year?.let { addQueryParameter("year", it.toString()) } }
            .apply { filters.genreSlug?.let { addQueryParameter("genres", it) } }
            .apply { filters.status?.let { addQueryParameter("status", it) } }
            .apply { filters.ageRating?.let { addQueryParameter("age", it) } }
            .apply { if (page > 1) addQueryParameter("page", page.toString()) }
            .build()
        return parser.parseSearch(fetchDocument(url.toString()), page)
    }

    override suspend fun getAnimeDetails(slug: String): AnimeDetails {
        return parser.parseDetails(fetchDocument("$BASE_URL/titles/$slug"), slug)
    }

    override suspend fun getEpisodeStream(
        titleSlug: String,
        episodeNumber: Int,
        preferredServerId: String?,
        excludedServerIds: Set<String>,
    ): EpisodeStream {
        val episodeUrl = "$BASE_URL/episode/$titleSlug/$episodeNumber"
        val parsed = parser.parseEpisodePage(fetchDocument(episodeUrl), titleSlug, episodeNumber)
        val playbackAttempt = autoFailoverPlaybackResolver.resolve(
            stream = parsed,
            preferredServerId = preferredServerId,
            excludedServerIds = excludedServerIds,
        )

        val resolvedSources = playbackAttempt.playableSources.ifEmpty {
            val fallbackUrl = parsed.playbackUrl ?: parsed.iframeUrl
            if (fallbackUrl.isNullOrBlank()) {
                emptyList()
            } else {
                videoStreamResolver.resolve(fallbackUrl, episodeUrl)
            }
        }

        val mergedSources = buildList {
            addAll(resolvedSources.filter { it.type != StreamType.PLAYER_PAGE })
            addAll(parsed.availableSources)
            parsed.downloadLinks.forEachIndexed { index, download ->
                add(
                    VideoSource(
                        id = "download-$index",
                        label = download.qualityLabel,
                        url = download.url,
                        type = StreamType.DOWNLOAD,
                    ),
                )
            }
        }.distinctBy { it.url }

        val preferredPlayback = mergedSources.firstOrNull { it.type == StreamType.HLS }?.url
            ?: mergedSources.firstOrNull { it.type == StreamType.MP4 }?.url
            ?: mergedSources.firstOrNull { it.type == StreamType.MKV }?.url

        return parsed.copy(
            playbackUrl = preferredPlayback ?: playbackAttempt.playbackUrl ?: parsed.playbackUrl,
            availableSources = mergedSources,
            selectedServerId = playbackAttempt.selectedServerId ?: parsed.selectedServerId,
            attemptedServerIds = playbackAttempt.attemptedServerIds,
        )
    }

    private suspend fun fetchDocument(url: String): Document {
        return withContext(dispatchers.io) {
            val httpUrl = url.toHttpUrl()
            cloudflareChallengeSolver.ensureClearance(httpUrl)
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Request failed: ${response.code} ${response.message}" }
                Jsoup.parse(response.body?.string().orEmpty(), response.request.url.toString())
            }
        }
    }

    private companion object {
        const val BASE_URL = "https://anime3rb.com"
    }
}
