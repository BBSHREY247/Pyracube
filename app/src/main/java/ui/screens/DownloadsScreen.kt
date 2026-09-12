package com.pyracube.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pyracube.music.data.DownloadTaskEntity
import com.pyracube.music.data.DownloadRepository
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextPrimary
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import com.pyracube.music.data.PlaylistResult

@Composable
fun DownloadsScreen(
    downloads: List<DownloadTaskEntity>,
    downloadRepository: DownloadRepository,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PyracubeBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Text(
                text = "Downloads",
                color = PyracubeTextPrimary,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Download music from supported media URLs or YouTube playlists",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    feedbackMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                label = {
                    Text("Song, media, or playlist URL")
                },
                placeholder = {
                    Text("Paste a URL here")
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()

                    if (trimmedUrl.isBlank()) {
                        feedbackMessage = "Please enter a URL."
                        isFeedbackError = true
                        return@Button
                    }

                    isLoading = true
                    feedbackMessage = null

                    scope.launch {
                        try {
                            if (downloadRepository.isPlaylist(trimmedUrl)) {
                                when (val result = downloadRepository.addPlaylistToQueue(trimmedUrl)) {
                                    is PlaylistResult.Success -> {
                                        val parts = mutableListOf<String>()
                                        parts.add("Added ${result.tasksCreated} song(s) to queue")
                                        if (result.duplicatesSkipped > 0) {
                                            parts.add("${result.duplicatesSkipped} duplicate(s) skipped")
                                        }
                                        if (result.unavailableSkipped > 0) {
                                            parts.add("${result.unavailableSkipped} unavailable skipped")
                                        }
                                        feedbackMessage = parts.joinToString(", ")
                                        isFeedbackError = false
                                        url = ""
                                    }
                                    is PlaylistResult.Error -> {
                                        feedbackMessage = "Playlist error: ${result.message}"
                                        isFeedbackError = true
                                    }
                                }
                            } else {
                                val taskId = downloadRepository.addDownload(
                                    sourceUrl = trimmedUrl,
                                    title = "Pending metadata",
                                    artist = "Unknown Artist",
                                    fileName = "pending_download"
                                )

                                downloadRepository.startDownload(taskId)

                                feedbackMessage = "Added song to queue"
                                isFeedbackError = false
                                url = ""
                            }
                        } catch (e: Exception) {
                            feedbackMessage = "Error: ${e.message}"
                            isFeedbackError = true
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(if (isLoading) "Processing..." else "Add to Queue")
            }
        }

        feedbackMessage?.let { msg ->
            item {
                Text(
                    text = msg,
                    color = if (isFeedbackError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Text(
                text = "Download Queue",
                color = PyracubeTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        if (downloads.isEmpty()) {
            item {
                Text(
                    text = "No downloads yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = downloads,
                key = { it.id }
            ) { download ->
                DownloadCard(
                    download = download,
                    onCancel = { id ->
                        scope.launch {
                            downloadRepository.cancel(id)
                        }
                    },
                    onRetry = { id ->
                        scope.launch {
                            downloadRepository.retry(id)
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            downloadRepository.delete(id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadCard(
    download: DownloadTaskEntity,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = download.title,
            color = PyracubeTextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = download.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = download.sourceUrl,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = when (download.sourceType) {
                "DIRECT_AUDIO" -> "Direct audio"
                "WEB_MEDIA" -> "Web media"
                else -> "Unknown source"
            },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (download.status == DownloadTaskEntity.STATUS_DOWNLOADING) {
            LinearProgressIndicator(
                progress = { download.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = download.status,
                color = when (download.status) {
                    DownloadTaskEntity.STATUS_FAILED -> MaterialTheme.colorScheme.error
                    DownloadTaskEntity.STATUS_CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    DownloadTaskEntity.STATUS_DOWNLOADING -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (download.status == DownloadTaskEntity.STATUS_DOWNLOADING) {
                Text(
                    text = "${download.progress}%",
                    color = PyracubeTextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (!download.errorMessage.isNullOrBlank() && download.status == DownloadTaskEntity.STATUS_FAILED) {
            Text(
                text = download.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (download.status) {
                DownloadTaskEntity.STATUS_DOWNLOADING,
                DownloadTaskEntity.STATUS_PENDING -> {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onCancel(download.id) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                DownloadTaskEntity.STATUS_FAILED,
                DownloadTaskEntity.STATUS_CANCELLED -> {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onDelete(download.id) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Remove")
                    }

                    Button(
                        onClick = { onRetry(download.id) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restart")
                    }
                }
            }
        }
    }
}
