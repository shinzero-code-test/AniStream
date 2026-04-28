package com.exapps.anistream.core.network

object BrowserHeaders {
    const val FALLBACK_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; AniStream) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    fun buildDefaultHeaders(userAgent: String = FALLBACK_USER_AGENT): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "ar,en-US;q=0.9,en;q=0.8",
        "Cache-Control" to "no-cache",
        "Pragma" to "no-cache",
        "Upgrade-Insecure-Requests" to "1",
    )
}
