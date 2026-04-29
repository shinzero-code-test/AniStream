package com.exapps.anistream.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exapps.anistream.domain.model.AnimeCard
import com.exapps.anistream.domain.model.CatalogCategory
import com.exapps.anistream.domain.model.CatalogFilters
import com.exapps.anistream.domain.model.CatalogSort
import com.exapps.anistream.domain.model.SortDirection
import com.exapps.anistream.domain.usecase.GetCatalogUseCase
import com.exapps.anistream.presentation.navigation.CatalogRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCatalogUseCase: GetCatalogUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CatalogRoute>()
    private val category = CatalogCategory.fromPath(route.categoryPath)

    private val _uiState = MutableStateFlow(CatalogUiState(category = category))
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                getCatalogUseCase(
                    category = category,
                    filters = _uiState.value.filters,
                )
            }.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = page.items,
                        page = page.currentPage,
                        hasMore = page.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "تعذر تحميل القائمة.",
                    )
                }
            }
        }
    }

    fun onSortSelected(sort: CatalogSort) {
        _uiState.update { it.copy(filters = it.filters.copy(sort = sort)) }
        refresh()
    }

    fun onDirectionToggle() {
        _uiState.update {
            it.copy(
                filters = it.filters.copy(
                    direction = if (it.filters.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC,
                ),
            )
        }
        refresh()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                getCatalogUseCase(
                    category = category,
                    page = state.page + 1,
                    filters = state.filters,
                )
            }.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        items = (it.items + page.items).distinctBy(AnimeCard::slug),
                        page = page.currentPage,
                        hasMore = page.hasNextPage,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "تعذر تحميل المزيد.",
                    )
                }
            }
        }
    }
}

data class CatalogUiState(
    val category: CatalogCategory,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val items: List<AnimeCard> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val filters: CatalogFilters = CatalogFilters(),
    val errorMessage: String? = null,
)
