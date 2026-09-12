package com.pyracube.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val duration: Long,
    val artworkUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)