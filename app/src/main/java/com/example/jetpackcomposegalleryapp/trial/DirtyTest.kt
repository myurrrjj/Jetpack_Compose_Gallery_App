package com.example.jetpackcomposegalleryapp.trial

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.jetpackcomposegalleryapp.core.ml.FaceClusterer
import com.example.jetpackcomposegalleryapp.core.ml.FaceCropper
import com.example.jetpackcomposegalleryapp.core.ml.FaceEmbedder
import com.example.jetpackcomposegalleryapp.core.ml.MlKitFaceDetector
import com.example.jetpackcomposegalleryapp.core.util.BitmapLoader
import com.example.jetpackcomposegalleryapp.data.local.dao.FaceDao
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceEmbeddingEntity
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DirtyClusterData(
    val clusterId: Long,
    val label: String,
    val coverCropPath: String,
    val faceCropPaths: List<String>
)

@HiltViewModel
class DirtyTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val faceDao: FaceDao,
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer,
    private val contentResolver: ContentResolver
) : ViewModel() {

    val statusMessage = MutableStateFlow("Ready to test.")
    val isProcessing = MutableStateFlow(false)

    val visualClusters = MutableStateFlow<List<DirtyClusterData>>(emptyList())
    val selectedClusterId = MutableStateFlow<Long?>(null)

    fun runDirtyPipeline(selectedUris: List<String>? = null) {
        if (isProcessing.value) return
        isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            statusMessage.value = "Wiping old database entries..."
            faceDao.deleteAllFaceData()
            visualClusters.value = emptyList()

            val cacheFolder = File(context.cacheDir, "dirty_face_crops")
            if (cacheFolder.exists()) cacheFolder.deleteRecursively()
            cacheFolder.mkdirs()

            statusMessage.value = "Fetching images from Gallery..."
            val allMedia = mediaRepository.getAllMedia().first()

            val targetMedia = if (selectedUris != null) {
                selectedUris.mapIndexed { index, uriStr ->
                    allMedia.find { it.uriString == uriStr } ?: MediaAsset(
                        id = android.net.Uri.parse(uriStr).lastPathSegment?.toLongOrNull() ?: (System.currentTimeMillis() + index),
                        uriString = uriStr,
                        name = "Manual_Pick_$index",
                        dateAdded = System.currentTimeMillis() / 1000,
                        mimeType = "image/jpeg",
                        size = 0L, width = null, height = null, duration = null, folderName = "Manual"
                    )
                }
            } else {
                allMedia.filter { !it.isVideo }.take(340)
            }

            if (targetMedia.isEmpty()) {
                statusMessage.value = "No valid images found to process!"
                isProcessing.value = false
                return@launch
            }

            val generatedEmbeddings = mutableListOf<FaceEmbeddingEntity>()
            val pendingEmbeddings = mutableListOf<FaceEmbeddingEntity>()
            val pendingCrops = mutableListOf<Bitmap>()

            // Lower batch limit to 10 so the UI updates and animates frequently!
            val batchLimit = 10

            for ((index, media) in targetMedia.withIndex()) {
                statusMessage.value = "Processing image ${index + 1} of ${targetMedia.size}..."

                val bitmap = BitmapLoader.loadBitmap(contentResolver, media.uriString, 2048)
                if (bitmap == null) continue

                val detectedFaces = faceDetector.detectFaces(bitmap)

                for ((faceIndex, face) in detectedFaces.withIndex()) {
                    val croppedBitmap = FaceCropper.cropFace(bitmap, face) ?: continue
                    val preprocessedBitmap = FaceCropper.preprocessForEmbedding(croppedBitmap)
                    val embeddingArray = faceEmbedder.generateEmbedding(preprocessedBitmap)

                    if (embeddingArray != null) {
                        val bounds = face.boundingBox
                        val entity = FaceEmbeddingEntity(
                            mediaId = media.id,
                            faceIndex = faceIndex,
                            embedding = embeddingArray,
                            boundsLeft = bounds?.left?.toFloat() ?: 0f,
                            boundsTop = bounds?.top?.toFloat() ?: 0f,
                            boundsRight = bounds?.right?.toFloat() ?: 0f,
                            boundsBottom = bounds?.bottom?.toFloat() ?: 0f
                        )

                        pendingEmbeddings.add(entity)
                        pendingCrops.add(croppedBitmap)
                    } else {
                        croppedBitmap.recycle()
                    }
                    preprocessedBitmap.recycle()
                }
                bitmap.recycle()

                // Execute clustering and UI update progressively
                if (pendingEmbeddings.size >= batchLimit || index == targetMedia.lastIndex) {
                    if (pendingEmbeddings.isNotEmpty()) {
                        val insertedIds = faceDao.insertEmbeddings(pendingEmbeddings)

                        for (i in pendingEmbeddings.indices) {
                            val id = insertedIds[i]
                            val crop = pendingCrops[i]

                            val cropFile = File(cacheFolder, "crop_${id}.jpeg")
                            FileOutputStream(cropFile).use { out ->
                                crop.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }

                            generatedEmbeddings.add(pendingEmbeddings[i].copy(embeddingId = id))
                            crop.recycle()
                        }

                        pendingEmbeddings.clear()
                        pendingCrops.clear()

                        // 1. Wipe DB clusters to re-calculate freshly with the new data
                        faceDao.deleteAllClusters()
                        // 2. Cluster everything found so far
                        faceClusterer.clusterEmbeddings(generatedEmbeddings, emptyMap())

                        // 3. Update the UI state so it animates
                        val finalClusters = faceDao.getAllClusters().first()
                        val mappedUiData = mutableListOf<DirtyClusterData>()

                        for (cluster in finalClusters) {
                            val embeddingIds = faceDao.getEmbeddingIdsForCluster(cluster.clusterId)
                            val facePaths = embeddingIds.map { id ->
                                File(cacheFolder, "crop_${id}.jpeg").absolutePath
                            }.filter { File(it).exists() }

                            if (facePaths.isNotEmpty()) {
                                mappedUiData.add(
                                    DirtyClusterData(
                                        clusterId = cluster.clusterId,
                                        label = cluster.personLabel,
                                        coverCropPath = facePaths.first(),
                                        faceCropPaths = facePaths
                                    )
                                )
                            }
                        }

                        visualClusters.value = mappedUiData.sortedByDescending { it.faceCropPaths.size }
                        System.gc()
                        delay(100) // Small delay to let Compose trigger the animations beautifully
                    }
                }
            }
            statusMessage.value = "Finished! Found ${visualClusters.value.size} unique people!"
            isProcessing.value = false
        }
    }

    fun selectCluster(id: Long?) {
        selectedClusterId.value = id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirtyTestScreen(viewModel: DirtyTestViewModel = hiltViewModel()) {
    val status by viewModel.statusMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val clusters by viewModel.visualClusters.collectAsState()
    val selectedClusterId by viewModel.selectedClusterId.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.runDirtyPipeline(selectedUris = uris.map { it.toString() })
        }
    }

    BackHandler(enabled = selectedClusterId != null) {
        viewModel.selectCluster(null)
    }

    if (selectedClusterId == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("People & Pets Indexer") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.runDirtyPipeline(null) },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isProcessing) "Processing..." else "Run Default (340 Images)")
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Select Photos Manually")
                    }
                }

                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // CRITICAL: Using coverCropPath as the key ensures stable identity for the animations
                    // even as the DB drops and rebuilds the cluster IDs behind the scenes!
                    items(clusters, key = { it.coverCropPath }) { cluster ->
                        ElevatedCard(
                            modifier = Modifier
                                .animateItem() // <--- The magic Compose animation modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f)
                                .clickable { viewModel.selectCluster(cluster.clusterId) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = File(cluster.coverCropPath),
                                    contentDescription = "Cover for ${cluster.label}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Gorgeous gradient for text readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                                startY = 150f
                                            )
                                        )
                                )

                                // Animated Tick Badge
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = cluster.faceCropPaths.size,
                                        transitionSpec = {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        },
                                        label = "CountAnimation"
                                    ) { count ->
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Person ${cluster.clusterId}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        val selectedCluster = clusters.find { it.clusterId == selectedClusterId }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Person ${selectedCluster?.clusterId}")
                            Text(
                                text = "${selectedCluster?.faceCropPaths?.size ?: 0} matching faces",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.selectCluster(null) }) {
                            Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val cropPaths = selectedCluster?.faceCropPaths ?: emptyList()
                items(cropPaths) { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Auditing Face Crop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.LightGray)
                    )
                }
            }
        }
    }
}