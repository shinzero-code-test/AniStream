package com.exapps.anistream.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exapps.anistream.domain.model.EpisodeStream
import com.exapps.anistream.domain.usecase.GetEpisodeStreamUseCase
import com.exapps.anistream.domain.usecase.ObservePreferencesUseCase
import com.exapps.anistream.domain.usecase.SavePlaybackHistoryUseCase
import com.exapps.anistream.presentation.navigation.PlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.LinkedHashSet
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEpisodeStreamUseCase: GetEpisodeStreamUseCase,
    private val savePlaybackHistoryUseCase: SavePlaybackHistoryUseCase,
    private val observePreferencesUseCase: ObservePreferencesUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlayerRoute>()
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private val failedServerIds = LinkedHashSet<String>()
    private var lastRecoveryServerId: String? = null

    init {
        loadStream()
        observePreferences()
    }

    fun persistPlayback(positionMs: Long) {
        val stream = _uiState.value.stream ?: return
        viewModelScope.launch {
            savePlaybackHistoryUseCase(stream, positionMs)
        }
    }

    fun handlePlaybackError() {
        val stream = _uiState.value.stream ?: return
        val currentServerId = stream.selectedServerId
        val shouldRetrySameServer = currentServerId != null && lastRecoveryServerId != currentServerId

        if (shouldRetrySameServer) {
            lastRecoveryServerId = currentServerId
            loadStream(
                preferredServerId = currentServerId,
                excludedServerIds = failedServerIds.toSet(),
                recovering = true,
            )
        } else {
            currentServerId?.let(failedServerIds::add)
            lastRecoveryServerId = null
            loadStream(
                preferredServerId = null,
                excludedServerIds = failedServerIds.toSet(),
                recovering = true,
            )
        }
    }

    private fun loadStream(
        preferredServerId: String? = null,
        excludedServerIds: Set<String> = emptySet(),
        recovering: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !recovering && it.stream == null,
                    isRecovering = recovering,
                    errorMessage = null,
                )
            }
            runCatching {
                getEpisodeStreamUseCase(
                    titleSlug = route.titleSlug,
                    episodeNumber = route.episodeNumber,
                    preferredServerId = preferredServerId,
                    excludedServerIds = excludedServerIds,
                )
            }
                .onSuccess { stream ->
                    lastRecoveryServerId = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRecovering = false,
                            stream = stream,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRecovering = false,
                            errorMessage = error.message ?: "تعذر تجهيز رابط المشاهدة.",
                        )
                    }
                }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            observePreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(autoPlayNext = preferences.autoPlayNext) }
            }
        }
    }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val isRecovering: Boolean = false,
    val stream: EpisodeStream? = null,
    val autoPlayNext: Boolean = true,
    val skipIntroSeconds: Int = 85,
    val errorMessage: String? = null,
)
