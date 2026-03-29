package com.example.jetpackcomposegalleryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryScreen
import com.example.jetpackcomposegalleryapp.presentation.navigation.GalleryNavGraph
import com.yourname.gallery.core.presentation.theme.JetpackComposeGalleryAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackComposeGalleryAppTheme{
                GalleryNavGraph()
            }
        }
    }
}

@Composable
fun Greeting(){
    Box(Modifier.fillMaxSize()){
        Text("hello")
    }
}
