package com.example.jetpackcomposegalleryapp.di

import android.content.ContentResolver
import android.content.Context
import androidx.compose.runtime.MovableContent
import com.example.jetpackcomposegalleryapp.data.repository.MediaStoreRepositoryImpl
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext content: Context): ContentResolver{
        return content.contentResolver

    }


    @Provides
    @Singleton
    fun provideMediaRepository(contentResolver: ContentResolver): MediaRepository {
        return MediaStoreRepositoryImpl(contentResolver)

    }
}