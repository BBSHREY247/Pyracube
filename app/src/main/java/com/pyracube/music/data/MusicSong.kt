package com.pyracube.music.data

data class MusicSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val duration: Long,
    val artworkUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

fun MusicSong.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        uri = uri,
        duration = duration,
        artworkUri = artworkUri,
        dateAdded = dateAdded
    )
}