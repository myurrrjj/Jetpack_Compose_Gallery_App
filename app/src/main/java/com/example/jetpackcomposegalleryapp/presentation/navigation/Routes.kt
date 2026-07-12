package com.example.jetpackcomposegalleryapp.presentation.navigation

import com.example.jetpackcomposegalleryapp.trial.DirtyTestViewModel
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Gallery : Route

    @Serializable
    data object DirtyTest : Route
    @Serializable
    data class Detail(val mediaId: Long) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Search: Route
}