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
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val imageRequest = remember(media.uriString, media.id) {
        ImageRequest.Builder(context).data(media.uriString).size(300)
            .setParameter("is_thumbnail", true)
//            .size(200)
//            .memoryCacheKey("${media.id}_thumbnail")
//            .diskCacheKey("${media.id}_thumbnail")
            .build()
    }
    with(sharedTransitionScope) {

        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "media_${media.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                )

                .bouncyClick(onClick = onClick)

        ) {

            AsyncImage(
                model = imageRequest,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
            val overlayBrush = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.0f),
                        Color.Black.copy(alpha = 0.5f)
                    ), startY = 150f
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        overlayBrush
                    )

            )

            if (media.isVideo) {
                val formattedDuration = remember(media.duration) {
                    media.duration?.let { formatDuration(it) }
                }
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
                if (formattedDuration != null) {
                    Text(
                        text = formattedDuration,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(
                                Alignment.BottomEnd
                            )
                            .padding(8.dp)
                    )


                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}