package com.exapps.anistream.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface MutableCookieStore {
    fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>)
    fun loadForRequest(url: HttpUrl): List<Cookie>
    fun saveFromHeader(url: HttpUrl, cookieHeader: String)
    fun hasCookies(url: HttpUrl): Boolean
}

@Singleton
class InMemoryCookieJar @Inject constructor() : CookieJar, MutableCookieStore {
    private val storage = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val key = url.host
        val current = storage[key] ?: mutableListOf()
        cookies.forEach { cookie ->
            current.removeAll { existing ->
                existing.name == cookie.name &&
                    existing.domain == cookie.domain &&
                    existing.path == cookie.path
            }
            current += cookie
        }
        storage[key] = current
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return storage.values
            .flatten()
            .filter { cookie ->
                cookie.expiresAt > now &&
                    url.host.endsWith(cookie.domain.removePrefix(".")) &&
                    url.encodedPath.startsWith(cookie.path)
            }
            .distinctBy { "${it.name}|${it.domain}|${it.path}" }
    }

    override fun saveFromHeader(url: HttpUrl, cookieHeader: String) {
        if (cookieHeader.isBlank()) return
        val cookies = cookieHeader
            .split(";")
            .mapNotNull { part ->
                val pieces = part.trim().split("=", limit = 2)
                if (pieces.size != 2) return@mapNotNull null
                Cookie.Builder()
                    .name(pieces[0].trim())
                    .value(pieces[1].trim())
                    .domain(url.host)
                    .path("/")
                    .apply { if (url.isHttps) secure() }
                    .build()
            }

        saveFromResponse(url, cookies)
    }

    override fun hasCookies(url: HttpUrl): Boolean = loadForRequest(url).isNotEmpty()
}
