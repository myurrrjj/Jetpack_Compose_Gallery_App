package com.example.jetpackcomposegalleryapp.presentation.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.CustomVideoPlayer
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaInfoSheet
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ZoomableImage
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.rememberVideoPlayerState

@Composable
fun DetailScreen(
    initialMediaId: Long,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isUiVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val initialIndex = remember(initialMediaId, state.displayedMediaList) {
        state.displayedMediaList.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex, pageCount = { state.displayedMediaList.size })

    val currentMedia by remember {
        derivedStateOf {
            state.displayedMediaList.getOrNull(pagerState.currentPage)
        }
    }

    val isFavorite = remember(currentMedia, state.favoriteMediaIds) {
        currentMedia?.id?.let { state.favoriteMediaIds.contains(it) } ?: false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            key = { page -> state.displayedMediaList[page].id }) { page ->

            val media = state.displayedMediaList[page]
            val imageRequest = ImageRequest.Builder(context)
                .data(media.uriString)
                .placeholderMemoryCacheKey(media.uriString)
                .build()
            val isCurrentPage = pagerState.currentPage == page
            val isTransitionRunning = animatedVisibilityScope.transition.isRunning
            val transitionKey = if (isCurrentPage) "media_${media.id}" else "{ghost_key}"

            if (media.isVideo) {
                val exoPlayer = remember { ExoPlayer.Builder(context).build() }
                val videoState = rememberVideoPlayerState(player = exoPlayer)
                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                with(sharedTransitionScope) {

//                    if (isTransitionRunning){
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {

                        if (!isTransitionRunning) {
                            CustomVideoPlayer(
                                media = media,
                                animatedVisibilityScope = animatedVisibilityScope,
                                isCurrentPage = isCurrentPage,
                                exoPlayer = exoPlayer,
                                isVisible = isUiVisible,
                                videoState = videoState.value,
                                onVisibilityChanged = { isUiVisible = it },
                                autoPlay = state.autoPlayVideo,
//                                modifier = Modifier.zIndex(1f)
                            )

                        }
                        val imageAlpha by animateFloatAsState(
                            targetValue = if (videoState.value.isFirstFrameRendered) 0f else 1f,
                            animationSpec = tween(500),
                            label = "thumbnail_crossfade"
                        )
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = media.name,
                            contentScale = ContentScale.Fit,

                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = transitionKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    renderInOverlayDuringTransition = true,
//
                                )
                                .graphicsLayer { alpha = imageAlpha}


                        )

//                    }

                    }
                }
            } else {
                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center


                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = media.name,
                            contentScale = ContentScale.Fit,

                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = transitionKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    renderInOverlayDuringTransition = true,
//
                                )

                        )
                        if (!isTransitionRunning) {
                            ZoomableImage(
                                uriString = media.uriString,
                                contentDescription = media.name,
                                onTap = { isUiVisible = !isUiVisible },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f)
                            )
                        }

                    }
                }
            }
        }

        with(sharedTransitionScope) {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                    )
                    .zIndex(1f)
            ) {
                DetailFloatingBar(
                    isFavorite = isFavorite, onActionClick = { action ->
                        currentMedia?.let { media ->
                            viewModel.setEvent(
                                GalleryEvent.PerformMediaAction(
                                    action, listOf(media)
                                )
                            )
                        }
                    }, modifier = Modifier

                        .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        zIndexInOverlay = 1f
                    )
                )
            }
        }

        MediaInfoSheet(
            sheetState = state.infoSheetState,
            onDismiss = { viewModel.setEvent(GalleryEvent.CloseInfoSheet) })
    }
}