package com.example.jetpackcomposegalleryapp.domain.repository

import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaAsset>>

}