package com.example.audioplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

// Enumeration for playback modes
enum class PlaybackMode {
    PLAY_ALL,    // Play all tracks sequentially
    REPEAT_ONE   // Repeat the current track
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    val supportedExtensions = listOf("mp3", "wav", "m4a", "flac", "ogg", "aac")

    val musicFiles = remember {
        getAllAudioFiles(context, supportedExtensions)
    }

    var currentTrackIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var trackTitle by remember { mutableStateOf<String?>(null) }
    var trackArtwork by remember { mutableStateOf<Bitmap?>(null) }
    var playbackMode by remember { mutableStateOf(PlaybackMode.PLAY_ALL) }

    var mediaPlayer by remember {
        mutableStateOf(MediaPlayer())
    }

    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }


    fun playTrack(index: Int) {
        if (index in musicFiles.indices) {
            mediaPlayer.release()
            val (player, title, artwork) = prepareTrack(
                file = musicFiles[index],
                onCompletion = {
                    // When a track finishes in PLAY_ALL mode, stop playback
                    isPlaying = false
                },
                playbackMode = playbackMode,
                onPlaybackModeAction = {
                    // When repeating a track in REPEAT_ONE mode, continue playback
                    isPlaying = true
                },
                onNextTrack = {
                    // Move to the next track
                    currentTrackIndex = (currentTrackIndex + 1) % musicFiles.size
                    playTrack(currentTrackIndex)
                }
            )

            mediaPlayer = player
            player.start()
            duration = player.duration
            isPlaying = true
            trackTitle = title
            trackArtwork = artwork
        }
    }

    // Load and prepare the first track
    LaunchedEffect(musicFiles) {
        if (musicFiles.isNotEmpty()) {
            mediaPlayer.release()
            val (player, title, artwork) = prepareTrack(
                file = musicFiles[currentTrackIndex],
                onCompletion = {
                    // When a track finishes in PLAY_ALL mode, stop playback
                    isPlaying = false
                },
                playbackMode = playbackMode,
                onPlaybackModeAction = {
                    // When repeating a track in REPEAT_ONE mode, continue playback
                    isPlaying = true
                },
                onNextTrack = {
                    // Move to the next track
                    currentTrackIndex = (currentTrackIndex + 1) % musicFiles.size
                    playTrack(currentTrackIndex)
                }
            )

            mediaPlayer = player
            duration = player.duration
            trackTitle = title
            trackArtwork = artwork
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    LaunchedEffect(mediaPlayer, isPlaying) {
        while (true) {
            if (isPlaying && mediaPlayer.isPlaying) {
                currentPosition = mediaPlayer.currentPosition
            }
            delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp)
    ) {
        // Track cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 54.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            TrackCover(artwork = trackArtwork)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-42).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 240.dp)
            ) {
                // Track title
                TrackTitle(
                    title = trackTitle
                        ?: musicFiles.getOrNull(currentTrackIndex)?.nameWithoutExtension
                )

                // Progress bar and timer
                TrackProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeekTo = { newPosition ->
                        mediaPlayer.seekTo(newPosition)
                        currentPosition = newPosition
                    }
                )

                // Control buttons
                PlayerControls(
                    isPlaying = isPlaying,
                    playbackMode = playbackMode,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                    },
                    onPreviousClick = {
                        currentTrackIndex =
                            if (currentTrackIndex - 1 < 0) musicFiles.lastIndex else currentTrackIndex - 1
                        playTrack(currentTrackIndex)
                    },
                    onNextClick = {
                        currentTrackIndex = (currentTrackIndex + 1) % musicFiles.size
                        playTrack(currentTrackIndex)
                    },
                    onPlaybackModeChange = { newMode ->
                        playbackMode = newMode
                        // Reload the current track with the new playback mode
                        if (musicFiles.isNotEmpty()) {
                            playTrack(currentTrackIndex)
                        }
                    }
                )
            }
        }
    }
}

// Displays the track cover
@Composable
fun TrackCover(artwork: Bitmap?) {
    val defaultImage = painterResource(id = R.drawable.default_cover)
    val imageModifier = Modifier
        .fillMaxWidth(0.8f)
        .aspectRatio(1f, matchHeightConstraintsFirst = false)
        .clip(RoundedCornerShape(14.dp))

    if (artwork != null) {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = stringResource(R.string.cover),
            modifier = imageModifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            painter = defaultImage,
            contentDescription = stringResource(R.string.default_cover),
            modifier = imageModifier,
            contentScale = ContentScale.Fit
        )
    }
}

