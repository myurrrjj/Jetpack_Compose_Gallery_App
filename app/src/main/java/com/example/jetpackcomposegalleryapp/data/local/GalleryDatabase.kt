package com.example.jetpackcomposegalleryapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.jetpackcomposegalleryapp.data.local.dao.FaceDao
import com.example.jetpackcomposegalleryapp.data.local.dao.FavouriteDao
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceClusterEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceEmbeddingEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.FavouriteMediaEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaPersonEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaProcessingStatusEntity

@Database(
    entities = [
        FavouriteMediaEntity::class,
        FaceEmbeddingEntity::class,
        FaceClusterEntity::class,
        MediaPersonEntity::class,
        MediaProcessingStatusEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract val favouriteDao: FavouriteDao
    abstract val faceDao : FaceDao


    companion object {
        @Volatile
        private var INSTANCE: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, GalleryDatabase::class.java, "gallery_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}