package com.exapps.anistream.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exapps.anistream.domain.model.AnimeDetails
import com.exapps.anistream.domain.model.WatchStatus
import com.exapps.anistream.domain.model.WatchlistAnime
import com.exapps.anistream.domain.usecase.GetAnimeDetailsUseCase
import com.exapps.anistream.domain.usecase.ObservePreferencesUseCase
import com.exapps.anistream.domain.usecase.ObserveWatchEntryUseCase
import com.exapps.anistream.domain.usecase.SetAnimeRatingUseCase
import com.exapps.anistream.domain.usecase.SetPreferSummaryUseCase
import com.exapps.anistream.domain.usecase.SetWatchStatusUseCase
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
    private val observeWatchEntryUseCase: ObserveWatchEntryUseCase,
    private val observePreferencesUseCase: ObservePreferencesUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    private val setWatchStatusUseCase: SetWatchStatusUseCase,
    private val setAnimeRatingUseCase: SetAnimeRatingUseCase,
    private val setPreferSummaryUseCase: SetPreferSummaryUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
        observeWatchEntry()
        observeSummaryPreference()
    }

    fun toggleWatchlist() {
        val details = _uiState.value.details ?: return
        viewModelScope.launch { toggleWatchlistUseCase(details) }
    }

    fun updatePreferSummary(enabled: Boolean) {
        viewModelScope.launch { setPreferSummaryUseCase(enabled) }
    }

    fun setWatchStatus(status: WatchStatus) {
        val details = _uiState.value.details ?: return
        viewModelScope.launch { setWatchStatusUseCase(details, status) }
    }

    fun setRating(rating: Int?) {
        val details = _uiState.value.details ?: return
        viewModelScope.launch { setAnimeRatingUseCase(details, rating) }
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
                            errorMessage = error.message ?: "تعذر تحميل تفاصيل العمل.",
                        )
                    }
                }
        }
    }

    private fun observeWatchEntry() {
        viewModelScope.launch {
            observeWatchEntryUseCase(route.slug).collect { entry ->
                _uiState.update { current ->
                    current.copy(
                        watchEntry = entry,
                        isWatchlisted = entry != null,
                    )
                }
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
    val watchEntry: WatchlistAnime? = null,
    val isWatchlisted: Boolean = false,
    val preferSummary: Boolean = false,
    val errorMessage: String? = null,
)
