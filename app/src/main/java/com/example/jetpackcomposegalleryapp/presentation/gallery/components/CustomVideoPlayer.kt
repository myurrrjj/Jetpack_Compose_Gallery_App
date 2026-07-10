package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class ControlPosition {
    BOTTOM, LEFT, RIGHT
}

data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
    val audioTracks: List<String> = emptyList(),
    val subtitleTracks: List<String> = emptyList(),
    val selectedAudioTrack: Int = -1,
    val selectedSubtitleTrack: Int = -1,
    val isFirstFrameRendered: Boolean = false
)

@Suppress("MultipleContentEmitters")
@Composable
fun rememberVideoPlayerState(player: ExoPlayer): State<VideoPlayerState> {
    val state = remember { mutableStateOf(VideoPlayerState()) }

    DisposableEffect(player) {
        @Suppress("DerivedStateOfCandidate") val listener = object : Player.Listener {
            @OptIn(UnstableApi::class)
            override fun onRenderedFirstFrame() {
                state.value = state.value.copy(isFirstFrameRendered = true)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                val subtitleGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

                state.value = state.value.copy(
                    audioTracks = audioGroups.mapIndexed { index, group ->
                        trackLabel(
                            group,
                            index
                        )
                    },
                    subtitleTracks = subtitleGroups.mapIndexed { index, group ->
                        trackLabel(
                            group,
                            index
                        )
                    },
                    selectedAudioTrack = audioGroups.indexOfFirst { it.isSelected },
                    selectedSubtitleTrack = subtitleGroups.indexOfFirst { it.isSelected }
                )
            }

            override fun onEvents(player: Player, events: Player.Events) {
                state.value = state.value.copy(
                    isPlaying = player.isPlaying,
                    playbackState = player.playbackState,
                    currentPosition = player.currentPosition.coerceAtLeast(0L),
                    duration = player.duration.coerceAtLeast(0L),
                    playbackSpeed = player.playbackParameters.speed
                )
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(state.value.isPlaying) {
        while (isActive && state.value.isPlaying) {
            state.value =
                state.value.copy(currentPosition = player.currentPosition.coerceAtLeast(0L))
            delay(200L)
        }
    }
    return state
}

@OptIn(UnstableApi::class)
private fun trackLabel(group: Tracks.Group, fallbackIndex: Int): String {
    if (group.mediaTrackGroup.length == 0) return "Track ${fallbackIndex + 1}"
    val format = group.mediaTrackGroup.getFormat(0)
    return format.label ?: format.language?.uppercase() ?: "Track ${fallbackIndex + 1}"
}

@Composable
private fun PlayerControlSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CircleShape,
    containerColor: Color,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .bouncyClick(onClick = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

fun formatVideoTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun VideoPlayerOverlay(
    player: ExoPlayer,
    state: VideoPlayerState,
    isVisible: Boolean,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val configuration = LocalConfiguration.current
    val videoIsLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    val currentProgress = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val sliderValue = if (isDragging) dragProgress else currentProgress
    val subtitlesOn = state.selectedSubtitleTrack >= 0
    var controlPosition by remember { mutableStateOf(ControlPosition.BOTTOM) }

    LaunchedEffect(
        isVisible,
        state.isPlaying,
        isDragging,
        showAudioMenu,
        showSubtitleMenu,
        controlPosition
    ) {
        if (isVisible && state.isPlaying && !isDragging && !showAudioMenu && !showSubtitleMenu) {
            delay(3000L)
            onVisibilityChanged(false)
        }
    }

    LaunchedEffect(videoIsLandscape) {
        controlPosition = if (!videoIsLandscape) {
            ControlPosition.BOTTOM
        } else {
            ControlPosition.RIGHT
        }
    }
    val horizontalBias by animateFloatAsState(
        targetValue = when (controlPosition) {
            ControlPosition.LEFT -> -1f
            ControlPosition.RIGHT -> 1f
            ControlPosition.BOTTOM -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "horizontalBias"
    )

    val verticalBias by animateFloatAsState(
        targetValue = when (controlPosition) {
            ControlPosition.BOTTOM -> 1f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "verticalBias"
    )



    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isVisible, videoIsLandscape) {
                detectTapGestures(onTap = { offset ->
                    val tappedPosition = if (videoIsLandscape) {
                        if (offset.x < size.width / 2) ControlPosition.LEFT else ControlPosition.RIGHT
                    } else {
                        ControlPosition.BOTTOM
                    }

                   if (!isVisible){
                       controlPosition = tappedPosition
                       onVisibilityChanged(true)

                   }
                    else{
                        if (controlPosition!=tappedPosition){
                            controlPosition = tappedPosition
                        }
                       else{
                           onVisibilityChanged(false)
                        }
                   }
                })
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
//                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets(
//                            androidx.compose.foundation.layout.WindowInsets.systemBars))
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .padding(
                            bottom = if (controlPosition == ControlPosition.BOTTOM) 110.dp else 0.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .align(BiasAlignment(horizontalBias, verticalBias))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.audioTracks.isNotEmpty()) {
                                Box {
                                    PlayerControlSurface(
                                        onClick = { showAudioMenu = true },
                                        modifier = Modifier.size(48.dp),
                                        contentDescription = "AudioTrack",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Audiotrack,
                                            contentDescription = "Audio track",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showAudioMenu,
                                        onDismissRequest = { showAudioMenu = false }
                                    ) {
                                        state.audioTracks.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(text = label) },
                                                trailingIcon = {
                                                    if (index == state.selectedAudioTrack) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectTrack(player, C.TRACK_TYPE_AUDIO, index)
                                                    showAudioMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (state.subtitleTracks.isNotEmpty()) {
                                Box {
                                    PlayerControlSurface(
                                        onClick = { showSubtitleMenu = true },
                                        modifier = Modifier.size(48.dp),
                                        contentDescription = "SubtitleTrack",
                                        containerColor = if (subtitlesOn) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (subtitlesOn) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                                            contentDescription = "Subtitles",
                                            tint = if (subtitlesOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSubtitleMenu,
                                        onDismissRequest = { showSubtitleMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Off") },
                                            trailingIcon = {
                                                if (!subtitlesOn) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                disableTrackType(player, C.TRACK_TYPE_TEXT)
                                                showSubtitleMenu = false
                                            }
                                        )
                                        state.subtitleTracks.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                trailingIcon = {
                                                    if (index == state.selectedSubtitleTrack) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectTrack(player, C.TRACK_TYPE_TEXT, index)
                                                    showSubtitleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            PlayerControlSurface(
                                onClick = { if (state.isPlaying) player.pause() else player.play() },
                                modifier = Modifier.size(56.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerControlSurface(
                                onClick = {
                                    val newSpeed = when (state.playbackSpeed) {
                                        1f -> 1.5f; 1.5f -> 2f; 2f -> 0.5f; else -> 1f
                                    }
                                    player.playbackParameters = PlaybackParameters(newSpeed)
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentDescription = "Playback speed"
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${state.playbackSpeed}x",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            PlayerControlSurface(
                                onClick = {
                                    activity?.requestedOrientation = if (!videoIsLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentDescription = "Fullscreen"
                            ) {
                                Icon(
                                    imageVector = if (videoIsLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatVideoTime(if (isDragging) (dragProgress * state.duration).toLong() else state.currentPosition),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                isDragging = true
                                dragProgress = newValue
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                player.seekTo((dragProgress * state.duration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.4f
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatVideoTime(state.duration),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

fun selectTrack(player: ExoPlayer, trackType: Int, index: Int) {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType }
    if (index in trackGroups.indices) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(TrackSelectionOverride(trackGroups[index].mediaTrackGroup, 0))
            .build()
    }
}

fun disableTrackType(player: ExoPlayer, trackType: Int) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(trackType, true)
        .clearOverridesOfType(trackType)
        .build()
}

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    media: MediaAsset,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isCurrentPage: Boolean,
    exoPlayer: ExoPlayer,
    isVisible: Boolean,
    videoState: VideoPlayerState,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isCurrentPage, media, autoPlay) {
        if (isCurrentPage) {
            val mediaItem = MediaItem.fromUri(media.uriString)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (autoPlay) {
                exoPlayer.play()
            }
        } else {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isCurrentPage) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val themeBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    useArtwork = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(themeBackgroundColor)
                }
            },
            update = { playerView ->
                if (isCurrentPage) {
                    playerView.player = exoPlayer
                    playerView.setBackgroundColor(themeBackgroundColor)
                } else {
                    playerView.player = null
                }
            }
        )

        if (isCurrentPage) {
            VideoPlayerOverlay(
                player = exoPlayer,
                state = videoState,
                isVisible = isVisible,
                onVisibilityChanged = onVisibilityChanged
            )
        }
    }
}