package com.example.jetpackcomposegalleryapp.di

import android.content.ContentResolver
import android.content.Context
import com.example.jetpackcomposegalleryapp.data.local.dao.FavouriteDao
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
    fun provideMediaRepository(contentResolver: ContentResolver,favouriteDao: FavouriteDao): MediaRepository {
        return MediaStoreRepositoryImpl(contentResolver, favouriteDao = favouriteDao)

    }
}