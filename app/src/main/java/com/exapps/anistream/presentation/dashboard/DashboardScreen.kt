package com.exapps.anistream.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.presentation.components.EpisodePosterCard
import com.exapps.anistream.presentation.components.SectionTitle
import com.exapps.anistream.presentation.components.TitlePosterCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenDetails: (String) -> Unit,
    onOpenEpisode: (String, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.anistream_logo),
                        contentDescription = "AniStream",
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp),
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
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    label = { Text("Search Anime3rb") },
                    supportingText = {
                        Text("Powered by the locally verified Anime3rb HTML structure.")
                    },
                )
            }

            if (state.isLoading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Loading AniStream home feed and catalog...")
                        }
                    }
                }
            }

            if (!state.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(onClick = viewModel::refresh) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            if (state.searchQuery.isNotBlank()) {
                item {
                    SectionTitle(
                        title = if (state.isSearching) "Searching..." else "Search Results",
                        subtitle = state.searchQuery,
                    )
                }
                items(state.searchResults, key = { it.slug }) { item ->
                    SearchResultCard(item = item, onClick = { onOpenDetails(item.slug) })
                }
            } else {
                homeSection(
                    title = "Popular Now",
                    subtitle = "Pinned episodes from Anime3rb home.html",
                    items = state.featuredEpisodes,
                    onClick = onOpenEpisode,
                )

                homeSection(
                    title = "Latest Episodes",
                    subtitle = "Exact `#videos a.video-card` extraction",
                    items = state.latestEpisodes,
                    onClick = onOpenEpisode,
                )

                titleSection(
                    title = "Recently Added Titles",
                    subtitle = "Exact `.title-card` slider extraction",
                    items = state.latestTitles,
                    onClick = onOpenDetails,
                )

                titleSection(
                    title = "Catalog",
                    subtitle = "Page 1 of `animelist.html`",
                    items = state.catalog,
                    onClick = onOpenDetails,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeSection(
    title: String,
    subtitle: String,
    items: List<EpisodeCard>,
    onClick: (String, Int) -> Unit,
) {
    item {
        SectionTitle(title = title, subtitle = subtitle)
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
    title: String,
    subtitle: String,
    items: List<AnimeCard>,
    onClick: (String) -> Unit,
) {
    item {
        SectionTitle(title = title, subtitle = subtitle)
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
private fun SearchResultCard(item: AnimeCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TitlePosterCard(item = item, onClick = { onClick() }, modifier = Modifier.fillMaxWidth())
            if (!item.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.synopsis.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
