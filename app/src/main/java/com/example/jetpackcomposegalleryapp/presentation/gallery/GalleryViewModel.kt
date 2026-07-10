package com.example.jetpackcomposegalleryapp.presentation.gallery

//import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.BaseViewModel
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.domain.repository.FaceRepository
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import com.example.jetpackcomposegalleryapp.domain.repository.SettingsRepository
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.worker.FaceIndexingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val mediaRepository: MediaRepository,
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<GalleryEvent, GalleryState, GalleryEffect>() {
    private var isFirstSettingsLoad = true

    init {
        observeFavorites()
        observePeopleClusters()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->

                setState {
                    val newViewMode =
                        if (isFirstSettingsLoad) settings.defaultGalleryViewMode else currentViewMode
                    if (isFirstSettingsLoad && newViewMode != currentViewMode) {
                        regroupMedia(displayedMediaList, newViewMode)
                    }
                    copy(
                        currentViewMode = newViewMode,
                        autoPlayVideo = settings.autoPlayVideo
                    ).also { isFirstSettingsLoad = false }
                }
            }
        }
    }

    override fun createInitialState(): GalleryState = GalleryState()

    private fun handleOthersSelectionChanged(selection: OthersSelection) {
        if (uiState.value.othersSelection == selection) return
        val filteredList = when (selection) {
            OthersSelection.FAVOURITES -> uiState.value.favouriteMediaList
            OthersSelection.PEOPLE -> persistentListOf()
        }
        setState {
            copy(
                othersSelection = selection,
                displayedMediaList = filteredList,
                selectionMode = false,
                selectedMediaIds = emptySet()
            )
        }
        regroupMedia(filteredList, uiState.value.currentViewMode)
    }

    override fun handleEvent(event: GalleryEvent) {
        when (event) {
            is GalleryEvent.LoadMedia -> fetchMedia()
            is GalleryEvent.PermissionResult -> handlePermission(event.isGranted)
            is GalleryEvent.MediaClicked -> navigateToDetail(event.mediaId)
            is GalleryEvent.OnTabSelected -> handleTabSelection(event.tab)
            is GalleryEvent.OnOthersSelectionChanged -> handleOthersSelectionChanged(event.selection)
            is GalleryEvent.OpenPerson -> openPerson(event.cluster)
            GalleryEvent.ClosePerson -> closePerson()
            is GalleryEvent.UpdatePersonName -> updatePersonName(event.clusterId, event.newName)

            is GalleryEvent.PerformMediaAction -> handleMediaAction(
                event.action,
                event.mediaList
            )

            is GalleryEvent.OpenInfoSheet -> fetchMediaDetails(event.media)
            is GalleryEvent.CloseInfoSheet -> {
                setState { copy(infoSheetState = InfoSheetState.Closed) }
            }

            is GalleryEvent.OpenAlbum -> {
                val newDisplayedList = uiState.value.masterMediaList.filter {
                    (it.folderName ?: "Unknown") == event.album.name
                }.toPersistentList()
                setState {
                    copy(
                        openedAlbum = event.album,
                        displayedMediaList = newDisplayedList
                    )
                }
                regroupMedia(newDisplayedList, uiState.value.currentViewMode)
            }

            GalleryEvent.CloseAlbum -> {
                val newDisplayedList = when (uiState.value.selectedTab) {
                    GalleryTab.VIDEOS -> uiState.value.masterMediaList.filter { it.isVideo }
                        .toPersistentList()

                    GalleryTab.OTHERS -> if (uiState.value.othersSelection == OthersSelection.FAVOURITES) {
                        uiState.value.favouriteMediaList
                    } else persistentListOf()

                    else -> uiState.value.masterMediaList
                }
                setState {
                    copy(openedAlbum = null, displayedMediaList = newDisplayedList)
                }
                regroupMedia(newDisplayedList, uiState.value.currentViewMode)
            }

            is GalleryEvent.EnterSelectionMode -> {
                setState { copy(selectionMode = true, selectedMediaIds = emptySet()) }
            }

            GalleryEvent.ExitSelectionMode -> {
                setState { copy(selectionMode = false, selectedMediaIds = emptySet()) }
            }

            GalleryEvent.SelectAll -> {
                setState {
                    val allIds = when (selectedTab) {
                        GalleryTab.ALBUMS -> emptySet()
                        else -> displayedMediaList.map { it.id }.toSet()
                    }
                    copy(selectedMediaIds = allIds)
                }
            }

            GalleryEvent.ClearSelection -> {
                setState { copy(selectedMediaIds = emptySet()) }
            }

            is GalleryEvent.ToggleMediaSelection -> {
                setState {
                    val newSet = if (selectedMediaIds.contains(event.mediaId)) {
                        selectedMediaIds - event.mediaId
                    } else {
                        selectedMediaIds + event.mediaId
                    }

                    val keepSelectionMode = newSet.isNotEmpty()
                    copy(
                        selectedMediaIds = newSet,
                        selectionMode = if (keepSelectionMode) selectionMode else false
                    )
                }
            }

            is GalleryEvent.ChangeViewMode -> {
                setState {
                    copy(currentViewMode = event.viewMode)
                }
                regroupMedia(uiState.value.displayedMediaList, event.viewMode)
            }

            GalleryEvent.StartFaceIndexing -> startManualFaceIndexing()
            GalleryEvent.OnSettingsClicked -> {
                setEffect { GalleryEffect.NavigateToSettings }
            }
        }
    }

    private fun observePeopleClusters() {
        viewModelScope.launch {
            faceRepository.getAllClusters().collect { clusters ->
                setState { copy(peopleClusters = clusters.toImmutableList()) }
            }
        }
    }

    private fun openPerson(cluster: PersonCluster) {
        viewModelScope.launch {
            val mediaIds = faceRepository.getMediaIdsForCluster(cluster.id).toSet()
            val personMedia =
                uiState.value.masterMediaList.filter { it.id in mediaIds }.toPersistentList()

            setState {
                copy(
                    openedPersonCluster = cluster,
                    displayedMediaList = personMedia
                )
            }
            regroupMedia(personMedia, uiState.value.currentViewMode)
        }
    }

    private fun closePerson() {
        val newDisplayedList = when (uiState.value.selectedTab) {
            GalleryTab.VIDEOS -> uiState.value.masterMediaList.filter { it.isVideo }
                .toPersistentList()

            GalleryTab.OTHERS -> if (uiState.value.othersSelection == OthersSelection.FAVOURITES) {
                uiState.value.favouriteMediaList
            } else persistentListOf()

            else -> uiState.value.masterMediaList
        }
        setState {
            copy(openedPersonCluster = null, displayedMediaList = newDisplayedList)
        }
        regroupMedia(newDisplayedList, uiState.value.currentViewMode)
    }

    private fun updatePersonName(clusterId: Long, newName: String) {
        viewModelScope.launch {
            faceRepository.updateClusterName(clusterId, newName)
        }
    }

    private fun regroupMedia(mediaList: List<MediaAsset>, viewMode: GalleryViewMode) {
        viewModelScope.launch(Dispatchers.Default) {
            val grouped = mediaList.groupBy { asset ->
                viewMode.getGroupingKey(asset.dateAdded)
            }
            setState { copy(groupedMedia = grouped) }
        }
    }

    private fun handleMediaAction(action: DetailAction, explicitMedia: List<MediaAsset>) {
        val targetMedia = explicitMedia.ifEmpty {
            val selectedIds = uiState.value.selectedMediaIds
            uiState.value.masterMediaList.filter { it.id in selectedIds }
        }

        if (targetMedia.isEmpty()) return

        when (action) {
            DetailAction.COPY -> {
                val uris = targetMedia.map { it.uriString }
                setEffect { GalleryEffect.CopyMedia(uris) }
            }

            DetailAction.SHARE -> {
                val uris = targetMedia.map { it.uriString }
                val mimeType =
                    if (targetMedia.size == 1) targetMedia.first().mimeType else "*/*"
                setEffect { GalleryEffect.ShareMedia(uris, mimeType) }
            }

            DetailAction.DELETE -> {
                val uris = targetMedia.map { it.uriString }
                setEffect { GalleryEffect.DeleteMedia(uris) }
            }

            DetailAction.FAVOURITE -> {
                viewModelScope.launch {
                    val favoriteIds = uiState.value.favoriteMediaIds
                    val allAreFavorites = targetMedia.all { it.id in favoriteIds }

                    targetMedia.forEach { media ->
                        mediaRepository.toggleFavourite(media, !allAreFavorites)
                    }
                    setEvent(GalleryEvent.ExitSelectionMode)
                }
            }

            DetailAction.INFO -> {
                if (targetMedia.size == 1) {
                    fetchMediaDetails(targetMedia.first())
                }
            }

            DetailAction.EDIT -> {
                if (targetMedia.size == 1) {
                    val media = targetMedia.first()
                    setEffect { GalleryEffect.EditMedia(media.uriString, media.mimeType) }
                }
            }
        }
    }

    private fun fetchMediaDetails(media: MediaAsset) {
        setState { copy(infoSheetState = InfoSheetState.Loading(media)) }
        viewModelScope.launch {
            try {
                val details = mediaRepository.getMediaDetails(media.uriString, media.isVideo)
                setState { copy(infoSheetState = InfoSheetState.Success(media, details)) }
            } catch (e: Exception) {
                setState {
                    copy(
                        infoSheetState = InfoSheetState.Error(
                            media,
                            e.message ?: "Unknown Error"
                        )
                    )
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
            GalleryTab.OTHERS -> {
                when (uiState.value.othersSelection) {
                    OthersSelection.FAVOURITES -> uiState.value.favouriteMediaList
                    OthersSelection.PEOPLE -> persistentListOf()
                }
            }
        }

        setState {
            copy(
                selectedTab = tab,
                displayedMediaList = filteredList,
                selectionMode = false,
                selectedMediaIds = emptySet()
            )
        }
        regroupMedia(filteredList, uiState.value.currentViewMode)
    }

    private fun handlePermission(isGranted: Boolean) {
        setState { copy(hasPermission = isGranted) }
        if (isGranted) fetchMedia()
        else setState {
            copy(
                isLoading = false,
                error = "Permission required to display media."
            )
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
                        .groupBy { it.folderName ?: "Unknown" }
                        .map { (folderName, items) ->
                            Album(
                                name = folderName,
                                items.size,
                                items.first()
                            )
                        }
                        .sortedBy { it.name }
                        .toImmutableList()

                    setState {
                        val newDisplayedList = when {
                            openedAlbum != null -> {
                                immutableMedia.filter {
                                    (it.folderName ?: "Unknown") == openedAlbum?.name
                                }.toImmutableList()
                            }

                            openedPersonCluster != null -> {
                                displayedMediaList
                            }

                            else -> when (selectedTab) {
                                GalleryTab.ALL -> immutableMedia
                                GalleryTab.VIDEOS -> immutableMedia.filter { it.isVideo }
                                    .toImmutableList()

                                GalleryTab.ALBUMS -> persistentListOf()
                                GalleryTab.OTHERS -> {
                                    if (othersSelection == OthersSelection.FAVOURITES) favouriteMediaList
                                    else persistentListOf()
                                }
                            }
                        }
                        copy(
                            isLoading = false,
                            masterMediaList = immutableMedia,
                            displayedMediaList = newDisplayedList,
                            albums = processedAlbums
                        )
                    }
                    regroupMedia(
                        uiState.value.displayedMediaList,
                        uiState.value.currentViewMode
                    )
                }
        }
    }

    private fun navigateToDetail(mediaId: Long) {
        setEffect { GalleryEffect.NavigateToDetail(mediaId) }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            mediaRepository.getFavorites().collect { favourites ->
                val immutableFavourites = favourites.toImmutableList()
                val ids = favourites.map { it.id }.toSet()

                setState {
                    val newDisplayedList = when {
                        openedAlbum != null || openedPersonCluster != null -> displayedMediaList
                        selectedTab == GalleryTab.OTHERS && othersSelection == OthersSelection.FAVOURITES -> immutableFavourites
                        else -> displayedMediaList
                    }
                    copy(
                        favouriteMediaList = immutableFavourites,
                        favoriteMediaIds = ids,
                        displayedMediaList = newDisplayedList
                    )
                }
                regroupMedia(uiState.value.displayedMediaList, uiState.value.currentViewMode)
            }
        }
    }

    private fun startManualFaceIndexing() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<FaceIndexingWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "FaceIndexing",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}