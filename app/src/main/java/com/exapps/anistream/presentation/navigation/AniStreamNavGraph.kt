package com.exapps.anistream.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.util.UnstableApi
import com.exapps.anistream.R
import com.exapps.anistream.presentation.dashboard.DashboardScreen
import com.exapps.anistream.presentation.dashboard.DashboardViewModel
import com.exapps.anistream.presentation.details.DetailsScreen
import com.exapps.anistream.presentation.details.DetailsViewModel
import com.exapps.anistream.presentation.cloudflare.Anime3rbSessionScreen
import com.exapps.anistream.presentation.library.LibraryScreen
import com.exapps.anistream.presentation.library.LibraryViewModel
import com.exapps.anistream.presentation.player.PlayerScreen
import com.exapps.anistream.presentation.player.PlayerViewModel
import com.exapps.anistream.presentation.settings.SettingsScreen
import com.exapps.anistream.presentation.settings.SettingsViewModel

private enum class RootNavTarget {
    HOME,
    LIBRARY,
    SETTINGS,
}

private data class RootDestination(
    val target: RootNavTarget,
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

@Composable
@UnstableApi
fun AniStreamNavGraph() {
    AniStreamNavGraph(
        needsAnime3rbSession = false,
        onAnime3rbSessionReady = {},
    )
}

@Composable
@UnstableApi
fun AniStreamNavGraph(
    needsAnime3rbSession: Boolean,
    onAnime3rbSessionReady: (String?) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val rootDestinations = listOf(
        RootDestination(RootNavTarget.HOME, R.string.nav_home) { Icon(Icons.Rounded.Home, contentDescription = null) },
        RootDestination(RootNavTarget.LIBRARY, R.string.nav_library) { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
        RootDestination(RootNavTarget.SETTINGS, R.string.nav_settings) { Icon(Icons.Rounded.Settings, contentDescription = null) },
    )

    val showBottomBar = !needsAnime3rbSession &&
        rootDestinations.any { root ->
            when (root.target) {
                RootNavTarget.HOME -> destination?.hasRoute<DashboardRoute>() == true
                RootNavTarget.LIBRARY -> destination?.hasRoute<LibraryRoute>() == true
                RootNavTarget.SETTINGS -> destination?.hasRoute<SettingsRoute>() == true
            }
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    rootDestinations.forEach { root ->
                        val selected = when (root.target) {
                            RootNavTarget.HOME -> destination?.hasRoute<DashboardRoute>() == true
                            RootNavTarget.LIBRARY -> destination?.hasRoute<LibraryRoute>() == true
                            RootNavTarget.SETTINGS -> destination?.hasRoute<SettingsRoute>() == true
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                when (root.target) {
                                    RootNavTarget.HOME -> navController.navigate(DashboardRoute) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }

                                    RootNavTarget.LIBRARY -> navController.navigate(LibraryRoute) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }

                                    RootNavTarget.SETTINGS -> navController.navigate(SettingsRoute) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { root.icon() },
                            label = { Text(stringResource(id = root.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (needsAnime3rbSession) {
            Anime3rbSessionScreen(
                modifier = Modifier.padding(innerPadding),
                onSessionReady = onAnime3rbSessionReady,
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = DashboardRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<DashboardRoute> {
                    val viewModel = hiltViewModel<DashboardViewModel>()
                    DashboardScreen(
                        viewModel = viewModel,
                        onOpenDetails = { slug -> navController.navigate(DetailsRoute(slug = slug)) },
                        onOpenEpisode = { titleSlug, episodeNumber ->
                            navController.navigate(
                                PlayerRoute(titleSlug = titleSlug, episodeNumber = episodeNumber),
                            )
                        },
                    )
                }

                composable<LibraryRoute> {
                    val viewModel = hiltViewModel<LibraryViewModel>()
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenDetails = { slug -> navController.navigate(DetailsRoute(slug = slug)) },
                        onOpenEpisode = { titleSlug, episodeNumber ->
                            navController.navigate(
                                PlayerRoute(titleSlug = titleSlug, episodeNumber = episodeNumber),
                            )
                        },
                    )
                }

                composable<SettingsRoute> {
                    val viewModel = hiltViewModel<SettingsViewModel>()
                    SettingsScreen(viewModel = viewModel)
                }

                composable<DetailsRoute> {
                    val viewModel = hiltViewModel<DetailsViewModel>()
                    DetailsScreen(
                        viewModel = viewModel,
                        onBack = navController::popBackStack,
                        onPlayEpisode = { titleSlug, episodeNumber ->
                            navController.navigate(
                                PlayerRoute(titleSlug = titleSlug, episodeNumber = episodeNumber),
                            )
                        },
                        onOpenDetails = { slug -> navController.navigate(DetailsRoute(slug = slug)) },
                    )
                }

                composable<PlayerRoute> {
                    val viewModel = hiltViewModel<PlayerViewModel>()
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = navController::popBackStack,
                        onOpenEpisode = { titleSlug, episodeNumber ->
                            navController.navigate(
                                PlayerRoute(titleSlug = titleSlug, episodeNumber = episodeNumber),
                            )
                        },
                    )
                }
            }
        }
    }
}
