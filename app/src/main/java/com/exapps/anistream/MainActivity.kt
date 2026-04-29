package com.exapps.anistream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.exapps.anistream.presentation.navigation.AniStreamNavGraph
import com.exapps.anistream.presentation.root.RootViewModel
import com.exapps.anistream.ui.theme.AniStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@UnstableApi
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val rootState = rootViewModel.uiState.collectAsStateWithLifecycle().value
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AniStreamTheme(dynamicColor = rootState.dynamicColors) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AniStreamNavGraph()
                    }
                }
            }
        }
    }
}
