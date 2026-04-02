package com.example.jetpackcomposegalleryapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.jetpackcomposegalleryapp.data.local.dao.FavouriteDao
import com.example.jetpackcomposegalleryapp.data.local.entity.FavouriteMediaEntity

@Database(
    entities = [FavouriteMediaEntity::class], version = 1, exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract val favouriteDao: FavouriteDao

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