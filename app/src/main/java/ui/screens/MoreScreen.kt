package com.pyracube.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pyracube.music.data.MusicStorageManager
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextPrimary

@Composable
fun MoreScreen(
    storageManager: MusicStorageManager,
    onMusicStorageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PyracubeBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "More",
            color = PyracubeTextPrimary,
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Pyracube settings and options",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        MoreOption(
            icon = Icons.Default.Storage,
            title = "Music Storage",
            subtitle =
                if (storageManager.hasMusicFolder()) {
                    "Music folder selected"
                } else {
                    "Choose where your music is stored"
                },
            onClick = onMusicStorageClick
        )

        MoreOption(
            icon = Icons.Default.Settings,
            title = "Playback Settings",
            subtitle = "Configure playback behavior",
            onClick = {}
        )

        MoreOption(
            icon = Icons.Default.Info,
            title = "About Pyracube",
            subtitle = "Version and application information",
            onClick = {}
        )
    }
}

@Composable
private fun MoreOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PyracubeAccent,
            modifier = Modifier.size(30.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {

            Text(
                text = title,
                color = PyracubeTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}