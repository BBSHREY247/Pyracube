package com.pyracube.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pyracube.music.data.PlaybackQueueEntity
import com.pyracube.music.player.PlayerState
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextPrimary
import com.pyracube.music.ui.theme.PyracubeTextSecondary

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {}
) {

    BackHandler {
        onBack()
    }

    var isSeeking by remember {
        mutableStateOf(false)
    }

    var seekFraction by remember {
        mutableFloatStateOf(0f)
    }

    val actualFraction =
        if (playerState.duration > 0L) {
            (playerState.position.toFloat() /
                    playerState.duration.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    val displayedFraction =
        if (isSeeking) {
            seekFraction
        } else {
            actualFraction
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PyracubeBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Album artwork
        if (!playerState.artworkUri.isNullOrBlank()) {

            AsyncImage(
                model = playerState.artworkUri,
                contentDescription = "${playerState.title} artwork",
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

        } else {

            // Fallback artwork
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PyracubeAccent,
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // Song title
        Text(
            text = playerState.title,
            color = PyracubeTextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        // Artist
        Text(
            text = playerState.artist,
            color = PyracubeTextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // Smooth seek bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(
                    playerState.duration
                ) {

                    detectTapGestures { offset ->

                        if (playerState.duration > 0L) {

                            val fraction =
                                (
                                        offset.x /
                                                size.width.toFloat()
                                        ).coerceIn(0f, 1f)

                            seekFraction = fraction

                            onSeek(
                                (playerState.duration *
                                        fraction)
                                    .toLong()
                            )
                        }
                    }
                }
                .pointerInput(
                    playerState.duration
                ) {

                    detectDragGestures(

                        onDragStart = { offset ->

                            if (playerState.duration > 0L) {

                                isSeeking = true

                                seekFraction =
                                    (
                                            offset.x /
                                                    size.width.toFloat()
                                            ).coerceIn(0f, 1f)
                            }
                        },

                        onDrag = { change, _ ->

                            if (playerState.duration > 0L) {

                                change.consume()

                                seekFraction =
                                    (
                                            change.position.x /
                                                    size.width.toFloat()
                                            ).coerceIn(0f, 1f)
                            }
                        },

                        onDragEnd = {

                            if (playerState.duration > 0L) {

                                val newPosition =
                                    (
                                            playerState.duration *
                                                    seekFraction
                                            ).toLong()

                                onSeek(newPosition)
                            }

                            isSeeking = false
                        },

                        onDragCancel = {
                            isSeeking = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {

            LinearProgressIndicator(
                progress = { displayedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(
                        RoundedCornerShape(50)
                    ),
                color = PyracubeAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = formatTime(
                    if (isSeeking) {
                        (
                                playerState.duration *
                                        seekFraction
                                ).toLong()
                    } else {
                        playerState.position
                    }
                ),
                color = PyracubeTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = formatTime(
                    playerState.duration
                ),
                color = PyracubeTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Repeat controls (shuffle disabled pending fix)
        if (playerState.hasQueue) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button disabled
                // TODO: Re-enable when shuffle is fixed
                Spacer(modifier = Modifier.size(48.dp))

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (playerState.repeatMode) {
                            PlaybackQueueEntity.REPEAT_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = when (playerState.repeatMode) {
                            PlaybackQueueEntity.REPEAT_ONE -> "Repeat One"
                            PlaybackQueueEntity.REPEAT_ALL -> "Repeat All"
                            else -> "Repeat Off"
                        },
                        tint = if (playerState.repeatMode != PlaybackQueueEntity.REPEAT_OFF) {
                            PyracubeAccent
                        } else {
                            PyracubeTextSecondary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.Center,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(56.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = PyracubeTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(
                            RoundedCornerShape(50)
                        )
                        .background(PyracubeAccent),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            if (playerState.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                        contentDescription =
                            if (playerState.isPlaying) {
                                "Pause"
                            } else {
                                "Play"
                            },
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(56.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = PyracubeTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatTime(
    milliseconds: Long
): String {

    val totalSeconds =
        milliseconds.coerceAtLeast(0L) / 1000

    val minutes =
        totalSeconds / 60

    val seconds =
        totalSeconds % 60

    return "%d:%02d".format(
        minutes,
        seconds
    )
}
