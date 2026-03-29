package com.example.jetpackcomposegalleryapp.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Gallery : Route

    @Serializable
    data class Detail(val mediaId: Long) : Route
}