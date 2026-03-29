package com.example.jetpackcomposegalleryapp.presentation.gallery

import androidx.lifecycle.viewModelScope
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.BaseViewModel
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
                    setState { copy(isLoading = false, mediaList = media) }
                }
        }
    }

    private fun navigateToDetail(mediaId: Long) {
        setEffect { GalleryEffect.NavigateToDetail(mediaId) }
    }
}