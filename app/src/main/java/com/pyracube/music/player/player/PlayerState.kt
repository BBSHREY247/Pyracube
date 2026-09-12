package com.pyracube.music.player

data class PlayerState(
    val title: String = "Nothing Playing",
    val artist: String = "Choose a song to start listening",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: String = "OFF",
    val hasQueue: Boolean = false
)
