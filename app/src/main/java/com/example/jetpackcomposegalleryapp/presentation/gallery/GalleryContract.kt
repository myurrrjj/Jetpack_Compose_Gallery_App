package com.example.jetpackcomposegalleryapp.presentation.gallery

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewSideEffect
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.DetailedMediaInfo
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.settings.SettingsEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class GalleryTab(val title: String) {
    ALL("All"), ALBUMS("Albums"), VIDEOS("Videos"), OTHERS("Others")

}

enum class OthersSelection(val title: String) {
    FAVOURITES("Favourites"), PEOPLE("People")
}


data class Album(
    val name: String?,
    val mediaCount: Int,
    val coverMedia: MediaAsset
)

data class GalleryState(
    val isLoading: Boolean = true,
    val masterMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val displayedMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val favouriteMediaList: ImmutableList<MediaAsset> = persistentListOf(),
    val albums: ImmutableList<Album> = persistentListOf(),
    val peopleClusters: ImmutableList<PersonCluster> = persistentListOf(),

    val openedAlbum: Album? = null,
    val openedPersonCluster: PersonCluster? = null,
    val error: String? = null,
    val hasPermission: Boolean = false,
    val selectedTab: GalleryTab = GalleryTab.ALL,
    val othersSelection: OthersSelection = OthersSelection.FAVOURITES,
    val autoPlayVideo : Boolean = true,

    val favoriteMediaIds: Set<Long> = emptySet(),
    val infoSheetState: InfoSheetState = InfoSheetState.Closed,
    val selectionMode: Boolean = false,
    val selectedMediaIds: Set<Long> = emptySet(),
    val currentViewMode: GalleryViewMode = GalleryViewMode.DAY,
    val groupedMedia: Map<String, List<MediaAsset>> = emptyMap()
) : ViewState

sealed class GalleryEvent : ViewEvent {
    object LoadMedia : GalleryEvent()
    data class PermissionResult(val isGranted: Boolean) : GalleryEvent()
    data class MediaClicked(val mediaId: Long) : GalleryEvent()
    data class OnTabSelected(val tab: GalleryTab) : GalleryEvent()
    data class OnOthersSelectionChanged(val selection: OthersSelection) : GalleryEvent()
    data class OpenPerson(val cluster: PersonCluster) : GalleryEvent()
    object ClosePerson : GalleryEvent()
    data class UpdatePersonName(val clusterId: Long, val newName: String) : GalleryEvent()
    data class PerformMediaAction(
        val action: DetailAction,
        val mediaList: List<MediaAsset> = emptyList()
    ) : GalleryEvent()

    data class OpenInfoSheet(val media: MediaAsset) : GalleryEvent()
    object CloseInfoSheet : GalleryEvent()

    data class OpenAlbum(val album: Album) : GalleryEvent()
    object CloseAlbum : GalleryEvent()

    object EnterSelectionMode : GalleryEvent()
    object ExitSelectionMode : GalleryEvent()
    data class ToggleMediaSelection(val mediaId: Long) : GalleryEvent()
    object SelectAll : GalleryEvent()
    object ClearSelection : GalleryEvent()
    data class ChangeViewMode(val viewMode: GalleryViewMode) : GalleryEvent()

    object StartFaceIndexing : GalleryEvent()
    object OnSettingsClicked : GalleryEvent()
}

sealed class GalleryEffect : ViewSideEffect {
    object RequestPermission : GalleryEffect()
    data class NavigateToDetail(val mediaId: Long) : GalleryEffect()

    data class ShareMedia(val uris: List<String>, val mimeType: String) : GalleryEffect()
    data class DeleteMedia(val uris: List<String>) : GalleryEffect()
    data class EditMedia(val uriString: String, val mimeType: String) : GalleryEffect()

    data class CopyMedia(val uris: List<String>) : GalleryEffect()
    object NavigateToSettings : GalleryEffect()
}

sealed interface InfoSheetState {
    object Closed : InfoSheetState
    data class Loading(val media: MediaAsset) : InfoSheetState
    data class Success(val media: MediaAsset, val details: DetailedMediaInfo) : InfoSheetState
    data class Error(val media: MediaAsset, val message: String) : InfoSheetState
}