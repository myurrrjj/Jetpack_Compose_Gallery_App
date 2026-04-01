package com.example.jetpackcomposegalleryapp.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.jetpackcomposegalleryapp.presentation.gallery.DetailScreen
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryScreen
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryViewModel

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
                val sharedViewModel: GalleryViewModel = hiltViewModel()

                GalleryScreen(
                    viewModel = sharedViewModel, onNavigateToDetail = { mediaId ->
                        navController.navigate(Route.Detail(mediaId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable<Route.Detail> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.Detail>()
                val parentEntry = remember(backStackEntry){
                    navController.getBackStackEntry(Route.Gallery)
                }
                val sharedViewModel: GalleryViewModel = hiltViewModel(parentEntry)

                DetailScreen(
                    initialMediaId = args.mediaId,
                    onNavigateBack = { navController.navigate(Route.Gallery) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    viewModel = sharedViewModel
                )
            }
        }
    }
}