// Displays the track name or “Untitled” if no title is available.
@Composable
fun TrackTitle(title: String?) {
    Text(
        text = title ?: stringResource(R.string.untitled),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// Displays a progress bar with drag-and-drop support and current time
@Composable
fun TrackProgressBar(
    currentPosition: Int,
    duration: Int,
    onSeekTo: (Int) -> Unit
) {
    val barWidthPx =
        with(LocalDensity.current) { (0.6f * LocalConfiguration.current.screenWidthDp).dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    val actualProgress = if (!isDragging) {
        if (duration > 0) currentPosition.toFloat() / duration else 0f
    } else {
        (dragOffsetX / barWidthPx).coerceIn(0f, 1f)
    }

    val offsetDp = with(LocalDensity.current) { (actualProgress * barWidthPx).toDp() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = formatTime(currentPosition),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 15.dp)
        )

        Box(
            modifier = Modifier
                .width((0.6f * LocalConfiguration.current.screenWidthDp).dp)
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .offset(x = offsetDp - 6.dp)
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            dragOffsetX = (dragOffsetX + delta).coerceIn(0f, barWidthPx)
                        },
                        onDragStarted = {
                            isDragging = true
                            dragOffsetX = actualProgress * barWidthPx
                        },
                        onDragStopped = {
                            val newPosition = ((dragOffsetX / barWidthPx).coerceIn(
                                0f, 1f
                            ) * duration).toInt()
                            onSeekTo(newPosition)
                            isDragging = false
                        }
                    )
            )
        }

        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 15.dp)
        )
    }
}

// Button with a round icon
@Composable
fun PlayerButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Int,
    iconSize: Int,
    isPrimary: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(size.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(iconSize.dp)
            )
        }
    }
}

// Playback mode switch button
@Composable
fun PlaybackModeButton(
    playbackMode: PlaybackMode,
    onModeChange: (PlaybackMode) -> Unit
) {
    val icon = when (playbackMode) {
        PlaybackMode.PLAY_ALL -> Icons.Filled.Repeat
        PlaybackMode.REPEAT_ONE -> Icons.Filled.RepeatOne
    }

    val description = when (playbackMode) {
        PlaybackMode.PLAY_ALL -> stringResource(R.string.play_all)
        PlaybackMode.REPEAT_ONE -> stringResource(R.string.repeat_one)
    }

    PlayerButton(
        icon = icon,
        contentDescription = description,
        onClick = {
            val newMode = when (playbackMode) {
                PlaybackMode.PLAY_ALL -> PlaybackMode.REPEAT_ONE
                PlaybackMode.REPEAT_ONE -> PlaybackMode.PLAY_ALL
            }
            onModeChange(newMode)
        },
        size = 60,
        iconSize = 24
    )
}

// Set of player control buttons for playback, pause, and track switching

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    playbackMode: PlaybackMode,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Playback mode button
        PlaybackModeButton(
            playbackMode = playbackMode,
            onModeChange = onPlaybackModeChange
        )

        // Main control buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.previous_track),
                onClick = onPreviousClick,
                size = 80,
                iconSize = 36
            )

            Spacer(modifier = Modifier.width(20.dp))

            PlayerButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(
                    R.string.play
                ),
                onClick = onPlayPauseClick,
                size = 100,
                iconSize = 48,
                isPrimary = true
            )

            Spacer(modifier = Modifier.width(20.dp))

            PlayerButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.next_track),
                onClick = onNextClick,
                size = 80,
                iconSize = 36
            )
        }
    }
}

// Formats time in milliseconds into a string of the form “mm:ss”

fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// Loads the track into the player and obtains information about it
fun prepareTrack(
    file: File,
    onCompletion: () -> Unit,
    playbackMode: PlaybackMode = PlaybackMode.PLAY_ALL,
    onPlaybackModeAction: () -> Unit = {},
    onNextTrack: () -> Unit = {}
): Triple<MediaPlayer, String?, Bitmap?> {
    val mediaPlayer = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        prepare()
        setOnCompletionListener {
            when (playbackMode) {
                PlaybackMode.PLAY_ALL -> {
                    // Instead of stopping, move to the next track
                    onNextTrack()
                }

                PlaybackMode.REPEAT_ONE -> {
                    seekTo(0)
                    start()
                    onPlaybackModeAction()
                }
            }
        }
    }

    val retriever = MediaMetadataRetriever().apply {
        setDataSource(file.absolutePath)
    }
    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val artwork = retriever.embeddedPicture?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }
    retriever.release()

    return Triple(mediaPlayer, title, artwork)
}

fun getAllAudioFiles(context: Context, supportedExtensions: List<String>): List<File> {
    val audioFiles = mutableListOf<File>()
    val projection = arrayOf(
        MediaStore.Audio.Media.DATA
    )

    val selection = null
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val cursor = context.contentResolver.query(
        uri,
        projection,
        selection,
        null,
        null
    )

    cursor?.use {
        val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        while (cursor.moveToNext()) {
            val filePath = cursor.getString(dataIndex)
            val file = File(filePath)
            if (file.exists() && supportedExtensions.contains(file.extension.lowercase())) {
                audioFiles.add(file)
            }
        }
    }

    return audioFiles.sortedBy { it.name }
}