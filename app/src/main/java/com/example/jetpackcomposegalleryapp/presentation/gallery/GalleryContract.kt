package com.example.jetpackcomposegalleryapp.presentation.gallery

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewSideEffect
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.DetailedMediaInfo
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


data class Album(
    val name: String?,
    val mediaCount: Int,
    val coverMedia: MediaAsset
)

data class GalleryState(
    val isLoading: Boolean = true,
    val masterMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val displayedMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val albums: ImmutableList<Album> = persistentListOf(),
    val error: String? = null,
    val hasPermission: Boolean = false,
    val selectedTab: GalleryTab = GalleryTab.ALL,
    val favoriteMediaIds: Set<Long> = emptySet(),
    val infoSheetState: InfoSheetState = InfoSheetState.Closed

) : ViewState

sealed interface InfoSheetState {
    object Closed : InfoSheetState
    data class Loading(val media: MediaAsset) : InfoSheetState
    data class Success(val media: MediaAsset, val details: DetailedMediaInfo) : InfoSheetState
    data class Error(val media: MediaAsset, val message: String) : InfoSheetState
}

sealed class GalleryEvent : ViewEvent {
    object LoadMedia : GalleryEvent()
    data class PermissionResult(val isGranted: Boolean) : GalleryEvent()
    data class MediaClicked(val mediaId: Long) : GalleryEvent()
    data class onTabSelected(val tab: GalleryTab) : GalleryEvent()
    data class ToggleFavorite(val media: MediaAsset, val isFavorite: Boolean) : GalleryEvent()

    data class OpenInfoSheet(val media: MediaAsset) : GalleryEvent()
    object CloseInfoSheet : GalleryEvent()
}

sealed class GalleryEffect : ViewSideEffect {
    object RequestPermission : GalleryEffect()
    data class NavigateToDetail(val mediaId: Long) : GalleryEffect()

}
