package com.example.jetpackcomposegalleryapp.domain.model

data class DetailedMediaInfo (
    val mediaId:Long?,
    val resolution:String?,
    val megapixelCount:Float?
    ,
    val locLatitude:Double?,
    val locLongitude:Double?
    ,
    val cameraMake:String?,
    val cameraModel:String?,
    val aperture:String?,
    val exposureTime:String?,
    val iso:String?,
    val frameRate: Float?,
    val bitrate:Int?,
    val colorSpace:String?,


    )