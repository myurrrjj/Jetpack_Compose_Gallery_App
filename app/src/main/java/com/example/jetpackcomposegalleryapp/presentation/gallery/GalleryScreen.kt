package com.example.jetpackcomposegalleryapp.presentation.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.core.util.rememberScrollingUp
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ContextualSelectionBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.EmptyStateView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.FloatingGalleryBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTopBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaItemCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonClusterCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.galleryZoomGesture
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.Album
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.OthersSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScreenTarget(
    val album: Album?, val person: PersonCluster?
)



@Suppress("EffectKeys")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: GalleryViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val isScrollingUp = rememberScrollingUp(gridState)
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentState = state.contentState
    val configState = state.configState
    val infoSheetState = state.infoSheetState
    val interactionState = state.interactionState

    BackHandler(enabled = interactionState.selectionMode || interactionState.openedAlbum != null || interactionState.openedPersonCluster != null) {
        if (interactionState.selectionMode) {
            viewModel.setEvent(GalleryEvent.ExitSelectionMode)
        } else if (interactionState.openedAlbum != null) {
            viewModel.setEvent(GalleryEvent.CloseAlbum)
        } else if (interactionState.openedPersonCluster != null) {
            viewModel.setEvent(GalleryEvent.ClosePerson)
        }
    }

    val scrollBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val mediaCount = when (interactionState.selectedTab) {
        GalleryTab.ALBUMS -> contentState.albums.size
        else -> contentState.displayedMediaList.size
    }

    LaunchedEffect(Unit) {
        if (contentState.masterMediaList.isEmpty()) {
            viewModel.setEvent(GalleryEvent.LoadMedia)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection), topBar = {
            if (interactionState.selectionMode) {
                ContextualSelectionBar(
                    selectedCount = interactionState.selectedMediaIds.size,
                    totalDisplayedCount = contentState.displayedMediaList.size,
                    onCloseClick = { viewModel.setEvent(GalleryEvent.ExitSelectionMode) },
                    onSelectAllClick = { viewModel.setEvent(GalleryEvent.SelectAll) },
                    onClearSelectionClick = { viewModel.setEvent(GalleryEvent.ClearSelection) },
                    scrollBehavior = scrollBehaviour
                )
            } else {
                GalleryTopBar(
                    mediaCount = mediaCount, scrollBehaviour = scrollBehaviour, onSettingsClick = {
                        viewModel.setEvent(
                            GalleryEvent.OnSettingsClicked
                        )
                    })
            }
        }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AnimatedContent(
                targetState = ScreenTarget(interactionState.openedAlbum, interactionState.openedPersonCluster),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "GalleryToDetail"
            ) { target ->
                val galleryToDetailScope = this@AnimatedContent
                if (target.album != null) {
                    with(sharedTransitionScope) {
                        AlbumDetailView(
                            album = target.album,
                            selectionMode = interactionState.selectionMode,
                            mediaList = contentState.displayedMediaList,
                            onBackClick = { viewModel.setEvent(GalleryEvent.CloseAlbum) },
                            onMediaClick = { id -> viewModel.setEvent(GalleryEvent.MediaClicked(id)) },
                            modifier = Modifier.padding(top = 8.dp),
                            selectedMediaIds = interactionState.selectedMediaIds,
                            onToggleSelection = { mediaId ->
                                viewModel.setEvent(GalleryEvent.ToggleMediaSelection(mediaId))
                            },
                            onEnterSelectionMode = {
                                viewModel.setEvent(GalleryEvent.EnterSelectionMode)
                            },
                            albumAnimatedVisibilityScope = galleryToDetailScope,
                            tabAnimatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else if (target.person != null) {
                    with(sharedTransitionScope) {
                        PersonDetailView(
                            cluster = target.person,
                            selectionMode = interactionState.selectionMode,
                            mediaList = contentState.displayedMediaList,
                            onBackClick = { viewModel.setEvent(GalleryEvent.ClosePerson) },
                            onUpdateName = { id, name ->
                                viewModel.setEvent(GalleryEvent.UpdatePersonName(id, name))
                            },
                            onMediaClick = { id -> viewModel.setEvent(GalleryEvent.MediaClicked(id)) },
                            modifier = Modifier.padding(top = 8.dp),
                            selectedMediaIds = interactionState.selectedMediaIds,
                            onToggleSelection = { mediaId ->
                                viewModel.setEvent(GalleryEvent.ToggleMediaSelection(mediaId))
                            },
                            onEnterSelectionMode = { viewModel.setEvent(GalleryEvent.EnterSelectionMode) },
                            personAnimatedVisibilityScope = galleryToDetailScope,
                            tabAnimatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = interactionState.selectedTab, transitionSpec = {
                                fadeIn() togetherWith fadeOut()

                            }, label = "TabContent"
                        ) { tab ->
                            when {
                                configState.isLoading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                !configState.hasPermission -> {
                                    EmptyStateView(
                                        title = "Access Required",
                                        description = "To display your beautiful memories, we need access to your device's photos and videos.",
                                        buttonText = "Grant Permission",
                                        onClick = onRequestPermission
                                    )
                                }

                                (tab == GalleryTab.ALBUMS && contentState.albums.isEmpty()) || (tab != GalleryTab.ALBUMS && tab != GalleryTab.OTHERS && contentState.displayedMediaList.isEmpty()) -> {
                                    EmptyStateView(
                                        title = "No Content Found",
                                        description = "Your gallery section is empty. Take some photos to get started!",
                                        buttonText = "Refresh",
                                        onClick = { viewModel.setEvent(GalleryEvent.LoadMedia) })
                                }

                                else -> {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = if (tab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.PEOPLE) 140.dp else if (tab == GalleryTab.ALBUMS) 150.dp else configState.currentViewMode.minCellSizeDp.dp),
                                        contentPadding = PaddingValues(
                                            top = 8.dp,
                                            start = 2.dp,
                                            bottom = WindowInsets.systemBars.asPaddingValues()
                                                .calculateBottomPadding() + 100.dp,
                                            end = 2.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .galleryZoomGesture(
                                                currentMode = configState.currentViewMode,
                                                onViewModeChanged = { newMode ->
                                                    viewModel.setEvent(
                                                        GalleryEvent.ChangeViewMode(
                                                            newMode
                                                        )
                                                    )
                                                })
                                    ) {
                                        if (tab == GalleryTab.OTHERS) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = 16.dp, vertical = 12.dp
                                                        ), contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            OthersSelection.entries.forEach { selection ->
                                                                val isSelected =
                                                                    interactionState.othersSelection == selection
                                                                val containerColor by animateColorAsState(
                                                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                    label = "SegmentContainerColor"
                                                                )
                                                                val contentColor by animateColorAsState(
                                                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    label = "SegmentContentColor"
                                                                )

                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(
                                                                            CircleShape
                                                                        )
                                                                        .background(containerColor)
                                                                        .bouncyClick {
                                                                            viewModel.setEvent(
                                                                                GalleryEvent.OnOthersSelectionChanged(
                                                                                    selection
                                                                                )
                                                                            )
                                                                        }
                                                                        .padding(
                                                                            horizontal = 24.dp,
                                                                            vertical = 10.dp
                                                                        ),
                                                                    contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = selection.title,
                                                                        color = contentColor,
                                                                        style = MaterialTheme.typography.labelLarge,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        val isOthersEmpty =
                                            tab == GalleryTab.OTHERS && ((interactionState.othersSelection == OthersSelection.PEOPLE && contentState.peopleClusters.isEmpty()) || (interactionState.othersSelection == OthersSelection.FAVOURITES && contentState.displayedMediaList.isEmpty()))
                                        if (isOthersEmpty) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                val isPeopleTab =
                                                    interactionState.othersSelection == OthersSelection.PEOPLE
                                                EmptyStateView(
                                                    title = if (isPeopleTab) "Find People" else "No Favourites",
                                                    description = if (isPeopleTab) "Plug in your device to scan your gallery for faces." else "No items found.",
                                                    buttonText = if (isPeopleTab) "Start Indexing" else "Refresh",
                                                    onClick = {
                                                        if (isPeopleTab) viewModel.setEvent(
                                                            GalleryEvent.StartFaceIndexing
                                                        )
                                                        else viewModel.setEvent(GalleryEvent.LoadMedia)
                                                    },
                                                    modifier = Modifier.padding(top = 64.dp)
                                                )
                                            }
                                        } else {
                                            if (tab == GalleryTab.ALBUMS) {
                                                items(
                                                    count = contentState.albums.size, key = { index ->
                                                        contentState.albums[index].name ?: "Unknown"
                                                    }) { index ->
                                                    with(sharedTransitionScope) {
                                                        AlbumCard(
                                                            album = contentState.albums[index],
                                                            albumAnimatedVisibilityScope = galleryToDetailScope,
                                                            tabAnimatedVisibilityScope = animatedVisibilityScope,
                                                            onClick = {
                                                                viewModel.setEvent(
                                                                    GalleryEvent.OpenAlbum(
                                                                        contentState.albums[index]
                                                                    )
                                                                )
                                                            },
                                                            modifier = Modifier.animateItem()
                                                        )
                                                    }
                                                }
                                            } else if (tab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.PEOPLE) {
                                                items(
                                                    count = contentState.peopleClusters.size,
                                                    key = { index -> contentState.peopleClusters[index].id }) { index ->
                                                    val cluster = contentState.peopleClusters[index]
                                                    val coverMedia =
                                                        contentState.masterMediaList.find { it.id == cluster.coverMediaId }
                                                    with(sharedTransitionScope) {
                                                        PersonClusterCard(
                                                            cluster = cluster,
                                                            coverMedia = coverMedia,
                                                            onClick = {
                                                                viewModel.setEvent(
                                                                    GalleryEvent.OpenPerson(
                                                                        cluster
                                                                    )
                                                                )
                                                            },
                                                            personAnimatedVisibilityScope = galleryToDetailScope,
                                                            tabAnimatedVisibilityScope = animatedVisibilityScope,
                                                            modifier = Modifier.animateItem()
                                                        )
                                                    }
                                                }
                                            } else {
                                                contentState.groupedMedia.forEach { (groupKey, mediaItems) ->
                                                    if (mediaItems.isNotEmpty()) {
                                                        item(
                                                            key = "header_$groupKey",
                                                            span = { GridItemSpan(maxLineSpan) },
                                                            contentType = "contentType1"
                                                        ) {
                                                            Text(
                                                                text = configState.currentViewMode.formatHeader(
                                                                    mediaItems.first().dateAdded
                                                                ),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                                modifier = Modifier
                                                                    .padding(
                                                                        horizontal = 14.dp,
                                                                        vertical = 24.dp
                                                                    )
                                                                    .animateItem()
                                                            )
                                                        }
                                                        items(
                                                            count = mediaItems.size,
                                                            contentType = { index -> if (mediaItems[index].isVideo) "video" else "photo" },
                                                            key = { index -> mediaItems[index].id }) { index ->
                                                            val mediaItem = mediaItems[index]
                                                            val isSelected =
                                                                interactionState.selectedMediaIds.contains(
                                                                    mediaItem.id
                                                                )
                                                            val currView = configState.currentViewMode
                                                            MediaItemCard(
                                                                size = when (currView) {
                                                                    GalleryViewMode.DAY -> 400
                                                                    GalleryViewMode.MONTH -> 150
                                                                    GalleryViewMode.YEAR -> 20
                                                                },
                                                                media = mediaItem,
                                                                isSelected = isSelected,
                                                                selectionMode = interactionState.selectionMode,
                                                                onClick = {
                                                                    if (interactionState.selectionMode) {
                                                                        viewModel.setEvent(
                                                                            GalleryEvent.ToggleMediaSelection(
                                                                                mediaItem.id
                                                                            )
                                                                        )
                                                                    } else {
                                                                        viewModel.setEvent(
                                                                            GalleryEvent.MediaClicked(
                                                                                mediaItem.id
                                                                            )
                                                                        )
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    if (!interactionState.selectionMode) {
                                                                        viewModel.setEvent(
                                                                            GalleryEvent.EnterSelectionMode
                                                                        )
                                                                        viewModel.setEvent(
                                                                            GalleryEvent.ToggleMediaSelection(
                                                                                mediaItem.id
                                                                            )
                                                                        )
                                                                    }
                                                                },
                                                                sharedTransitionScope =
                                                                    sharedTransitionScope,
                                                                tabAnimatedVisibilityScope =
                                                                    animatedVisibilityScope,
                                                                albumAnimatedVisibilityScope =
                                                                    galleryToDetailScope,
                                                                modifier = Modifier.animateItem()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val isMainGallery = interactionState.openedAlbum == null && interactionState.openedPersonCluster == null
            val hasContent = if (interactionState.selectedTab == GalleryTab.ALBUMS) {
                contentState.albums.isNotEmpty()
            } else if (interactionState.selectedTab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.PEOPLE) {
                contentState.peopleClusters.isNotEmpty()
            } else {
                contentState.displayedMediaList.isNotEmpty()
            }
            val shouldShowBar by remember(hasContent, isScrollingUp) {
                derivedStateOf { !hasContent || isScrollingUp }
            }
            val bottomBarVisible = interactionState.selectionMode || (isMainGallery && shouldShowBar)

            val barOffset by animateDpAsState(
                targetValue = if (bottomBarVisible) 0.dp else 150.dp, label = "barOffset"
            )
            val barAlpha by animateFloatAsState(
                targetValue = if (bottomBarVisible) 1f else 0f, label = "barAlpha"
            )

            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues()
                            .calculateBottomPadding() + 24.dp
                    )
                    .graphicsLayer {
                        translationY = barOffset.toPx()
                        alpha = barAlpha
                    }
                    .zIndex(1f)) {
                AnimatedContent(
                    targetState = interactionState.selectionMode, label = "BottomBarSwap"
                ) { isSelectionMode ->
                    if (isSelectionMode) {
                        val selectedMedia =contentState.masterMediaList.filter { it.id in interactionState.selectedMediaIds }
                        DetailFloatingBar(
                            isFavorite = false, onActionClick = { action ->
                                viewModel.setEvent(
                                    GalleryEvent.PerformMediaAction(
                                        action, selectedMedia
                                    )
                                )
                            }, allowedActions = listOfNotNull(
                                DetailAction.SHARE,
                                DetailAction.FAVOURITE,
                                DetailAction.DELETE,
                                if (selectedMedia.none { it.isVideo }) {
                                    DetailAction.COPY
                                } else null
                            )
                        )
                    } else if (isMainGallery) {
                        with(sharedTransitionScope) {
                            FloatingGalleryBar(
                                selectedTab = interactionState.selectedTab, onClick = {
                                    coroutineScope.launch {
                                        gridState.animateScrollToItem(0)
                                    }
                                }, onTabSelected = { tab ->

                                    viewModel.setEvent(
                                        GalleryEvent.OnTabSelected(tab)
                                    )
                                    coroutineScope.launch {
                                        delay(100)
                                        gridState.animateScrollToItem(0)
                                    }
                                }, modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    zIndexInOverlay = 1f
                                )
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}