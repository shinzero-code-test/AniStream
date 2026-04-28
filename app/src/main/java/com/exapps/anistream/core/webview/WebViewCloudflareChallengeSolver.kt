package com.exapps.anistream.core.webview

import android.content.Context
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.core.network.BrowserHeaders
import com.exapps.anistream.core.network.MutableCookieStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewCloudflareChallengeSolver @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cookieStore: MutableCookieStore,
    private val dispatchers: DispatcherProvider,
) : CloudflareChallengeSolver {

    private val mutex = Mutex()

    override suspend fun ensureClearance(url: HttpUrl) {
        mutex.withLock {
            withTimeout(45_000) {
                withContext(dispatchers.main) {
                    if (Looper.myLooper() == null) {
                        throw IllegalStateException("WebView requires a prepared main looper")
                    }

                    suspendCancellableCoroutine { continuation ->
                        val cookieManager = CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                        }

                        val webView = WebView(appContext)
                        runCatching { cookieManager.setAcceptThirdPartyCookies(webView, true) }
                        fun cleanup() {
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.clearHistory()
                            webView.removeAllViews()
                            webView.destroy()
                        }

                        continuation.invokeOnCancellation { cleanup() }

                        webView.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = false
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = BrowserHeaders.USER_AGENT
                        }
                        webView.isVerticalScrollBarEnabled = false
                        webView.isHorizontalScrollBarEnabled = false
                        webView.webChromeClient = WebChromeClient()
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, finishedUrl: String) {
                                view.postDelayed({
                                    val cookieHeader = cookieManager.getCookie(url.toString()).orEmpty()
                                    val title = view.title.orEmpty()
                                    val solved = cookieHeader.contains("cf_clearance") ||
                                        (!title.contains("Just a moment", ignoreCase = true) &&
                                            !title.contains("Attention Required", ignoreCase = true))
                                    if (!solved || continuation.isCompleted) return@postDelayed

                                    cookieManager.flush()
                                    cookieStore.saveFromHeader(url, cookieHeader)
                                    continuation.resume(Unit)
                                    cleanup()
                                }, 750)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: android.webkit.WebResourceRequest,
                                error: android.webkit.WebResourceError,
                            ) {
                                if (!request.isForMainFrame || continuation.isCompleted) return
                                continuation.resumeWithException(IllegalStateException(error.description.toString()))
                                cleanup()
                            }
                        }

                        webView.loadUrl(url.toString(), BrowserHeaders.defaultHeaders)
                    }
                }
            }
        }
    }
}
