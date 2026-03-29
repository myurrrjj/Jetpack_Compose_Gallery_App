package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalWithHostDefaultOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset

@Composable
fun MediaItemCard(
    media: MediaAsset,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
with(sharedTransitionScope) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bouncyClick(onClick = onClick)
            .sharedElement(
                sharedContentState = rememberSharedContentState(key = "media_${media.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
            .bouncyClick(onClick = onClick)

    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.uriString)
                .crossfade(true)
                .crossfade(300)
                .size(Size(512, 512))
                .memoryCacheKey("${media.id}_thumbnail")
                .diskCacheKey("${media.id}_thumbnail")
                .build(),
            contentDescription = media.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            ),
                            startY = 100f
                        )
                    )
            )

            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
}