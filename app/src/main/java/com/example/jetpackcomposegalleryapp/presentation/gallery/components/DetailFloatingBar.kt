package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick

enum class DetailAction(val icon: ImageVector, val contentDescription: String) {
    SHARE(Icons.Rounded.Share, "Share"),
    EDIT(Icons.Rounded.Edit, "Edit"),
    FAVOURITE(Icons.Rounded.FavoriteBorder, "Favourite"),
    INFO(Icons.Rounded.Info, "Info"),
    DELETE(Icons.Rounded.DeleteOutline, "Delete")
}

@Composable
fun DetailFloatingBar(isFavorite: Boolean, onActionClick: (DetailAction) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = .5f)
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .85f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailAction.entries.forEach { action ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .bouncyClick(
                        onClick =
                            { onActionClick(action) }
                    )
                    .padding(12.dp)
            ) {
                val displayIcon = if (action == DetailAction.FAVOURITE && isFavorite) {
                    Icons.Rounded.Favorite
                } else {
                    action.icon
                }

                val displayTint = if (action == DetailAction.FAVOURITE && isFavorite) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                Icon(
                    imageVector = displayIcon,
                    contentDescription = action.contentDescription,
                    tint = displayTint
                )
            }
        }
    }
}
