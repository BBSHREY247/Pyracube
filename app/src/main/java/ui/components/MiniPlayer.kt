package com.pyracube.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pyracube.music.player.PlayerState
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextPrimary
import com.pyracube.music.ui.theme.PyracubeTextSecondary

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onClick: () -> Unit = {},
    onPlayPause: () -> Unit = {}
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PyracubeBackground)
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Album artwork
        if (!playerState.artworkUri.isNullOrBlank()) {

            AsyncImage(
                model = playerState.artworkUri,
                contentDescription = "${playerState.title} artwork",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

        } else {

            // Fallback artwork
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        androidx.compose.material3.MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    )
            ) {

                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PyracubeAccent,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = playerState.title,
                color = PyracubeTextPrimary,
                fontSize = 14.sp,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.size(2.dp)
            )

            Text(
                text = playerState.artist,
                color = PyracubeTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        IconButton(
            onClick = onPlayPause
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
                tint = PyracubeAccent
            )
        }
    }
}