package com.exapps.anistream.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exapps.anistream.BuildConfig
import com.exapps.anistream.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
            )
        },
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            item {
                SettingToggleCard(
                    title = stringResource(id = R.string.settings_auto_play_next),
                    subtitle = stringResource(id = R.string.settings_auto_play_next_desc),
                    checked = state.preferences.autoPlayNext,
                    onCheckedChange = viewModel::setAutoPlayNext,
                )
            }
            item {
                SettingToggleCard(
                    title = stringResource(id = R.string.settings_prefer_summary),
                    subtitle = stringResource(id = R.string.settings_prefer_summary_desc),
                    checked = state.preferences.preferSummary,
                    onCheckedChange = viewModel::setPreferSummary,
                )
            }
            item {
                SettingToggleCard(
                    title = stringResource(id = R.string.settings_cinema_mode),
                    subtitle = stringResource(id = R.string.settings_cinema_mode_desc),
                    checked = state.preferences.cinemaMode,
                    onCheckedChange = viewModel::setCinemaMode,
                )
            }
            item {
                SettingToggleCard(
                    title = stringResource(id = R.string.settings_dynamic_colors),
                    subtitle = stringResource(id = R.string.settings_dynamic_colors_desc),
                    checked = state.preferences.dynamicColors,
                    onCheckedChange = viewModel::setDynamicColors,
                )
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(id = R.string.settings_local_scraping_note), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(
                                id = R.string.settings_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = viewModel::clearHistory, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.settings_clear_history))
                        }
                        Button(onClick = viewModel::clearWatchlist, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.settings_clear_watchlist))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
