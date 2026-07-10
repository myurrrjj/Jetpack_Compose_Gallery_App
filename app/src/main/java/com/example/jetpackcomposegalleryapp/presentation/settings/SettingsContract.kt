package com.example.jetpackcomposegalleryapp.presentation.settings

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewSideEffect
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.AppSettings
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryEvent

data class SettingsState(
    val isLoading: Boolean = true,
    val appSettings: AppSettings = AppSettings()
) : ViewState

sealed class SettingsEvent : ViewEvent {
    data class ToggleAutoPlay(val isEnabled: Boolean) : SettingsEvent()
    data class ChangeDefaultViewMode(val viewMode: GalleryViewMode) : SettingsEvent()
    object CloseSettings : SettingsEvent()
}

sealed class SettingsEffect : ViewSideEffect {
    object DismissSheet : SettingsEffect()
}