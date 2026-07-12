package com.example.jetpackcomposegalleryapp.domain.usecase

import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import javax.inject.Inject

class GroupMediaUseCase @Inject constructor() {
    operator fun invoke(
        mediaList: List<MediaAsset>,
        viewMode: GalleryViewMode
    ): Map<String, List<MediaAsset>> {
        return mediaList.groupBy { asset ->
            viewMode.getGroupingKey(asset.dateAdded)
        }
    }
}