package com.exapps.anistream.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.usecase.GetEpisodeStreamUseCase
import com.exapps.anistream.domain.usecase.SavePlaybackHistoryUseCase
import com.exapps.anistream.presentation.navigation.PlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEpisodeStream: GetEpisodeStreamUseCase,
    private val savePlaybackHistory: SavePlaybackHistoryUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlayerRoute>()
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadStream()
    }

    fun persistPlayback(positionMs: Long) {
        val stream = _uiState.value.stream ?: return
        viewModelScope.launch {
            savePlaybackHistory(stream, positionMs)
        }
    }

    private fun loadStream() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getEpisodeStream(route.titleSlug, route.episodeNumber) }
                .onSuccess { stream ->
                    _uiState.update { it.copy(isLoading = false, stream = stream) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to resolve the player source.",
                        )
                    }
                }
        }
    }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val stream: EpisodeStream? = null,
    val errorMessage: String? = null,
)
