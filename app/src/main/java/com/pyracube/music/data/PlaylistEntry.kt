package com.pyracube.music.data

data class PlaylistEntry(
    val videoUrl: String,
    val videoId: String?,
    val title: String,
    val artist: String = "Unknown Artist"
)
