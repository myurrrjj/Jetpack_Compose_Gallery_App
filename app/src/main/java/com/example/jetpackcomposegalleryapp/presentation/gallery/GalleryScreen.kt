package com.example.jetpackcomposegalleryapp.presentation.gallery

//import android.graphics.drawable.Icon
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.core.util.rememberScrollingUp
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.FloatingGalleryBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaItemCard

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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    val gridState = rememberLazyGridState()
    val isScrollingUp = rememberScrollingUp(gridState)
    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        val isGranted = permission.values.all { it }
        viewModel.setEvent(GalleryEvent.PermissionResult(isGranted))


    }
    LaunchedEffect(Unit) {
        viewModel.setEvent(GalleryEvent.LoadMedia)
        viewModel.effect.collect { effect ->
            when (effect) {
                is GalleryEffect.RequestPermission -> permissionLauncher.launch(permissionToRequest)
                is GalleryEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.mediaId)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    onClick = { permissionLauncher.launch(permissionToRequest) })
            }

            state.mediaList.isEmpty() -> {
                EmptyStateView(
                    title = "No Media Found",
                    description = "Your gallery is completely empty. Take some photos to get started!",
                    buttonText = "Refresh",
                    onClick = { viewModel.setEvent(GalleryEvent.LoadMedia) })
            }

            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 120.dp),

                    contentPadding = PaddingValues(
                        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.systemBars.asPaddingValues()
                            .calculateBottomPadding() + 80.dp,
                        start = 2.dp,
                        end = 2.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = state.mediaList.size,
                        contentType = { index -> if (state.mediaList[index].isVideo) "video" else "photo" },
                        key = { index -> state.mediaList[index].id }) { index ->
                        MediaItemCard(
                            media = state.mediaList[index],
                            onClick = {
                                viewModel.setEvent(GalleryEvent.MediaClicked(state.mediaList[index].id))
                            },
                            modifier = Modifier.animateItem(),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                }


            }
        }
        val barOffset by animateDpAsState(
            targetValue = if (state.mediaList.isNotEmpty() && isScrollingUp) 0.dp else 150.dp,
            label = "barOffset"
        )
        val barAlpha by animateFloatAsState(
            targetValue = if (state.mediaList.isNotEmpty() && isScrollingUp) 1f else 0f,
            label = "barAlpha"
        )
//        AnimatedVisibility(
//            visible = state.mediaList.isNotEmpty() && isScrollingUp,
//            enter =EnterTransition.None,
//            exit =  ExitTransition.None,
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 24.dp)
//                .zIndex(1f)
//        ) {
        with(sharedTransitionScope) {
//                Box(
//                    modifier = Modifier.sharedElement(
//                        sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
//                        animatedVisibilityScope = animatedVisibilityScope,zIndexInOverlay = 1f
//                    )
//                ) {
            FloatingGalleryBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues()
                            .calculateBottomPadding() + 24.dp
                    )
                    .graphicsLayer {
                        translationY = barOffset.toPx()
                        alpha = barAlpha
                    }
                    .zIndex(1f)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 1f
                    )
            )

        }

    }
}
