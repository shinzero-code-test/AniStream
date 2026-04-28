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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.exapps.anistream.presentation.components.EpisodeRow
import com.exapps.anistream.presentation.components.SectionTitle

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.details?.title ?: "Anime Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleWatchlist) {
                        Icon(
                            imageVector = if (state.isWatchlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (state.isWatchlisted) "Remove from watchlist" else "Add to watchlist",
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
                    Text(text = state.errorMessage ?: "Nothing to show")
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
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(genre) },
                                                colors = AssistChipDefaults.assistChipColors(),
                                            )
                                        }
                                    }

                                    MetadataGrid(details = details)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (state.preferSummary) "Summary mode" else "Full story mode",
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                text = "Persisted with DataStore",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Switch(
                                            checked = state.preferSummary,
                                            onCheckedChange = viewModel::updatePreferSummary,
                                        )
                                    }

                                    Text(
                                        text = if (state.preferSummary) details.summary ?: details.synopsis else details.synopsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )

                                    if (details.alternateTitles.isNotEmpty()) {
                                        SectionTitle(title = "Alternate Titles")
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

                    item {
                        SectionTitle(
                            title = "Episodes",
                            subtitle = "Parsed from the exact `.video-list .video-data` structure.",
                        )
                    }

                    items(details.episodes, key = { it.episodeUrl }) { episode ->
                        EpisodeRow(
                            item = episode,
                            onClick = { onPlayEpisode(it.titleSlug, it.episodeNumber) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetadataGrid(details: com.exapps.anistream.domain.model.AnimeDetails) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metadataCard(label = "Status", value = details.status)
        metadataCard(label = "Season", value = details.releaseSeason)
        metadataCard(label = "Studio", value = details.studio)
        metadataCard(label = "Author", value = details.author)
        metadataCard(label = "Score", value = details.score)
        metadataCard(label = "Episodes", value = details.episodeCount?.toString())
        metadataCard(label = "Age", value = details.ageRating)
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
