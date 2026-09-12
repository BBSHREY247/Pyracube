package com.pyracube.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(

    @PrimaryKey(autoGenerate =
        true)
    val id: Long = 0L,

    val sourceUrl: String,

    val sourceType: String = "UNKNOWN",

    val title: String,

    val artist: String = "Unknown Artist",

    val thumbnailUrl: String? = null,

    val fileName: String,

    val status: String = STATUS_PENDING,

    val progress: Int = 0,

    val errorMessage: String? = null,

    val createdAt: Long = System.currentTimeMillis()
) {

    companion object {

        const val STATUS_PENDING = "PENDING"

        const val STATUS_DOWNLOADING = "DOWNLOADING"

        const val STATUS_COMPLETED = "COMPLETED"

        const val STATUS_FAILED = "FAILED"

        const val STATUS_CANCELLED = "CANCELLED"
    }
}