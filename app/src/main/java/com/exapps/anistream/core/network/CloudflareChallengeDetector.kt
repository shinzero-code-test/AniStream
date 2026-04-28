package com.exapps.anistream.core.network

import okhttp3.Response

object CloudflareChallengeDetector {
    private val markers = listOf(
        "Just a moment...",
        "Attention Required!",
        "cf-browser-verification",
        "/cdn-cgi/challenge-platform/",
        "cloudflare",
    )

    fun isChallenge(response: Response): Boolean {
        val server = response.header("Server").orEmpty().lowercase()
        val bodyPreview = runCatching { response.peekBody(64L * 1024L).string().lowercase() }.getOrDefault("")
        return response.code in setOf(403, 429, 503) &&
            (server.contains("cloudflare") || markers.any { marker -> bodyPreview.contains(marker.lowercase()) })
    }
}
