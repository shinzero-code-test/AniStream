package com.exapps.anistream.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.usecase.GetCatalogUseCase
import com.exapps.anistream.domain.usecase.GetHomeFeedUseCase
import com.exapps.anistream.domain.usecase.SearchAnimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getCatalog: GetCatalogUseCase,
    private val searchAnime: SearchAnimeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val homeFeed = getHomeFeed()
                val catalog = getCatalog()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        featuredEpisodes = homeFeed.featuredEpisodes,
                        latestEpisodes = homeFeed.latestEpisodes,
                        latestTitles = homeFeed.latestTitles,
                        catalog = catalog.items,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load AniStream.",
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(400)
            runCatching { searchAnime(query).items }
                .onSuccess { items ->
                    _uiState.update { it.copy(searchResults = items, isSearching = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = error.message ?: "Search failed.",
                        )
                    }
                }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val featuredEpisodes: List<EpisodeCard> = emptyList(),
    val latestEpisodes: List<EpisodeCard> = emptyList(),
    val latestTitles: List<AnimeCard> = emptyList(),
    val catalog: List<AnimeCard> = emptyList(),
    val searchResults: List<AnimeCard> = emptyList(),
    val errorMessage: String? = null,
)
