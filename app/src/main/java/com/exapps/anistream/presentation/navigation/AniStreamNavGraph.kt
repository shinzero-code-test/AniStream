package com.exapps.anistream.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.exapps.anistream.presentation.dashboard.DashboardScreen
import com.exapps.anistream.presentation.dashboard.DashboardViewModel
import com.exapps.anistream.presentation.details.DetailsScreen
import com.exapps.anistream.presentation.details.DetailsViewModel
import com.exapps.anistream.presentation.player.PlayerScreen
import com.exapps.anistream.presentation.player.PlayerViewModel

@Composable
fun AniStreamNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
    ) {
        composable<DashboardRoute> {
            val viewModel = hiltViewModel<DashboardViewModel>()
            DashboardScreen(
                viewModel = viewModel,
                onOpenDetails = { slug -> navController.navigate(DetailsRoute(slug = slug)) },
                onOpenEpisode = { titleSlug, episodeNumber ->
                    navController.navigate(
                        PlayerRoute(
                            titleSlug = titleSlug,
                            episodeNumber = episodeNumber,
                        ),
                    )
                },
            )
        }

        composable<DetailsRoute> {
            val viewModel = hiltViewModel<DetailsViewModel>()
            DetailsScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
                onPlayEpisode = { titleSlug, episodeNumber ->
                    navController.navigate(
                        PlayerRoute(
                            titleSlug = titleSlug,
                            episodeNumber = episodeNumber,
                        ),
                    )
                },
            )
        }

        composable<PlayerRoute> {
            val viewModel = hiltViewModel<PlayerViewModel>()
            PlayerScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
            )
        }
    }
}
