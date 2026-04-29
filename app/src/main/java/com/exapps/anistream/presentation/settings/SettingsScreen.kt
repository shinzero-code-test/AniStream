package com.exapps.anistream.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.exapps.anistream.presentation.components.GradientHeroCard

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
                GradientHeroCard(
                    title = stringResource(id = R.string.settings_hero_title),
                    subtitle = stringResource(id = R.string.settings_subtitle),
                )
            }

            item {
                SkipIntroCard(
                    seconds = state.preferences.skipIntroSeconds,
                    onSecondsChange = viewModel::setSkipIntroSeconds,
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))) {
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
                        Text(stringResource(id = R.string.settings_danger_zone), style = MaterialTheme.typography.titleMedium)
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
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

@Composable
private fun SkipIntroCard(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(id = R.string.settings_skip_intro), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = R.string.settings_skip_intro_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(onClick = {}, label = { Text(stringResource(id = R.string.settings_skip_intro_value, seconds)) })
            }
            Slider(
                value = seconds.toFloat(),
                onValueChange = { onSecondsChange(it.toInt()) },
                valueRange = 0f..180f,
                steps = 11,
            )
        }
    }
}
