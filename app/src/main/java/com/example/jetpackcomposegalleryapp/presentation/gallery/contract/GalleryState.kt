package com.example.jetpackcomposegalleryapp.presentation.gallery.contract

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.presentation.gallery.InfoSheetState
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.Album
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.OthersSelection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GalleryUiConfig(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasPermission: Boolean = false,
    val currentViewMode: GalleryViewMode = GalleryViewMode.DAY,
    val autoPlayVideo: Boolean = true
)

data class MediaContentState(
    val masterMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val displayedMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val favouriteMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val favoriteMediaIds: Set<Long> = emptySet(),
    val groupedMedia: Map<String, List<MediaAsset>> = emptyMap(),
    val albums: ImmutableList<Album> = persistentListOf(),
    val peopleClusters: ImmutableList<PersonCluster> = persistentListOf()
)


data class InteractionState(
    val selectedTab: GalleryTab = GalleryTab.ALL,
    val othersSelection: OthersSelection = OthersSelection.FAVOURITES,
    val openedAlbum: Album? = null,
    val openedPersonCluster: PersonCluster? = null,
    val selectionMode: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet()
)

data class GalleryState(
    val configState: GalleryUiConfig = GalleryUiConfig(),
    val contentState: MediaContentState = MediaContentState(),
    val interactionState: InteractionState = InteractionState(),
    val infoSheetState: InfoSheetState = InfoSheetState.Closed
) : ViewState