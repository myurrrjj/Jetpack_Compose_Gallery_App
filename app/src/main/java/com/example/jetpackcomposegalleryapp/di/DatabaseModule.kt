package com.example.jetpackcomposegalleryapp.di

import android.content.Context
import com.example.jetpackcomposegalleryapp.data.local.GalleryDatabase
import com.example.jetpackcomposegalleryapp.data.local.dao.FavouriteDao
//import com.example.jetpackcomposegalleryapp.data.local.dao.MediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase {
        return GalleryDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMediaDao(database: GalleryDatabase): FavouriteDao {
        return database.favouriteDao
    }
}