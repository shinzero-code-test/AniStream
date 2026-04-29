package com.exapps.anistream.core.network

import com.exapps.anistream.core.webview.CloudflareChallengeSolver
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareChallengeInterceptor @Inject constructor(
    private val solver: CloudflareChallengeSolver,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.header(RETRY_HEADER) == RETRY_MARKER || !CloudflareChallengeDetector.isChallenge(response)) {
            return response
        }

        response.close()
        solver.markChallengeRequired()

        return chain.proceed(
            request.newBuilder()
                .header(RETRY_HEADER, RETRY_MARKER)
                .build(),
        )
    }

    private companion object {
        const val RETRY_HEADER = "X-AniStream-CF-Retry"
        const val RETRY_MARKER = "1"
    }
}
