package com.exapps.anistream.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.model.EpisodeItem
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.WatchStatus
import com.exapps.anistream.domain.model.WatchlistAnime
import kotlin.math.roundToInt

@Composable
fun TitlePosterCard(
    item: AnimeCard,
    onClick: (AnimeCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(188.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item) }
                .padding(10.dp),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            MetadataLine(primary = item.seasonLabel, secondary = item.episodeCount, tertiary = item.rating)
        }
    }
}

@Composable
fun TitleSummaryCard(
    item: AnimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetadataLine(primary = item.seasonLabel, secondary = item.episodeCount, tertiary = item.rating)
                if (!item.genres.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.genres.take(3).forEach { genre ->
                            AssistChip(
                                onClick = {},
                                label = { Text(genre) },
                                colors = AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }
                if (!item.synopsis.isNullOrBlank()) {
                    Text(
                        text = item.synopsis.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodePosterCard(
    item: EpisodeCard,
    onClick: (EpisodeCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item) }
                .padding(10.dp),
        ) {
            Box {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.animeTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                ),
                            ),
                        )
                        .padding(8.dp),
                ) {
                    Text(
                        text = item.episodeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.animeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.episodeTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.episodeTitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action?.invoke()
        }
    }
}

@Composable
fun GradientHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                )
            }
            content()
        }
    }
}

@Composable
fun EpisodeRow(
    item: EpisodeItem,
    onClick: (EpisodeItem) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.episode_row_title, item.episodeNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = item.title ?: stringResource(id = R.string.action_open),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.duration.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.episode_duration, item.duration.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun WatchlistRowCard(
    item: WatchlistAnime,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(watchStatusLabel(item.watchStatus)) })
                    if (item.userRating != null) {
                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.metadata_rating_short, item.userRating.toString())) })
                    }
                }

                if (!item.synopsis.isNullOrBlank()) {
                    Text(
                        text = item.synopsis.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryRowCard(
    item: PlaybackHistory,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.animeTitle,
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.animeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(id = R.string.episode_row_title, item.episodeNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!item.episodeTitle.isNullOrBlank()) {
                    Text(
                        text = item.episodeTitle.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(id = R.string.history_position, formatMillis(item.playbackPositionMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetadataLine(
    primary: String?,
    secondary: String?,
    tertiary: String?,
) {
    val secondaryText = secondary?.takeIf { it.isNotBlank() }
        ?.let { stringResource(id = R.string.metadata_episode_count_short, it) }
    val tertiaryText = tertiary?.takeIf { it.isNotBlank() }
        ?.let { stringResource(id = R.string.metadata_rating_short, it) }
    val parts = listOfNotNull(primary?.takeIf { it.isNotBlank() }, secondaryText, tertiaryText)
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString(separator = "  •  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun watchStatusLabel(status: WatchStatus): String {
    return when (status) {
        WatchStatus.WATCHING -> stringResource(id = R.string.watch_status_watching)
        WatchStatus.COMPLETED -> stringResource(id = R.string.watch_status_completed)
        WatchStatus.ON_HOLD -> stringResource(id = R.string.watch_status_on_hold)
        WatchStatus.DROPPED -> stringResource(id = R.string.watch_status_dropped)
        WatchStatus.PLAN_TO_WATCH -> stringResource(id = R.string.watch_status_plan_to_watch)
    }
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1000f).roundToInt().coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
