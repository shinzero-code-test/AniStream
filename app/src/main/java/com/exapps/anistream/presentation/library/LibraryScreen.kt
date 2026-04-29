package com.exapps.anistream.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.WatchStatus
import com.exapps.anistream.presentation.components.EmptyStateCard
import com.exapps.anistream.presentation.components.GradientHeroCard
import com.exapps.anistream.presentation.components.HistoryRowCard
import com.exapps.anistream.presentation.components.SectionTitle
import com.exapps.anistream.presentation.components.WatchlistRowCard
import com.exapps.anistream.presentation.components.watchStatusLabel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenDetails: (String) -> Unit,
    onOpenEpisode: (String, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedStatus by remember { mutableStateOf(WatchStatus.PLAN_TO_WATCH) }

    val filteredWatchlist = state.watchlist.filter { it.watchStatus == selectedStatus }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.library_title)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GradientHeroCard(
                    title = stringResource(id = R.string.library_hero_title),
                    subtitle = stringResource(id = R.string.library_hero_subtitle),
                )
            }
            item {
                LibraryStatsCard(
                    watchlistCount = state.watchlist.size,
                    historyCount = state.history.size,
                    completedCount = state.watchlist.count { it.watchStatus == WatchStatus.COMPLETED },
                )
            }
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(id = R.string.library_watchlist_tab)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(id = R.string.library_history_tab)) })
                }
            }

            if (selectedTab == 0) {
                item {
                    SectionTitle(title = stringResource(id = R.string.library_watch_status_filter))
                }
                item {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WatchStatus.entries.forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(watchStatusLabel(status)) },
                            )
                        }
                    }
                }

                if (filteredWatchlist.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(id = R.string.library_watchlist_empty_title),
                            message = stringResource(id = R.string.library_watchlist_empty),
                        )
                    }
                } else {
                    items(filteredWatchlist, key = { it.slug }) { item ->
                        WatchlistRowCard(item = item, onClick = { onOpenDetails(item.slug) })
                    }
                }
            } else {
                if (state.history.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(id = R.string.library_history_empty_title),
                            message = stringResource(id = R.string.library_history_empty),
                        )
                    }
                } else {
                    items(state.history, key = { "${it.titleSlug}-${it.episodeNumber}" }) { item ->
                        HistoryRowCard(item = item, onClick = { onOpenEpisode(item.titleSlug, item.episodeNumber) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryStatsCard(
    watchlistCount: Int,
    historyCount: Int,
    completedCount: Int,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LibraryStat(label = stringResource(id = R.string.library_stat_watchlist), value = watchlistCount.toString())
            LibraryStat(label = stringResource(id = R.string.library_stat_history), value = historyCount.toString())
            LibraryStat(label = stringResource(id = R.string.library_stat_completed), value = completedCount.toString())
        }
    }
}

@Composable
private fun LibraryStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
