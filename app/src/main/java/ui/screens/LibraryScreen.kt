package com.pyracube.music.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pyracube.music.data.LibraryPreferences
import com.pyracube.music.data.SongEntity
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextPrimary
import java.io.File

enum class SortField(val key: String, val label: String) {
    TITLE("TITLE", "Title"),
    ARTIST("ARTIST", "Artist"),
    DATE_ADDED("DATE_ADDED", "Date Added")
}

enum class SortDirection(val label: String) {
    ASCENDING("Oldest / A–Z"),
    DESCENDING("Newest / Z–A")
}

data class LibrarySort(
    val field: SortField,
    val direction: SortDirection
)

private fun LibrarySort.displayLabel(): String {
    return when (field) {
        SortField.TITLE -> if (direction == SortDirection.ASCENDING) "Title A–Z" else "Title Z–A"
        SortField.ARTIST -> if (direction == SortDirection.ASCENDING) "Artist A–Z" else "Artist Z–A"
        SortField.DATE_ADDED -> if (direction == SortDirection.ASCENDING) "Date Added (Oldest)" else "Date Added (Newest)"
    }
}

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val libraryPreferences = remember { LibraryPreferences(context) }

    var showSortMenu by remember { mutableStateOf(false) }
    var currentSort by remember {
        mutableStateOf(
            LibrarySort(
                field = SortField.valueOf(libraryPreferences.getSortField()),
                direction = if (libraryPreferences.isSortAscending()) SortDirection.ASCENDING else SortDirection.DESCENDING
            )
        )
    }

    val sortedSongs = remember(songs, currentSort) {
        when (currentSort.field) {
            SortField.TITLE -> {
                val sorted = songs.sortedBy { it.title.lowercase() }
                if (currentSort.direction == SortDirection.DESCENDING) sorted.reversed() else sorted
            }
            SortField.ARTIST -> {
                val sorted = songs.sortedBy { it.artist.lowercase() }
                if (currentSort.direction == SortDirection.DESCENDING) sorted.reversed() else sorted
            }
            SortField.DATE_ADDED -> {
                if (currentSort.direction == SortDirection.DESCENDING) {
                    songs.sortedByDescending { it.dateAdded }
                } else {
                    songs.sortedBy { it.dateAdded }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PyracubeBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 20.dp
        )
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Library",
                        color = PyracubeTextPrimary,
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Text(
                        text = "Your offline music collection",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PyracubeAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh library",
                            tint = PyracubeAccent
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Songs (${songs.size})",
                    color = PyracubeTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp)
                )

                Box {
                    Text(
                        text = currentSort.displayLabel(),
                        color = PyracubeAccent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .clickable { showSortMenu = true }
                    )

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortField.entries.forEach { field ->
                            SortDirection.entries.forEach { direction ->
                                val sort = LibrarySort(field, direction)
                                val isSelected = sort == currentSort
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.displayLabel(),
                                            color = if (isSelected) PyracubeAccent else PyracubeTextPrimary
                                        )
                                    },
                                    onClick = {
                                        currentSort = sort
                                        libraryPreferences.saveSort(
                                            field = field.key,
                                            ascending = direction == SortDirection.ASCENDING
                                        )
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (songs.isEmpty()) {

            item {
                Text(
                    text = "No music found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

        } else {

            items(
                items = sortedSongs,
                key = { song -> song.id }
            ) { song ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            onSongClick(song, sortedSongs)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Album artwork
                    if (song.artworkUri != null) {

                        AsyncImage(
                            model = File(song.artworkUri),
                            contentDescription = "${song.title} artwork",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        // Fallback when artwork is unavailable
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PyracubeAccent,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {

                        Text(
                            text = song.title,
                            color = PyracubeTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )

                        Text(
                            text = "${song.artist} • ${formatDuration(song.duration)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {

    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}
