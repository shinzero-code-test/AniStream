package com.exapps.anistream.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.CatalogCategory
import com.exapps.anistream.domain.model.CatalogSort
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.SortDirection
import com.exapps.anistream.presentation.components.EmptyStateCard
import com.exapps.anistream.presentation.components.EpisodePosterCard
import com.exapps.anistream.presentation.components.GradientHeroCard
import com.exapps.anistream.presentation.components.SectionTitle
import com.exapps.anistream.presentation.components.TitlePosterCard
import com.exapps.anistream.presentation.components.TitleSummaryCard

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDetails: (String) -> Unit,
    onOpenEpisode: (String, Int) -> Unit,
    onOpenCatalog: (CatalogCategory) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stringResource(id = R.string.nav_home), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(id = R.string.home_search_supporting),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.anistream_logo),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp)
                            .padding(end = 8.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SectionTitle(
                    title = stringResource(id = R.string.catalog_categories_title),
                    subtitle = stringResource(id = R.string.catalog_categories_subtitle),
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.catalogCategories.forEach { category ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenCatalog(category) },
                            label = { Text(category.label) },
                        )
                    }
                }
            }

            item {
                GradientHeroCard(
                    title = stringResource(id = R.string.home_hero_title),
                    subtitle = stringResource(id = R.string.home_hero_subtitle),
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    label = { Text(stringResource(id = R.string.home_search_label)) },
                    supportingText = {
                        Text(stringResource(id = R.string.home_search_supporting))
                    },
                )
            }

            item {
                SectionTitle(title = stringResource(id = R.string.catalog_sort_label))
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogSort.entries.forEach { sort ->
                        FilterChip(
                            selected = state.catalogFilters.sort == sort,
                            onClick = { viewModel.onCatalogSortSelected(sort) },
                            label = { Text(sort.label()) },
                        )
                    }
                    FilterChip(
                        selected = true,
                        onClick = viewModel::onCatalogDirectionToggle,
                        label = {
                            Text(
                                if (state.catalogFilters.direction == SortDirection.ASC) {
                                    stringResource(id = R.string.catalog_sort_direction_asc)
                                } else {
                                    stringResource(id = R.string.catalog_sort_direction_desc)
                                },
                            )
                        },
                    )
                }
            }

            if (state.isLoading) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(stringResource(id = R.string.home_loading))
                        }
                    }
                }
            }

            if (!state.errorMessage.isNullOrBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(onClick = viewModel::refresh) {
                                Text(stringResource(id = R.string.action_retry))
                            }
                        }
                    }
                }
            }

            if (state.searchQuery.isNotBlank()) {
                item {
                    SectionTitle(
                        title = if (state.isSearching) {
                            stringResource(id = R.string.search_loading_title)
                        } else {
                            stringResource(id = R.string.search_results_title)
                        },
                        subtitle = state.searchQuery,
                    )
                }

                if (!state.isSearching && state.searchResults.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(id = R.string.search_empty_title),
                            message = stringResource(id = R.string.search_empty),
                        )
                    }
                }

                items(state.searchResults, key = { it.slug }) { item ->
                    TitleSummaryCard(item = item, onClick = { onOpenDetails(item.slug) })
                }

                if (state.hasMoreSearchResults) {
                    item {
                        Button(
                            onClick = viewModel::loadMoreSearchResults,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isSearchLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(id = R.string.search_load_more))
                            }
                        }
                    }
                }
            } else {
                item {
                    QuickStatsCard(
                        latestEpisodesCount = state.latestEpisodes.size,
                        latestTitlesCount = state.latestTitles.size,
                        catalogCount = state.catalog.size,
                    )
                }

                if (state.continueWatching.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = stringResource(id = R.string.section_continue_watching_title),
                            subtitle = stringResource(id = R.string.section_continue_watching_subtitle),
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.continueWatching, key = { "${it.titleSlug}-${it.episodeNumber}" }) { item ->
                                HistoryCardCompact(
                                    item = item,
                                    onClick = { onOpenEpisode(item.titleSlug, item.episodeNumber) },
                                )
                            }
                        }
                    }
                }

                homeSection(
                    titleRes = R.string.section_featured_title,
                    subtitleRes = R.string.section_featured_subtitle,
                    items = state.featuredEpisodes,
                    onClick = onOpenEpisode,
                )

                homeSection(
                    titleRes = R.string.section_latest_episodes_title,
                    subtitleRes = R.string.section_latest_episodes_subtitle,
                    items = state.latestEpisodes,
                    onClick = onOpenEpisode,
                )

                titleSection(
                    titleRes = R.string.section_latest_titles_title,
                    subtitleRes = R.string.section_latest_titles_subtitle,
                    items = state.latestTitles,
                    onClick = onOpenDetails,
                )

                item {
                    SectionTitle(
                        title = stringResource(id = R.string.section_catalog_title),
                        subtitle = stringResource(id = R.string.section_catalog_subtitle),
                    )
                }
                items(state.catalog, key = { it.slug }) { item ->
                    TitleSummaryCard(item = item, onClick = { onOpenDetails(item.slug) })
                }
                if (state.hasMoreCatalog) {
                    item {
                        Button(
                            onClick = viewModel::loadMoreCatalog,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isCatalogLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(id = R.string.action_load_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeSection(
    titleRes: Int,
    subtitleRes: Int,
    items: List<EpisodeCard>,
    onClick: (String, Int) -> Unit,
) {
    item {
        SectionTitle(
            title = stringResource(id = titleRes),
            subtitle = stringResource(id = subtitleRes),
        )
    }
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.episodeUrl }) { item ->
                EpisodePosterCard(item = item, onClick = { onClick(it.titleSlug, it.episodeNumber) })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.titleSection(
    titleRes: Int,
    subtitleRes: Int,
    items: List<AnimeCard>,
    onClick: (String) -> Unit,
) {
    item {
        SectionTitle(
            title = stringResource(id = titleRes),
            subtitle = stringResource(id = subtitleRes),
        )
    }
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.slug }) { item ->
                TitlePosterCard(item = item, onClick = { onClick(it.slug) })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickStatsCard(
    latestEpisodesCount: Int,
    latestTitlesCount: Int,
    catalogCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
        shape = RoundedCornerShape(26.dp),
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatPill(label = stringResource(id = R.string.home_stat_latest_episodes), value = latestEpisodesCount.toString())
            StatPill(label = stringResource(id = R.string.home_stat_latest_titles), value = latestTitlesCount.toString())
            StatPill(label = stringResource(id = R.string.home_stat_catalog), value = catalogCount.toString())
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryCardCompact(item: PlaybackHistory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.animeTitle,
                style = MaterialTheme.typography.titleSmall,
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
fun CatalogSort.label(): String {
    return when (this) {
        CatalogSort.ADDITION_DATE -> stringResource(id = R.string.catalog_sort_addition_date)
        CatalogSort.NAME -> stringResource(id = R.string.catalog_sort_name)
        CatalogSort.RELEASE_DATE -> stringResource(id = R.string.catalog_sort_release_date)
        CatalogSort.RATE -> stringResource(id = R.string.catalog_sort_rate)
    }
}
