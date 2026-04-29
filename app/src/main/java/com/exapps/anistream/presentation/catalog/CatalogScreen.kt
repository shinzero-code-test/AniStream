package com.exapps.anistream.presentation.catalog

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exapps.anistream.R
import com.exapps.anistream.domain.model.CatalogSort
import com.exapps.anistream.domain.model.SortDirection
import com.exapps.anistream.presentation.components.EmptyStateCard
import com.exapps.anistream.presentation.components.SectionTitle
import com.exapps.anistream.presentation.components.TitleSummaryCard
import com.exapps.anistream.presentation.dashboard.label

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onBack: () -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.category.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.content_back))
                    }
                },
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
                SectionTitle(
                    title = state.category.label,
                    subtitle = stringResource(id = R.string.catalog_screen_subtitle),
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogSort.entries.forEach { sort ->
                        FilterChip(
                            selected = state.filters.sort == sort,
                            onClick = { viewModel.onSortSelected(sort) },
                            label = { Text(sort.label()) },
                        )
                    }
                    FilterChip(
                        selected = true,
                        onClick = viewModel::onDirectionToggle,
                        label = {
                            Text(
                                if (state.filters.direction == SortDirection.ASC) {
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

            if (!state.isLoading && state.items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(id = R.string.search_empty_title),
                        message = stringResource(id = R.string.search_empty),
                    )
                }
            }

            items(state.items, key = { it.slug }) { item ->
                TitleSummaryCard(item = item, onClick = { onOpenDetails(item.slug) })
            }

            if (state.hasMore) {
                item {
                    Button(
                        onClick = viewModel::loadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isLoadingMore) {
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
