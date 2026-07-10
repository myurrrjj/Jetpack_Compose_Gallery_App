package com.example.jetpackcomposegalleryapp.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MlKitFaceDetector @Inject constructor() {
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(.1f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val rawFaces = faceDetector.process(image).await()
            rawFaces.filter { isValidFace(it) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emptyList()
        }
    }

    fun close() {
        faceDetector.close()
    }

    private fun isValidFace(face: Face): Boolean {
        val bounds = face.boundingBox
        val width = bounds.width()
        val height = bounds.height()
        if (width < 80 || height < 80) return false
        val aspectRatio = height.toFloat() / width.toFloat()
        if (aspectRatio < 1f || aspectRatio > 1.6f) return false

        if (abs(face.headEulerAngleX) > 30f ||
            abs(face.headEulerAngleY) > 30f ||
            abs(face.headEulerAngleZ) > 30f
        ) return false

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        if (leftEye == null || rightEye == null || mouth == null) return false

        val eyeDistance = abs(leftEye.x - rightEye.x)
        val eyeDistanceRatio = eyeDistance / width.toFloat()
        if (eyeDistanceRatio < .3f || eyeDistanceRatio > .6f) return false
        if (leftEye.y >= mouth.y || rightEye.y >= mouth.y) return false
        if (face.leftEyeOpenProbability == null || face.smilingProbability == null) return false


        return true


    }


}