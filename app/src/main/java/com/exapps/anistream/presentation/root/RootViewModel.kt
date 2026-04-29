package com.exapps.anistream.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exapps.anistream.core.webview.Anime3rbSessionBridge
import com.exapps.anistream.core.webview.VisibleCloudflareChallengeSolver
import com.exapps.anistream.domain.usecase.ObservePreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val observePreferencesUseCase: ObservePreferencesUseCase,
    private val sessionBridge: Anime3rbSessionBridge,
    private val challengeSolver: VisibleCloudflareChallengeSolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observePreferencesUseCase(),
                challengeSolver.challengeRequired,
            ) { preferences, challengeRequired ->
                RootUiState(
                    dynamicColors = preferences.dynamicColors,
                    needsAnime3rbSession = challengeRequired || !sessionBridge.hasSession(),
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun syncAnime3rbSession(userAgent: String?) {
        val hasCookies = sessionBridge.syncFromWebView(userAgent)
        if (hasCookies) {
            challengeSolver.markSolved()
        }
        _uiState.update { it.copy(needsAnime3rbSession = !hasCookies) }
    }
}

data class RootUiState(
    val dynamicColors: Boolean = true,
    val needsAnime3rbSession: Boolean = true,
)
