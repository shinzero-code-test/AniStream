package com.exapps.anistream.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exapps.anistream.domain.model.UserPreferences
import com.exapps.anistream.domain.usecase.ClearHistoryUseCase
import com.exapps.anistream.domain.usecase.ClearWatchlistUseCase
import com.exapps.anistream.domain.usecase.ObservePreferencesUseCase
import com.exapps.anistream.domain.usecase.SetAutoPlayNextUseCase
import com.exapps.anistream.domain.usecase.SetCinemaModeUseCase
import com.exapps.anistream.domain.usecase.SetDynamicColorsUseCase
import com.exapps.anistream.domain.usecase.SetPreferSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observePreferencesUseCase: ObservePreferencesUseCase,
    private val setAutoPlayNextUseCase: SetAutoPlayNextUseCase,
    private val setPreferSummaryUseCase: SetPreferSummaryUseCase,
    private val setCinemaModeUseCase: SetCinemaModeUseCase,
    private val setDynamicColorsUseCase: SetDynamicColorsUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val clearWatchlistUseCase: ClearWatchlistUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
            }
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { setAutoPlayNextUseCase(enabled) }
    }

    fun setPreferSummary(enabled: Boolean) {
        viewModelScope.launch { setPreferSummaryUseCase(enabled) }
    }

    fun setCinemaMode(enabled: Boolean) {
        viewModelScope.launch { setCinemaModeUseCase(enabled) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { setDynamicColorsUseCase(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { clearHistoryUseCase() }
    }

    fun clearWatchlist() {
        viewModelScope.launch { clearWatchlistUseCase() }
    }
}

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
)
