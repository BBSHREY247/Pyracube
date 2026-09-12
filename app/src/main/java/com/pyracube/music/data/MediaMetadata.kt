package com.pyracube.music.data

data class MediaMetadata(
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val thumbnailUrl: String? = null,
    val artworkUri: String? = null
)