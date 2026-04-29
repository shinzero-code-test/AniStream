package com.exapps.anistream.core.webview

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.core.network.BrowserHeaders
import com.exapps.anistream.core.network.MutableCookieStore
import com.exapps.anistream.core.network.WebSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareWebViewInterceptor @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cookieStore: MutableCookieStore,
    private val sessionStore: WebSessionStore,
    private val dispatchers: DispatcherProvider,
) : CloudflareChallengeSolver {

    private val mutex = Mutex()

    override suspend fun ensureClearance(url: HttpUrl) {
        if (sessionStore.hasFreshSession(url)) return

        mutex.withLock {
            if (sessionStore.hasFreshSession(url)) return

            withTimeout(45_000) {
                withContext(dispatchers.main) {
                    check(Looper.myLooper() == Looper.getMainLooper()) {
                        "WebView bootstrap must run on the main thread"
                    }

                    suspendCancellableCoroutine<Unit> { continuation ->
                        val cookieManager = CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                        }
                        val webView = WebView(appContext)
                        val mainHandler = Handler(Looper.getMainLooper())
                        val cleanedUp = AtomicBoolean(false)
                        runCatching { cookieManager.setAcceptThirdPartyCookies(webView, true) }

                        fun cleanup() {
                            if (!cleanedUp.compareAndSet(false, true)) return
                            mainHandler.post {
                                runCatching { webView.stopLoading() }
                                runCatching { webView.loadUrl("about:blank") }
                                runCatching { webView.clearHistory() }
                                runCatching { webView.removeAllViews() }
                                runCatching { webView.destroy() }
                            }
                        }

                        continuation.invokeOnCancellation { cleanup() }

                        webView.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = false
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = sessionStore.currentUserAgent()
                        }
                        webView.isVerticalScrollBarEnabled = false
                        webView.isHorizontalScrollBarEnabled = false
                        webView.webChromeClient = WebChromeClient()
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, pageUrl: String, favicon: Bitmap?) {
                                val currentUserAgent = view.settings.userAgentString.orEmpty()
                                if (currentUserAgent.isNotBlank()) {
                                    sessionStore.updateUserAgent(currentUserAgent)
                                }
                            }

                            override fun onPageFinished(view: WebView, pageUrl: String) {
                                view.postDelayed({
                                    if (continuation.isCompleted) return@postDelayed

                                    val resolvedUrl = pageUrl.toHttpUrlOrNull() ?: url
                                    val cookieHeader = cookieManager.getCookie(pageUrl)
                                        ?: cookieManager.getCookie(url.toString())
                                        .orEmpty()
                                    val currentUserAgent = view.settings.userAgentString.orEmpty()
                                    if (currentUserAgent.isNotBlank()) {
                                        sessionStore.updateUserAgent(currentUserAgent)
                                    }

                                    val solved = cookieHeader.contains("cf_clearance") ||
                                        (!view.title.orEmpty().contains("Just a moment", ignoreCase = true) &&
                                            !view.title.orEmpty().contains("Attention Required", ignoreCase = true))

                                    if (!solved) return@postDelayed

                                    cookieManager.flush()
                                    cookieStore.saveFromHeader(resolvedUrl, cookieHeader)
                                    sessionStore.markSessionAcquired(resolvedUrl)
                                    continuation.resume(Unit)
                                    cleanup()
                                }, 900)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (!request.isForMainFrame || continuation.isCompleted) return
                                continuation.resumeWithException(IllegalStateException(error.description.toString()))
                                cleanup()
                            }
                        }

                        webView.loadUrl(
                            url.toString(),
                            BrowserHeaders.buildDefaultHeaders(sessionStore.currentUserAgent()),
                        )
                    }
                }
            }
        }
    }
}
