package com.exapps.anistream.core.webview

import okhttp3.HttpUrl

interface CloudflareChallengeSolver {
    suspend fun ensureClearance(url: HttpUrl)
}
