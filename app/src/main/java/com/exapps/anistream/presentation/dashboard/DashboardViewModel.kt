package com.exapps.anistream.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.CatalogCategory
import com.exapps.anistream.domain.model.CatalogFilters
import com.exapps.anistream.domain.model.CatalogSort
import com.exapps.anistream.domain.model.EpisodeCard
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.SortDirection
import com.exapps.anistream.domain.usecase.GetCatalogUseCase
import com.exapps.anistream.domain.usecase.GetHomeFeedUseCase
import com.exapps.anistream.domain.usecase.ObservePlaybackHistoryUseCase
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
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val getCatalogUseCase: GetCatalogUseCase,
    private val searchAnimeUseCase: SearchAnimeUseCase,
    private val observePlaybackHistoryUseCase: ObservePlaybackHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeHistory()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val homeFeed = getHomeFeedUseCase()
                val catalog = getCatalogUseCase(filters = _uiState.value.catalogFilters)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        featuredEpisodes = homeFeed.featuredEpisodes,
                        latestEpisodes = homeFeed.latestEpisodes,
                        latestTitles = homeFeed.latestTitles,
                        catalog = catalog.items,
                        catalogPage = catalog.currentPage,
                        hasMoreCatalog = catalog.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "تعذر تحميل البيانات حالياً.",
                    )
                }
            }
        }
    }

    fun onCatalogSortSelected(sort: CatalogSort) {
        _uiState.update { it.copy(catalogFilters = it.catalogFilters.copy(sort = sort)) }
        reloadCatalog()
    }

    fun onCatalogDirectionToggle() {
        _uiState.update {
            it.copy(
                catalogFilters = it.catalogFilters.copy(
                    direction = if (it.catalogFilters.direction == SortDirection.ASC) {
                        SortDirection.DESC
                    } else {
                        SortDirection.ASC
                    },
                ),
            )
        }
        reloadCatalog()
    }

    fun loadMoreCatalog() {
        if (_uiState.value.isCatalogLoadingMore || !_uiState.value.hasMoreCatalog) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCatalogLoadingMore = true) }
            runCatching {
                getCatalogUseCase(
                    page = _uiState.value.catalogPage + 1,
                    filters = _uiState.value.catalogFilters,
                )
            }.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        isCatalogLoadingMore = false,
                        catalog = (it.catalog + page.items).distinctBy(AnimeCard::slug),
                        catalogPage = page.currentPage,
                        hasMoreCatalog = page.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCatalogLoadingMore = false,
                        errorMessage = error.message ?: "تعذر تحميل المزيد من الفهرس.",
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    isSearchLoadingMore = false,
                    searchPage = 1,
                    hasMoreSearchResults = false,
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(400)
            runCatching {
                searchAnimeUseCase(
                    query = query,
                    filters = _uiState.value.catalogFilters,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        searchResults = result.items,
                        isSearching = false,
                        searchPage = result.currentPage,
                        hasMoreSearchResults = result.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = error.message ?: "فشل البحث.",
                    )
                }
            }
        }
    }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        if (state.searchQuery.isBlank() || state.isSearchLoadingMore || !state.hasMoreSearchResults) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearchLoadingMore = true) }
            runCatching {
                searchAnimeUseCase(
                    query = state.searchQuery,
                    page = state.searchPage + 1,
                    filters = state.catalogFilters,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isSearchLoadingMore = false,
                        searchResults = (it.searchResults + result.items).distinctBy(AnimeCard::slug),
                        searchPage = result.currentPage,
                        hasMoreSearchResults = result.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearchLoadingMore = false,
                        errorMessage = error.message ?: "تعذر تحميل المزيد من النتائج.",
                    )
                }
            }
        }
    }

    private fun reloadCatalog() {
        if (_uiState.value.searchQuery.isBlank()) {
            refresh()
        } else {
            onSearchQueryChange(_uiState.value.searchQuery)
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            observePlaybackHistoryUseCase().collect { history ->
                _uiState.update { it.copy(continueWatching = history.take(10)) }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isCatalogLoadingMore: Boolean = false,
    val isSearchLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val featuredEpisodes: List<EpisodeCard> = emptyList(),
    val latestEpisodes: List<EpisodeCard> = emptyList(),
    val latestTitles: List<AnimeCard> = emptyList(),
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val catalog: List<AnimeCard> = emptyList(),
    val catalogPage: Int = 1,
    val hasMoreCatalog: Boolean = false,
    val searchResults: List<AnimeCard> = emptyList(),
    val searchPage: Int = 1,
    val hasMoreSearchResults: Boolean = false,
    val catalogCategories: List<CatalogCategory> = CatalogCategory.entries,
    val catalogFilters: CatalogFilters = CatalogFilters(),
    val errorMessage: String? = null,
)
