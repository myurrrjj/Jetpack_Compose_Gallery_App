package com.example.jetpackcomposegalleryapp.di

import com.example.jetpackcomposegalleryapp.domain.repository.FaceRepository
import com.example.jetpackcomposegalleryapp.domain.repository.FaceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class FaceRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFaceRepository(
        faceRepositoryImpl: FaceRepositoryImpl
    ): FaceRepository
}