package com.exapps.anistream.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.WatchStatus
import com.exapps.anistream.presentation.components.EmptyStateCard
import com.exapps.anistream.presentation.components.EpisodeRow
import com.exapps.anistream.presentation.components.SectionTitle
import com.exapps.anistream.presentation.components.TitlePosterCard
import com.exapps.anistream.presentation.components.watchStatusLabel

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.details?.title ?: stringResource(id = R.string.details_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.content_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleWatchlist) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (state.isWatchlisted) {
                                stringResource(id = R.string.details_watchlist_remove)
                            } else {
                                stringResource(id = R.string.details_watchlist_add)
                            },
                        )
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

            state.details == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = state.errorMessage ?: stringResource(id = R.string.details_empty))
                }
            }

            else -> {
                val details = state.details!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        HeaderCard(details = details)
                    }

                    details.episodes.firstOrNull()?.let { firstEpisode ->
                        item {
                            Button(
                                onClick = { onPlayEpisode(firstEpisode.titleSlug, firstEpisode.episodeNumber) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(id = R.string.details_start_watching))
                            }
                        }
                    }

                    item {
                        WatchStateCard(
                            details = details,
                            state = state,
                            onSetStatus = viewModel::setWatchStatus,
                            onSetRating = viewModel::setRating,
                            onToggleSummary = viewModel::updatePreferSummary,
                        )
                    }

                    if (details.externalLinks.isNotEmpty()) {
                        item { SectionTitle(title = stringResource(id = R.string.details_external_sources)) }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                details.externalLinks.forEach { link ->
                                    AssistChip(
                                        onClick = { uriHandler.openUri(link.url) },
                                        label = { Text(link.label) },
                                        colors = AssistChipDefaults.assistChipColors(),
                                    )
                                }
                            }
                        }
                    }

                    if (details.trailers.isNotEmpty()) {
                        item { SectionTitle(title = stringResource(id = R.string.details_trailers)) }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                details.trailers.forEach { trailer ->
                                    AssistChip(
                                        onClick = { uriHandler.openUri(trailer.embedUrl) },
                                        label = { Text(trailer.title) },
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionTitle(
                            title = stringResource(id = R.string.details_episodes_title),
                            subtitle = stringResource(id = R.string.details_episodes_subtitle),
                        )
                    }

                    if (details.episodes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = stringResource(id = R.string.details_episodes_empty_title),
                                message = stringResource(id = R.string.details_episodes_empty),
                            )
                        }
                    } else {
                        items(details.episodes, key = { it.episodeUrl }) { episode ->
                            EpisodeRow(item = episode, onClick = { onPlayEpisode(it.titleSlug, it.episodeNumber) })
                        }
                    }

                    if (details.relatedTitles.isNotEmpty()) {
                        item { SectionTitle(title = stringResource(id = R.string.details_related_titles)) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(details.relatedTitles, key = { it.slug }) { item ->
                                    TitlePosterCard(item = item, onClick = { onOpenDetails(it.slug) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeaderCard(details: AnimeDetails) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = details.posterUrl,
                    contentDescription = details.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )
            }

            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (!details.typeLabel.isNullOrBlank()) {
                    Text(
                        text = details.typeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.genres.forEach { genre ->
                        AssistChip(onClick = {}, label = { Text(genre) }, colors = AssistChipDefaults.assistChipColors())
                    }
                }

                MetadataGrid(details = details)

                if (details.alternateTitles.isNotEmpty()) {
                    SectionTitle(title = stringResource(id = R.string.details_alternate_titles))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.alternateTitles.forEach { alt ->
                            AssistChip(onClick = {}, label = { Text(alt) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WatchStateCard(
    details: AnimeDetails,
    state: DetailsUiState,
    onSetStatus: (WatchStatus) -> Unit,
    onSetRating: (Int?) -> Unit,
    onToggleSummary: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.preferSummary) {
                            stringResource(id = R.string.details_summary_mode)
                        } else {
                            stringResource(id = R.string.details_full_story_mode)
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(id = R.string.details_preference_persisted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.preferSummary, onCheckedChange = onToggleSummary)
            }

            Text(
                text = if (state.preferSummary) details.summary ?: details.synopsis else details.synopsis,
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle(title = stringResource(id = R.string.details_local_status))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WatchStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.watchEntry?.watchStatus == status,
                        onClick = { onSetStatus(status) },
                        label = { Text(watchStatusLabel(status)) },
                    )
                }
            }

            SectionTitle(title = stringResource(id = R.string.details_local_rating))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..10).forEach { rating ->
                    FilterChip(
                        selected = state.watchEntry?.userRating == rating,
                        onClick = { onSetRating(rating) },
                        label = { Text(rating.toString()) },
                    )
                }
                if (state.watchEntry?.userRating != null) {
                    AssistChip(onClick = { onSetRating(null) }, label = { Text(stringResource(id = R.string.rating_clear)) })
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetadataGrid(details: AnimeDetails) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metadataCard(label = stringResource(id = R.string.label_status), value = details.status)
        metadataCard(label = stringResource(id = R.string.label_season), value = details.releaseSeason)
        metadataCard(label = stringResource(id = R.string.label_studio), value = details.studio)
        metadataCard(label = stringResource(id = R.string.label_author), value = details.author)
        metadataCard(label = stringResource(id = R.string.label_score), value = details.score)
        metadataCard(label = stringResource(id = R.string.label_rating_count), value = details.ratingCount?.toString())
        metadataCard(label = stringResource(id = R.string.label_published_at), value = details.publishedAt)
        metadataCard(label = stringResource(id = R.string.label_episodes), value = details.episodeCount?.toString())
        metadataCard(label = stringResource(id = R.string.label_age), value = details.ageRating)
    }
}

@Composable
private fun metadataCard(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
