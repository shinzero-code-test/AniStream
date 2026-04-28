package com.exapps.anistream.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object LibraryRoute

@Serializable
object SettingsRoute

@Serializable
data class DetailsRoute(val slug: String)

@Serializable
data class PlayerRoute(
    val titleSlug: String,
    val episodeNumber: Int,
)
