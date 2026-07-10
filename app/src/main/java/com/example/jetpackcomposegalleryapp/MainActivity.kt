package com.example.jetpackcomposegalleryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jetpackcomposegalleryapp.presentation.navigation.GalleryNavGraph
import com.yourname.gallery.core.presentation.theme.JetpackComposeGalleryAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            JetpackComposeGalleryAppTheme {
                GalleryNavGraph()
            }
        }
    }

}