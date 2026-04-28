package com.exapps.anistream.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exapps.anistream.domain.model.PlaybackHistory
import com.exapps.anistream.domain.model.WatchlistAnime
import com.exapps.anistream.domain.usecase.ObservePlaybackHistoryUseCase
import com.exapps.anistream.domain.usecase.ObserveWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val observeWatchlistUseCase: ObserveWatchlistUseCase,
    private val observePlaybackHistoryUseCase: ObservePlaybackHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeWatchlistUseCase().collect { watchlist ->
                _uiState.update { it.copy(watchlist = watchlist) }
            }
        }

        viewModelScope.launch {
            observePlaybackHistoryUseCase().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }
}

data class LibraryUiState(
    val watchlist: List<WatchlistAnime> = emptyList(),
    val history: List<PlaybackHistory> = emptyList(),
)
