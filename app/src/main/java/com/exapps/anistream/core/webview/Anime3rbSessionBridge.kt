package com.exapps.anistream.core.webview

import android.webkit.CookieManager
import com.exapps.anistream.core.network.BrowserHeaders
import com.exapps.anistream.core.network.MutableCookieStore
import com.exapps.anistream.core.network.WebSessionStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Anime3rbSessionBridge @Inject constructor(
    private val cookieStore: MutableCookieStore,
    private val sessionStore: WebSessionStore,
) {
    fun hasSession(): Boolean = sessionStore.hasFreshSession(BASE_URL.toHttpUrl())

    fun syncFromWebView(userAgent: String? = null): Boolean {
        val url = BASE_URL.toHttpUrl()
        val cookieHeader = CookieManager.getInstance().getCookie(BASE_URL).orEmpty()
        val resolvedUserAgent = userAgent?.takeIf { it.isNotBlank() }
            ?: BrowserHeaders.FALLBACK_USER_AGENT
        sessionStore.updateUserAgent(resolvedUserAgent)

        cookieStore.saveFromHeader(url, cookieHeader)
        val hasCookies = cookieStore.hasCookies(url)
        if (hasCookies) {
            sessionStore.markSessionAcquired(url)
        }
        return hasCookies
    }

    companion object {
        const val BASE_URL = "https://anime3rb.com/"
    }
}
