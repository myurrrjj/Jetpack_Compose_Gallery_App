package com.example.jetpackcomposegalleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class GalleryApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoaderProvider: Provider<ImageLoader>

    override fun newImageLoader(): ImageLoader {
        return imageLoaderProvider.get()
    }
}