package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.presentation.gallery.Album

@Composable
fun SharedTransitionScope.AlbumDetailView(
    album: Album,
    mediaList: List<MediaAsset>,
    selectedMediaIds: Set<Long> = emptySet(),
    selectionMode: Boolean = false,
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: () -> Unit,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    albumAnimatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = album.name ?: "Unknown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "album_title_${album.name}"),
                        animatedVisibilityScope = albumAnimatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                    )
                )
            }

            Box(modifier = Modifier.weight(1f))
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                start = 2.dp,
                end = 2.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = mediaList.size,
                key = { index -> mediaList[index].id }
            ) { index ->
                val mediaItem = mediaList[index]
                val isSelected = selectedMediaIds.contains(mediaItem.id)

                MediaItemCard(
                    media = mediaItem,
                    isSelected = isSelected,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            onToggleSelection(mediaItem.id)
                        } else {
                            onMediaClick(mediaItem.id)
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            onEnterSelectionMode()
                            onToggleSelection(mediaItem.id)
                        }
                    },
                    sharedTransitionScope = this@AlbumDetailView,
                    tabAnimatedVisibilityScope = tabAnimatedVisibilityScope,
                    albumAnimatedVisibilityScope = albumAnimatedVisibilityScope,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}