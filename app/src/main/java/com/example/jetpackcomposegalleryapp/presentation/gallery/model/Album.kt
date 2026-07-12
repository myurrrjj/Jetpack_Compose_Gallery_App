package com.example.jetpackcomposegalleryapp.presentation.gallery.model

import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset

data class Album(
    val name: String?,
    val mediaCount: Int,
    val coverMedia: MediaAsset
)
