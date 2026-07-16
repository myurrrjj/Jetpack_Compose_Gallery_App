package com.example.jetpackcomposegalleryapp.domain.repository

import com.example.jetpackcomposegalleryapp.data.local.dao.FaceDao
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface FaceRepository {
    fun getAllClusters(): Flow<List<PersonCluster>>
    suspend fun getClusterById(clusterId: Long): PersonCluster?
    suspend fun updateClusterName(clusterId: Long, name: String)
    suspend fun getMediaIdsForCluster(clusterId: Long): List<Long>
    suspend fun deleteCluster(clusterId: Long)

}

