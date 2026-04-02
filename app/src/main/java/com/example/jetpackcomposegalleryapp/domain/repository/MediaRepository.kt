package com.example.jetpackcomposegalleryapp.domain.repository

import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaAsset>>

    fun getFavorites(): Flow<List<MediaAsset>>

    suspend fun toggleFavourite(media: MediaAsset, isFavourite: Boolean)

}