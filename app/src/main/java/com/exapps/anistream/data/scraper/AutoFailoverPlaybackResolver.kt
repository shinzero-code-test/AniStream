package com.exapps.anistream.data.scraper

import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.model.VideoSource
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoFailoverPlaybackResolver @Inject constructor(
    private val client: OkHttpClient,
    private val videoStreamResolver: VideoStreamResolver,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun resolve(
        stream: EpisodeStream,
        preferredServerId: String? = null,
        excludedServerIds: Set<String> = emptySet(),
    ): PlaybackAttemptResult {
        return withContext(dispatchers.io) {
            val playerServers = stream.availableSources
                .filter { it.type == StreamType.PLAYER_PAGE }
                .distinctBy { it.serverId ?: it.id }

            val orderedServers = buildList {
                preferredServerId?.let { preferred ->
                    playerServers.firstOrNull { (it.serverId ?: it.id) == preferred }?.let(::add)
                }
                addAll(
                    playerServers.filterNot { source ->
                        val serverId = source.serverId ?: source.id
                        serverId == preferredServerId || excludedServerIds.contains(serverId)
                    },
                )
            }.distinctBy { it.serverId ?: it.id }

            val attemptedServerIds = mutableListOf<String>()

            if (orderedServers.isEmpty() && !stream.playbackUrl.isNullOrBlank()) {
                val playableSources = resolvePlayableSources(stream.playbackUrl, stream.refererUrl)
                return@withContext PlaybackAttemptResult(
                    playbackUrl = playableSources.firstOrNull()?.url ?: stream.playbackUrl,
                    selectedServerId = stream.selectedServerId,
                    playableSources = playableSources,
                    attemptedServerIds = emptyList(),
                )
            }

            orderedServers.forEach { server ->
                val serverId = server.serverId ?: server.id
                attemptedServerIds += serverId
                val playerUrl = when {
                    serverId == stream.selectedServerId && !stream.playbackUrl.isNullOrBlank() -> stream.playbackUrl
                    else -> switchToServer(stream, serverId)
                }

                if (playerUrl.isNullOrBlank()) return@forEach

                val playableSources = resolvePlayableSources(playerUrl, stream.refererUrl)
                if (playableSources.isNotEmpty()) {
                    return@withContext PlaybackAttemptResult(
                        playbackUrl = playableSources.first().url,
                        selectedServerId = serverId,
                        playableSources = playableSources,
                        attemptedServerIds = attemptedServerIds.toList(),
                    )
                }
            }

            PlaybackAttemptResult(
                playbackUrl = stream.playbackUrl,
                selectedServerId = stream.selectedServerId,
                playableSources = emptyList(),
                attemptedServerIds = attemptedServerIds.toList(),
            )
        }
    }

    private suspend fun switchToServer(stream: EpisodeStream, serverId: String): String? {
        val csrfToken = stream.csrfToken ?: return null
        val snapshot = stream.livewireSnapshot ?: return null
        val componentId = stream.livewireComponentId ?: return null

        val componentPayload = JSONObject()
            .put("snapshot", snapshot)
            .put("updates", JSONObject())
            .put(
                "calls",
                JSONArray().put(
                    JSONObject()
                        .put("path", "")
                        .put("method", "setVideoSource")
                        .put("params", JSONArray().put(serverId)),
                ),
            )

        val payload = JSONObject()
            .put("_token", csrfToken)
            .put("components", JSONArray().put(componentPayload))

        val request = Request.Builder()
            .url("https://anime3rb.com/livewire/update")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-CSRF-TOKEN", csrfToken)
            .header("X-Livewire", "true")
            .header("Referer", stream.refererUrl)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("X-AniStream-Component-Id", componentId)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string().orEmpty()
            val root = runCatching { Json.parseToJsonElement(responseBody).jsonObject }.getOrNull() ?: return null
            val firstComponent = root["components"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            val updatedSnapshot = firstComponent["snapshot"]?.jsonPrimitive?.contentOrNull ?: return null
            return extractVideoUrlFromSnapshot(updatedSnapshot)
        }
    }

    private suspend fun resolvePlayableSources(playerUrl: String, refererUrl: String): List<VideoSource> {
        return runCatching {
            videoStreamResolver.resolve(playerUrl = playerUrl, refererUrl = refererUrl)
                .filter { it.type == StreamType.HLS || it.type == StreamType.MP4 || it.type == StreamType.MKV }
        }.getOrDefault(emptyList())
    }

    private fun extractVideoUrlFromSnapshot(snapshot: String): String? {
        return Regex("\"video_url\":\"(.*?)\"")
            .find(snapshot)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\/", "/")
            ?.replace("&amp;", "&")
    }

    data class PlaybackAttemptResult(
        val playbackUrl: String?,
        val selectedServerId: String?,
        val playableSources: List<VideoSource>,
        val attemptedServerIds: List<String>,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
