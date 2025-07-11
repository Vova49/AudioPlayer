package com.example.audioplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Environment
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MainScreen() {

    val musicDir = File(Environment.getExternalStorageDirectory(), "AppMusic")
    val musicFiles = remember {
        musicDir
            ?.listFiles()
            ?.filter { it.extension.lowercase() == "mp3" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    var currentTrackIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var trackTitle by remember { mutableStateOf<String?>(null) }
    var trackArtwork by remember { mutableStateOf<Bitmap?>(null) }

    var mediaPlayer by remember {
        mutableStateOf(MediaPlayer())
    }

    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }

    // Загружаем и подготавливаем первый трек
    LaunchedEffect(musicFiles) {
        if (musicFiles.isNotEmpty()) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(musicFiles[currentTrackIndex].absolutePath)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration

            // метаданные
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(musicFiles[currentTrackIndex].absolutePath)

            trackTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            retriever.embeddedPicture?.let {
                trackArtwork = BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: run {
                trackArtwork = null
            }
            retriever.release()

            mediaPlayer.setOnCompletionListener {
                isPlaying = false
            }
        }
    }

    fun playTrack(index: Int) {
        if (index in musicFiles.indices) {
            mediaPlayer.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(musicFiles[index].absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            duration = mediaPlayer.duration
            isPlaying = true

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(musicFiles[index].absolutePath)
            trackTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            retriever.embeddedPicture?.let {
                trackArtwork = BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: run {
                trackArtwork = null
            }
            retriever.release()
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 54.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val defaultImage = painterResource(id = R.drawable.default_cover)
            val artwork = trackArtwork

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
                // Название
                Text(
                    text = trackTitle
                        ?: musicFiles.getOrNull(currentTrackIndex)?.nameWithoutExtension
                        ?: stringResource(R.string.untitled),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

                fun formatTime(ms: Int): String {
                    val totalSeconds = ms / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    return "%02d:%02d".format(minutes, seconds)
                }

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
                                        mediaPlayer.seekTo(newPosition)
                                        currentPosition = newPosition
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

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                currentTrackIndex =
                                    if (currentTrackIndex - 1 < 0) musicFiles.lastIndex else currentTrackIndex - 1
                                playTrack(currentTrackIndex)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = stringResource(R.string.previous_track),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable {
                                if (isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(
                                    R.string.play
                                ),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                currentTrackIndex = (currentTrackIndex + 1) % musicFiles.size
                                playTrack(currentTrackIndex)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.next_track),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
