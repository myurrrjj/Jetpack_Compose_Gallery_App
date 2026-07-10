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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.core.util.rememberScrollingUp
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ContextualSelectionBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.FloatingGalleryBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTopBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaItemCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonClusterCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.galleryZoomGesture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScreenTarget(
    val album: Album?, val person: PersonCluster?
)

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.primary)
                .bouncyClick(onClick = onClick)
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

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

    BackHandler(enabled = state.selectionMode || state.openedAlbum != null || state.openedPersonCluster != null) {
        if (state.selectionMode) {
            viewModel.setEvent(GalleryEvent.ExitSelectionMode)
        } else if (state.openedAlbum != null) {
            viewModel.setEvent(GalleryEvent.CloseAlbum)
        } else if (state.openedPersonCluster != null) {
            viewModel.setEvent(GalleryEvent.ClosePerson)
        }
    }

    val scrollBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val mediaCount = when (state.selectedTab) {
        GalleryTab.ALBUMS -> state.albums.size
        else -> state.displayedMediaList.size
    }

    LaunchedEffect(Unit) {
        if (state.masterMediaList.isEmpty()) {
            viewModel.setEvent(GalleryEvent.LoadMedia)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection), topBar = {
            if (state.selectionMode) {
                ContextualSelectionBar(
                    selectedCount = state.selectedMediaIds.size,
                    totalDisplayedCount = state.displayedMediaList.size,
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
                targetState = ScreenTarget(state.openedAlbum, state.openedPersonCluster),
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
                            selectionMode = state.selectionMode,
                            mediaList = state.displayedMediaList,
                            onBackClick = { viewModel.setEvent(GalleryEvent.CloseAlbum) },
                            onMediaClick = { id -> viewModel.setEvent(GalleryEvent.MediaClicked(id)) },
                            modifier = Modifier.padding(top = 8.dp),
                            selectedMediaIds = state.selectedMediaIds,
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
                            selectionMode = state.selectionMode,
                            mediaList = state.displayedMediaList,
                            onBackClick = { viewModel.setEvent(GalleryEvent.ClosePerson) },
                            onUpdateName = { id, name ->
                                viewModel.setEvent(GalleryEvent.UpdatePersonName(id, name))
                            },
                            onMediaClick = { id -> viewModel.setEvent(GalleryEvent.MediaClicked(id)) },
                            modifier = Modifier.padding(top = 8.dp),
                            selectedMediaIds = state.selectedMediaIds,
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
                            targetState = state.selectedTab, transitionSpec = {
                                fadeIn() togetherWith fadeOut()

                            }, label = "TabContent"
                        ) { tab ->
                            when {
                                state.isLoading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                !state.hasPermission -> {
                                    EmptyStateView(
                                        title = "Access Required",
                                        description = "To display your beautiful memories, we need access to your device's photos and videos.",
                                        buttonText = "Grant Permission",
                                        onClick = onRequestPermission
                                    )
                                }

                                (tab == GalleryTab.ALBUMS && state.albums.isEmpty()) || (tab != GalleryTab.ALBUMS && tab != GalleryTab.OTHERS && state.displayedMediaList.isEmpty()) -> {
                                    EmptyStateView(
                                        title = "No Content Found",
                                        description = "Your gallery section is empty. Take some photos to get started!",
                                        buttonText = "Refresh",
                                        onClick = { viewModel.setEvent(GalleryEvent.LoadMedia) })
                                }

                                else -> {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = if (tab == GalleryTab.OTHERS && state.othersSelection == OthersSelection.PEOPLE) 140.dp else if (tab == GalleryTab.ALBUMS) 150.dp else state.currentViewMode.minCellSizeDp.dp),
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
                                                currentMode = state.currentViewMode,
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
                                                                    state.othersSelection == selection
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
                                            tab == GalleryTab.OTHERS && ((state.othersSelection == OthersSelection.PEOPLE && state.peopleClusters.isEmpty()) || (state.othersSelection == OthersSelection.FAVOURITES && state.displayedMediaList.isEmpty()))
                                        if (isOthersEmpty) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                val isPeopleTab =
                                                    state.othersSelection == OthersSelection.PEOPLE
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
                                                    count = state.albums.size, key = { index ->
                                                        state.albums[index].name ?: "Unknown"
                                                    }) { index ->
                                                    with(sharedTransitionScope) {
                                                        AlbumCard(
                                                            album = state.albums[index],
                                                            albumAnimatedVisibilityScope = galleryToDetailScope,
                                                            tabAnimatedVisibilityScope = animatedVisibilityScope,
                                                            onClick = {
                                                                viewModel.setEvent(
                                                                    GalleryEvent.OpenAlbum(
                                                                        state.albums[index]
                                                                    )
                                                                )
                                                            },
                                                            modifier = Modifier.animateItem()
                                                        )
                                                    }
                                                }
                                            } else if (tab == GalleryTab.OTHERS && state.othersSelection == OthersSelection.PEOPLE) {
                                                items(
                                                    count = state.peopleClusters.size,
                                                    key = { index -> state.peopleClusters[index].id }) { index ->
                                                    val cluster = state.peopleClusters[index]
                                                    val coverMedia =
                                                        state.masterMediaList.find { it.id == cluster.coverMediaId }
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
                                                state.groupedMedia.forEach { (groupKey, mediaItems) ->
                                                    if (mediaItems.isNotEmpty()) {
                                                        item(
                                                            key = "header_$groupKey",
                                                            span = { GridItemSpan(maxLineSpan) },
                                                            contentType = "contentType1"
                                                        ) {
                                                            Text(
                                                                text = state.currentViewMode.formatHeader(
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
                                                                state.selectedMediaIds.contains(
                                                                    mediaItem.id
                                                                )
                                                            val currView = state.currentViewMode
                                                            MediaItemCard(
                                                                size = when (currView) {
                                                                    GalleryViewMode.DAY -> 400
                                                                    GalleryViewMode.MONTH -> 150
                                                                    GalleryViewMode.YEAR -> 20
                                                                },
                                                                media = mediaItem,
                                                                isSelected = isSelected,
                                                                selectionMode = state.selectionMode,
                                                                onClick = {
                                                                    if (state.selectionMode) {
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
                                                                    if (!state.selectionMode) {
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

            val isMainGallery = state.openedAlbum == null && state.openedPersonCluster == null
            val hasContent = if (state.selectedTab == GalleryTab.ALBUMS) {
                state.albums.isNotEmpty()
            } else if (state.selectedTab == GalleryTab.OTHERS && state.othersSelection == OthersSelection.PEOPLE) {
                state.peopleClusters.isNotEmpty()
            } else {
                state.displayedMediaList.isNotEmpty()
            }
            val shouldShowBar by remember(hasContent, isScrollingUp) {
                derivedStateOf { !hasContent || isScrollingUp }
            }
            val bottomBarVisible = state.selectionMode || (isMainGallery && shouldShowBar)

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
                    targetState = state.selectionMode, label = "BottomBarSwap"
                ) { isSelectionMode ->
                    if (isSelectionMode) {
                        val selectedMedia =state.masterMediaList.filter { it.id in state.selectedMediaIds }
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
                                selectedTab = state.selectedTab, onClick = {
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