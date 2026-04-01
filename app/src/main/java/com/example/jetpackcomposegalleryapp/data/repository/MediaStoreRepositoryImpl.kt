package com.example.jetpackcomposegalleryapp.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaStoreRepositoryImpl(private val contentResolver: ContentResolver) : MediaRepository {
    override fun getAllMedia(): Flow<List<MediaAsset>> = flow {
        val mediaList = mutableListOf<MediaAsset>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val bucketColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)

                    val width = if (widthColumn != -1 && !cursor.isNull(widthColumn)) cursor.getInt(
                        widthColumn
                    ) else null
                    val height =
                        if (heightColumn != -1 && !cursor.isNull(heightColumn)) cursor.getInt(
                            heightColumn
                        ) else null
                    val folderName = cursor.getString(bucketColumn) ?: "Unknown"
                    val duration =
                        if (durationColumn != -1 && !cursor.isNull(durationColumn)) cursor.getLong(
                            durationColumn
                        ) else null

                    val contentUri = ContentUris.withAppendedId(
                        if (mimeType.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id

                    )
                    mediaList.add(
                        MediaAsset(
                            id = id,
                            uriString = contentUri.toString(),
                            name = name,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            size = size,
                            width = width,
                            height = height,
                            duration = duration,
                            folderName = folderName

                            )
                    )
                }

            }

        emit(mediaList)
    }.flowOn(Dispatchers.IO)

}