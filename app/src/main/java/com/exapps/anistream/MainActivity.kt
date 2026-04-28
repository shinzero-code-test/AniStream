package com.exapps.anistream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.exapps.anistream.presentation.navigation.AniStreamNavGraph
import com.exapps.anistream.ui.theme.AniStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AniStreamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AniStreamNavGraph()
                }
            }
        }
    }
}
