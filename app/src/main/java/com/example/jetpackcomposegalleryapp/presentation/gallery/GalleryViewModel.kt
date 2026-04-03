package com.example.jetpackcomposegalleryapp.presentation.gallery

import androidx.lifecycle.viewModelScope
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.BaseViewModel
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : BaseViewModel<GalleryEvent, GalleryState, GalleryEffect>() {

    override fun createInitialState(): GalleryState = GalleryState()

    override fun handleEvent(event: GalleryEvent) {
        when (event) {
            is GalleryEvent.LoadMedia -> fetchMedia()
            is GalleryEvent.PermissionResult -> handlePermission(event.isGranted)
            is GalleryEvent.MediaClicked -> navigateToDetail(event.mediaId)
            is GalleryEvent.onTabSelected -> handleTabSelection(event.tab)
            is GalleryEvent.ToggleFavorite ->
                viewModelScope.launch {
                    mediaRepository.toggleFavourite(event.media, event.isFavorite)
                }


            is GalleryEvent.OpenInfoSheet -> fetchMediaDetails(event.media)

            is GalleryEvent.CloseInfoSheet -> {
                setState { copy(infoSheetState = InfoSheetState.Closed) }
            }


        }
    }

    private fun fetchMediaDetails(media: MediaAsset) {
        setState {
            copy(infoSheetState = InfoSheetState.Loading(media))
        }
        viewModelScope.launch {
            try {
                val details = mediaRepository.getMediaDetails(media.uriString, media.isVideo)
                setState {
                    copy(infoSheetState = InfoSheetState.Success(media, details))

                }
            }catch (e: Exception){
                setState {
                    copy(infoSheetState = InfoSheetState.Error(media,e.message?:"Unknown Error"))

                }
            }

        }
    }


    private fun handleTabSelection(tab: GalleryTab) {
        if (uiState.value.selectedTab == tab) return

        val filteredList = when (tab) {
            GalleryTab.ALL -> uiState.value.masterMediaList
            GalleryTab.VIDEOS -> uiState.value.masterMediaList.filter { it.isVideo }
                .toImmutableList()

            GalleryTab.ALBUMS -> persistentListOf()
        }

        setState {
            copy(
                selectedTab = tab,
                displayedMediaList = filteredList
            )
        }
    }

    private fun handlePermission(isGranted: Boolean) {
        setState { copy(hasPermission = isGranted) }
        if (isGranted) {
            fetchMedia()
        } else {
            setState { copy(isLoading = false, error = "Permission required to display media.") }
        }
    }

    private fun fetchMedia() {
        if (!uiState.value.hasPermission) {
            setEffect { GalleryEffect.RequestPermission }
            return
        }

        viewModelScope.launch {
            mediaRepository.getAllMedia()
                .onStart { setState { copy(isLoading = true, error = null) } }
                .catch { exception ->
                    setState { copy(isLoading = false, error = exception.message) }
                }
                .collect { media ->
                    val immutableMedia = media.toImmutableList()
                    val processedAlbums = immutableMedia
                        .groupBy { it.folderName }
                        .map { (folderName, items) ->
                            Album(name = folderName, items.size, items.first())
                        }
                        .sortedBy { it.name }
                        .toImmutableList()

                    setState {
                        copy(
                            isLoading = false,
                            masterMediaList = immutableMedia,
                            displayedMediaList = immutableMedia,
                            albums = processedAlbums
                        )
                    }
                }
        }
    }

    private fun navigateToDetail(mediaId: Long) {
        setEffect { GalleryEffect.NavigateToDetail(mediaId) }
    }

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            mediaRepository.getFavorites().collect { favorites ->
                setState { copy(favoriteMediaIds = favorites.map { it.id }.toSet()) }
            }
        }
    }

}