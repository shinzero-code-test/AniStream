package com.exapps.anistream.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.usecase.GetAnimeDetailsUseCase
import com.exapps.anistream.domain.usecase.ObserveIsWatchlistedUseCase
import com.exapps.anistream.domain.usecase.ObservePreferencesUseCase
import com.exapps.anistream.domain.usecase.SetPreferSummaryUseCase
import com.exapps.anistream.domain.usecase.ToggleWatchlistUseCase
import com.exapps.anistream.presentation.navigation.DetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val observeIsWatchlistedUseCase: ObserveIsWatchlistedUseCase,
    private val observePreferencesUseCase: ObservePreferencesUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    private val setPreferSummaryUseCase: SetPreferSummaryUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
        observeWatchlistState()
        observeSummaryPreference()
    }

    fun toggleWatchlist() {
        val details = _uiState.value.details ?: return
        viewModelScope.launch { toggleWatchlistUseCase(details) }
    }

    fun updatePreferSummary(enabled: Boolean) {
        viewModelScope.launch { setPreferSummaryUseCase(enabled) }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getAnimeDetailsUseCase(route.slug) }
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            details = details,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load anime details.",
                        )
                    }
                }
        }
    }

    private fun observeWatchlistState() {
        viewModelScope.launch {
            observeIsWatchlistedUseCase(route.slug).collect { isWatchlisted ->
                _uiState.update { it.copy(isWatchlisted = isWatchlisted) }
            }
        }
    }

    private fun observeSummaryPreference() {
        viewModelScope.launch {
            observePreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferSummary = preferences.preferSummary) }
            }
        }
    }
}

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: AnimeDetails? = null,
    val isWatchlisted: Boolean = false,
    val preferSummary: Boolean = false,
    val errorMessage: String? = null,
)
