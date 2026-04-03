package com.example.jetpackcomposegalleryapp.presentation.gallery

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jetpackcomposegalleryapp.core.util.MediaIntents
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.CustomVideoPlayer
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaInfoSheet

@Composable
fun DetailScreen(
    initialMediaId: Long,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(
    )
    var isUiVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val initialIndex = remember(initialMediaId, state.displayedMediaList) {
        state.displayedMediaList.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex, pageCount = { state.displayedMediaList.size })

    val currentMedia = remember(pagerState.currentPage, state.masterMediaList) {
        if (state.masterMediaList.isNotEmpty()) {
            state.masterMediaList[pagerState.currentPage]
        } else {
            null
        }
    }

    val isFavorite = remember(currentMedia, state.favoriteMediaIds) {
        currentMedia?.id?.let { state.favoriteMediaIds.contains(it) } ?: false
    }

    val deleteLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setEvent(GalleryEvent.LoadMedia)
                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
            }
        }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
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
            key = { page -> state.displayedMediaList[page].id },
//            beyondViewportPageCount = 1
        ) { page ->
            val media = state.displayedMediaList[page]
            val isCurrentPage = pagerState.currentPage == page

            if (media.isVideo) {
                with(sharedTransitionScope) {
                    CustomVideoPlayer(
                        media = media,
                        animatedVisibilityScope = animatedVisibilityScope,
                        isCurrentPage = isCurrentPage,
                        exoPlayer= exoPlayer,
                        isVisible = isUiVisible,
                        onVisibilityChanged = {isUiVisible = it}
                    )
                }
            } else {
                with(sharedTransitionScope) {
                    val imageRequest = remember(media.uriString) {
                        ImageRequest.Builder(context).data(media.uriString).build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = media.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { isUiVisible = !isUiVisible }
                                )
                            }
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "media_${media.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                            )
                    )
                }
            }
        }
        with(sharedTransitionScope) {


            AnimatedVisibility(visible = isUiVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 24.dp)
                    .zIndex(1f)){
                DetailFloatingBar(
                    isFavorite = isFavorite, onActionClick = { action ->
                        currentMedia?.let { media ->
                            when (action) {
                                DetailAction.SHARE -> {
                                    MediaIntents.shareMedia(
                                        context,
                                        media.uriString,
                                        media.mimeType
                                    )
                                }

                                DetailAction.EDIT -> {
                                    MediaIntents.editMedia(context, media.uriString, media.mimeType)
                                }

                                DetailAction.FAVOURITE -> {
                                    viewModel.setEvent(
                                        GalleryEvent.ToggleFavorite(
                                            media,
                                            !isFavorite
                                        )
                                    )
                                }

                                DetailAction.INFO -> {
                                    viewModel.setEvent(GalleryEvent.OpenInfoSheet(media))
                                }

                                DetailAction.DELETE -> {
                                    val uri = media.uriString.toUri()
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            val pendingIntent = MediaStore.createDeleteRequest(
                                                context.contentResolver, listOf(uri)
                                            )
                                            deleteLauncher.launch(
                                                IntentSenderRequest.Builder(
                                                    pendingIntent.intentSender
                                                ).build()
                                            )
                                        } else {
                                            val deletedRows =
                                                context.contentResolver.delete(uri, null, null)
                                            if (deletedRows > 0) {
                                                viewModel.setEvent(GalleryEvent.LoadMedia)
                                            }
                                        }
                                    } catch (e: SecurityException) {
                                        Toast.makeText(
                                            context, "Cannot delete this file", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }


                    }, modifier = Modifier

                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            zIndexInOverlay = 1f
                        )
                )
            }

        }
        MediaInfoSheet(sheetState = state.infoSheetState, onDismiss = {
            viewModel.setEvent(
                GalleryEvent.CloseInfoSheet
            )
        })
    }
}