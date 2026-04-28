package com.exapps.anistream.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserHeadersInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        BrowserHeaders.defaultHeaders.forEach { (name, value) ->
            if (chain.request().header(name).isNullOrBlank()) {
                builder.header(name, value)
            }
        }

        val request = builder.build()
        return chain.proceed(request)
    }
}
