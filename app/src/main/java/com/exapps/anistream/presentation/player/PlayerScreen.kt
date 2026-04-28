package com.exapps.anistream.presentation.player

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.exapps.anistream.R
import com.exapps.anistream.core.network.BrowserHeaders
import com.exapps.anistream.domain.model.StreamType

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenEpisode: (String, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val latestOpenEpisode by rememberUpdatedState(newValue = onOpenEpisode)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.stream?.animeTitle ?: stringResource(id = R.string.player_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.content_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.stream == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = state.errorMessage ?: stringResource(id = R.string.player_empty))
                }
            }

            else -> {
                val stream = state.stream!!
                val playableSources = stream.availableSources.filter { it.type == StreamType.HLS || it.type == StreamType.MP4 }
                var selectedUrl by rememberSaveable(stream.titleSlug, stream.episodeNumber) {
                    mutableStateOf(
                        playableSources.firstOrNull()?.url
                            ?: stream.playbackUrl?.takeIf { it.contains(".m3u8") || it.contains(".mp4") },
                    )
                }

                val context = LocalContext.current
                val exoPlayer = remember(stream.titleSlug, stream.episodeNumber) {
                    ExoPlayer.Builder(context)
                        .setMediaSourceFactory(
                            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                                DefaultHttpDataSource.Factory()
                                    .setUserAgent(BrowserHeaders.USER_AGENT)
                                    .setDefaultRequestProperties(
                                        mapOf(
                                            "Referer" to stream.refererUrl,
                                            "Origin" to "https://anime3rb.com",
                                        ),
                                    ),
                            ),
                        )
                        .build()
                }

                LaunchedEffect(selectedUrl) {
                    val url = selectedUrl ?: return@LaunchedEffect
                    exoPlayer.setMediaItem(MediaItem.fromUri(url))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }

                DisposableEffect(exoPlayer, state.autoPlayNext, stream.nextEpisodeNumber) {
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (
                                playbackState == Player.STATE_ENDED &&
                                state.autoPlayNext &&
                                stream.nextEpisodeNumber != null
                            ) {
                                latestOpenEpisode(stream.titleSlug, stream.nextEpisodeNumber)
                            }
                        }
                    }
                    exoPlayer.addListener(listener)
                    onDispose {
                        exoPlayer.removeListener(listener)
                        viewModel.persistPlayback(exoPlayer.currentPosition)
                        exoPlayer.release()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (selectedUrl != null) {
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp),
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                                useController = true
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                player = exoPlayer
                                            }
                                        },
                                        update = { view -> view.player = exoPlayer },
                                    )
                                }

                                Text(
                                    text = stringResource(id = R.string.player_episode_header, stream.animeTitle, stream.episodeNumber),
                                    style = MaterialTheme.typography.titleLarge,
                                )

                                if (!stream.episodeTitle.isNullOrBlank()) {
                                    Text(
                                        text = stream.episodeTitle.orEmpty(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (stream.views != null) {
                                    Text(
                                        text = "${stream.views} ${stringResource(id = R.string.label_views)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                if (stream.batchDownloadUrl != null) {
                                    AssistChip(
                                        onClick = { uriHandler.openUri(stream.batchDownloadUrl) },
                                        label = { Text(stringResource(id = R.string.player_open_download_pack)) },
                                    )
                                }

                                if (stream.nextEpisodeNumber != null) {
                                    AssistChip(
                                        onClick = { latestOpenEpisode(stream.titleSlug, stream.nextEpisodeNumber) },
                                        label = { Text(stringResource(id = R.string.player_next_episode)) },
                                    )
                                }
                            }
                        }
                    }

                    if (playableSources.isNotEmpty()) {
                        item {
                            Text(text = stringResource(id = R.string.player_sources_title), style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                playableSources.forEach { source ->
                                    AssistChip(
                                        onClick = { selectedUrl = source.url },
                                        label = { Text(source.label) },
                                    )
                                }
                            }
                        }
                    }

                    if (stream.downloadLinks.isNotEmpty()) {
                        item {
                            Text(text = stringResource(id = R.string.player_downloads_title), style = MaterialTheme.typography.titleMedium)
                        }

                        items(stream.downloadLinks, key = { it.url }) { download ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri(download.url) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = download.qualityLabel, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = download.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
