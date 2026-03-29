package com.example.jetpackcomposegalleryapp.presentation.gallery

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewSideEffect
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset

data class GalleryState(
    val isLoading: Boolean = true,
    val mediaList: List<MediaAsset> = emptyList(),
    val error: String? = null,
    val hasPermission: Boolean = false,

    ) : ViewState

sealed class GalleryEvent : ViewEvent {
    object LoadMedia : GalleryEvent()
    data class PermissionResult(val isGranted: Boolean) : GalleryEvent()
    data class MediaClicked(val mediaId: Long) : GalleryEvent()


}

sealed class GalleryEffect : ViewSideEffect {
    object RequestPermission : GalleryEffect()
    data class NavigateToDetail(val mediaId:Long): GalleryEffect()

}
