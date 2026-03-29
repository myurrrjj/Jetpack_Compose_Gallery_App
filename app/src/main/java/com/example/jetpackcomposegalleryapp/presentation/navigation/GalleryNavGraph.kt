package com.example.jetpackcomposegalleryapp.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryNavGraph() {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Route.Gallery,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<Route.Gallery> {
                GalleryScreen(
                    onNavigateToDetail = { mediaId ->
                        navController.navigate(Route.Detail(mediaId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable<Route.Detail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.Detail>()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Detail Screen for ID: ${args.mediaId}",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}