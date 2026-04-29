package com.exapps.anistream.data.scraper

import android.util.Base64
import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.core.network.BrowserHeaders
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.model.VideoSource
import com.squareup.duktape.Duktape
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoStreamResolver @Inject constructor(
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun resolve(playerUrl: String, refererUrl: String): List<VideoSource> {
        return withContext(dispatchers.io) {
            val request = Request.Builder()
                .url(playerUrl)
                .header("Referer", refererUrl)
                .header("Origin", "https://anime3rb.com")
                .header("User-Agent", BrowserHeaders.FALLBACK_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Player request failed: ${response.code} ${response.message}" }
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type").orEmpty()

                if (finalUrl.endsWith(".m3u8") || contentType.contains("mpegurl", ignoreCase = true)) {
                    return@withContext listOf(
                        VideoSource(
                            id = "resolved-hls",
                            label = "HLS",
                            url = finalUrl,
                            type = StreamType.HLS,
                        ),
                    )
                }

                if (finalUrl.endsWith(".mp4") || contentType.contains("video/mp4", ignoreCase = true)) {
                    return@withContext listOf(
                        VideoSource(
                            id = "resolved-mp4",
                            label = "MP4",
                            url = finalUrl,
                            type = StreamType.MP4,
                        ),
                    )
                }

                if (finalUrl.endsWith(".mkv") || contentType.contains("matroska", ignoreCase = true)) {
                    return@withContext listOf(
                        VideoSource(
                            id = "resolved-mkv",
                            label = "MKV",
                            url = finalUrl,
                            type = StreamType.MKV,
                        ),
                    )
                }

                val body = response.body?.string().orEmpty()
                val found = linkedMapOf<String, VideoSource>()

                (extractDirectUrls(body) + extractBase64Urls(body) + extractWithDuktape(body)).forEachIndexed { index, url ->
                    val normalized = url.trim().replace("\\/", "/")
                    val type = when {
                        normalized.contains(".m3u8") -> StreamType.HLS
                        normalized.contains(".mp4") -> StreamType.MP4
                        normalized.contains(".mkv") -> StreamType.MKV
                        else -> StreamType.PLAYER_PAGE
                    }
                    found.putIfAbsent(
                        normalized,
                        VideoSource(
                            id = "resolved-$index",
                            label = when (type) {
                                StreamType.HLS -> "HLS"
                                StreamType.MP4 -> "MP4"
                                StreamType.MKV -> "MKV"
                                StreamType.DOWNLOAD -> "Download"
                                StreamType.PLAYER_PAGE -> "Embedded player"
                            },
                            url = normalized,
                            type = type,
                        ),
                    )
                }

                if (found.isEmpty()) {
                    found[finalUrl] = VideoSource(
                        id = "player-page",
                        label = "Embedded player",
                        url = finalUrl,
                        type = StreamType.PLAYER_PAGE,
                    )
                }

                found.values.toList()
            }
        }
    }

    private fun extractDirectUrls(text: String): List<String> {
        val patterns = listOf(
            "https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*".toRegex(),
            "https?://[^\"'\\s]+\\.mp4[^\"'\\s]*".toRegex(),
            "file\\s*[:=]\\s*[\"'](https?://[^\"']+)[\"']".toRegex(),
            "source\\s*[:=]\\s*[\"'](https?://[^\"']+)[\"']".toRegex(),
        )
        return patterns.flatMap { regex ->
            regex.findAll(text).map { match -> match.groupValues.last() }.toList()
        }.distinct()
    }

    private fun extractBase64Urls(text: String): List<String> {
        val encodedCandidates = "[A-Za-z0-9+/=]{40,}".toRegex()
            .findAll(text)
            .map { it.value }
            .take(30)
            .toList()

        return encodedCandidates.flatMap { encoded ->
            runCatching {
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
                extractDirectUrls(decoded)
            }.getOrDefault(emptyList())
        }.distinct()
    }

    private fun extractWithDuktape(text: String): List<String> {
        return runCatching {
            Duktape.create().use { duktape ->
                val escaped = text
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                val result = duktape.evaluate(
                    """
                    (function() {
                        var source = "$escaped";
                        var match = source.match(/https?:\\/\\/[^"'\\s]+(?:m3u8|mp4)[^"'\\s]*/g);
                        return match ? JSON.stringify(match) : "[]";
                    })()
                    """.trimIndent(),
                ) as? String ?: "[]"
                Json.decodeFromString<List<String>>(result)
            }
        }.getOrDefault(emptyList()).distinct()
    }
}
