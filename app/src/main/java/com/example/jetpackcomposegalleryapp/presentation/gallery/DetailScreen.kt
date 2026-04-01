package com.example.jetpackcomposegalleryapp.presentation.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar

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
    val context = LocalContext.current
    val initialIndex = state.mediaList.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { state.mediaList.size }
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
    {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            val media = state.mediaList[page]
            with(sharedTransitionScope) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uriString)
                        .build(), contentDescription = media.name, contentScale = ContentScale.Fit,
                     modifier = Modifier
                        .fillMaxSize()
                         .sharedBounds(
                             sharedContentState = rememberSharedContentState(key = "media_${media.id}"),
                             animatedVisibilityScope = animatedVisibilityScope,
                             resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                         )
                )
            }
        }
        with(sharedTransitionScope) {
            DetailFloatingBar(
                onActionClick = { action ->
                    when (action) {
                        DetailAction.SHARE -> {}
                        DetailAction.EDIT -> {}
                        DetailAction.FAVOURITE -> {}
                        DetailAction.INFO -> {}
                        DetailAction.DELETE -> {}
                    }

                },
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues()
                            .calculateBottomPadding() + 24.dp
                    )
                    .zIndex(1f)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                        animatedVisibilityScope = animatedVisibilityScope,zIndexInOverlay = 1f
                    )
            )
        }
    }
}