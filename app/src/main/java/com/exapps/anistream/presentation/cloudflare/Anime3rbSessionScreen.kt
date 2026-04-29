package com.exapps.anistream.presentation.cloudflare

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.exapps.anistream.R
import com.exapps.anistream.core.webview.Anime3rbSessionBridge

@Composable
fun Anime3rbSessionScreen(
    modifier: Modifier = Modifier,
    onSessionReady: (String?) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var sessionDelivered by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.cloudflare_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = status ?: stringResource(id = R.string.cloudflare_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val cookieManager = CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                    }
                    WebView(context).apply {
                        webViewRef = this
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            loadsImagesAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                        }

                        fun tryCompleteSession() {
                            if (sessionDelivered) return

                            val title = title.orEmpty()
                            val cookieHeader = cookieManager.getCookie(Anime3rbSessionBridge.BASE_URL).orEmpty()
                            val solved = cookieHeader.contains("cf_clearance") ||
                                (
                                    cookieHeader.isNotBlank() &&
                                        title.isNotBlank() &&
                                        !title.contains("Just a moment", ignoreCase = true) &&
                                        !title.contains("Attention Required", ignoreCase = true)
                                    )

                            if (!solved) {
                                status = context.getString(R.string.cloudflare_waiting)
                                postDelayed({ tryCompleteSession() }, SESSION_SYNC_DELAY_MS)
                                return
                            }

                            sessionDelivered = true
                            cookieManager.flush()
                            status = context.getString(R.string.cloudflare_ready)
                            onSessionReady(settings.userAgentString)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                sessionDelivered = false
                                isLoading = true
                                status = context.getString(R.string.cloudflare_loading)
                                canGoBack = view.canGoBack()
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                isLoading = false
                                canGoBack = view.canGoBack()
                                view.postDelayed({ tryCompleteSession() }, SESSION_SYNC_DELAY_MS)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (!request.isForMainFrame) return
                                isLoading = false
                                val message = error.description.toString()
                                status = message.ifBlank { context.getString(R.string.cloudflare_error) }
                            }
                        }
                        loadUrl(Anime3rbSessionBridge.BASE_URL)
                    }
                },
                update = { webViewRef = it },
            )
        }
    }
}

private const val SESSION_SYNC_DELAY_MS = 800L
