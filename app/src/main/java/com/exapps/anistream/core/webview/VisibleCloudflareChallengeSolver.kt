package com.exapps.anistream.core.webview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisibleCloudflareChallengeSolver @Inject constructor() : CloudflareChallengeSolver {
    private val _challengeRequired = MutableStateFlow(false)
    val challengeRequired: StateFlow<Boolean> = _challengeRequired.asStateFlow()

    override fun markChallengeRequired() {
        _challengeRequired.value = true
    }

    fun markSolved() {
        _challengeRequired.value = false
    }
}
