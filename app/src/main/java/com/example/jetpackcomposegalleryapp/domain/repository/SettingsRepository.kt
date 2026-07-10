package com.example.jetpackcomposegalleryapp.domain.repository

import com.example.jetpackcomposegalleryapp.domain.model.AppSettings
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings : Flow<AppSettings>
    suspend fun updateAutoPlayVideos(enabled:Boolean)
    suspend fun updateDefaultGalleryViewMode(viewMode: GalleryViewMode)


}