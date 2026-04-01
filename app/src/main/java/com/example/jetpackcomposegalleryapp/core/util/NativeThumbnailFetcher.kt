package com.example.jetpackcomposegalleryapp.core.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable

class NativeThumbnailFetcher(
    private val uri: Uri,
    private val context: Context,
    private val isVideo: Boolean
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        android.util.Size(350, 350),
                        null
                    )
                } else {
                    val id = uri.lastPathSegment?.toLongOrNull() ?: throw IllegalArgumentException()
                    if (isVideo) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null
                        )
                    } else {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            context.contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null
                        )
                    }
                }

                if (bitmap != null) {
                    DrawableResult(
                        drawable = bitmap.toDrawable(context.resources),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isThumbnail = options.parameters.value("is_thumbnail") as? Boolean ?: false

            if (isThumbnail && data.scheme == "content" && data.authority == "media") {
                val isVideo = data.toString().contains("video")
                return NativeThumbnailFetcher(data, context, isVideo)
            }
            return null
        }
    }
}