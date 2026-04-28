package com.exapps.anistream.core.network

import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface WebSessionStore {
    fun currentUserAgent(): String
    fun updateUserAgent(userAgent: String)
    fun hasFreshSession(url: HttpUrl): Boolean
    fun markSessionAcquired(url: HttpUrl)
}

@Singleton
class InMemoryWebSessionStore @Inject constructor(
    private val cookieStore: MutableCookieStore,
) : WebSessionStore {
    private val userAgent = AtomicReference(BrowserHeaders.FALLBACK_USER_AGENT)
    private val sessionTimestamps = ConcurrentHashMap<String, Long>()

    override fun currentUserAgent(): String = userAgent.get()

    override fun updateUserAgent(userAgent: String) {
        if (userAgent.isBlank()) return
        this.userAgent.set(userAgent)
    }

    override fun hasFreshSession(url: HttpUrl): Boolean {
        val hasCookies = cookieStore.loadForRequest(url).isNotEmpty()
        val timestamp = sessionTimestamps[url.host] ?: return false
        val ageMs = System.currentTimeMillis() - timestamp
        return hasCookies && ageMs < SESSION_TTL_MS
    }

    override fun markSessionAcquired(url: HttpUrl) {
        sessionTimestamps[url.host] = System.currentTimeMillis()
    }

    private companion object {
        const val SESSION_TTL_MS = 30 * 60 * 1000L
    }
}
