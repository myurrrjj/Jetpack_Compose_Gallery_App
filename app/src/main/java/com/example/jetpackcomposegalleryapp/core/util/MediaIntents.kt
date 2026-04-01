package com.example.jetpackcomposegalleryapp.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object MediaIntents{
    fun shareMedia(context: Context, uriString:String, mimeType: String){
        val uri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        }
        context.startActivity(Intent.createChooser(shareIntent,"Share Media"))

    }
    fun editMedia(context: Context,uriString:String,mimeType: String){
        val uri = Uri.parse(uriString)
        val editIntent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, mimeType)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(editIntent,"Edit Media"))
    }
}