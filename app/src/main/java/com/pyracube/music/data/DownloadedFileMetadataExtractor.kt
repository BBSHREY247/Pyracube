package com.pyracube.music.data

import android.media.MediaMetadataRetriever
import java.io.File

class DownloadedFileMetadataExtractor {

    fun extract(
        file: File,
        fallbackTitle: String,
        fallbackArtist: String
    ): MediaMetadata {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)

            val title =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_TITLE
                )?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle

            val artist =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_ARTIST
                )?.takeIf { it.isNotBlank() }
                    ?: fallbackArtist

            val album =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_ALBUM
                )?.takeIf { it.isNotBlank() }
                    ?: "Unknown Album"

            MediaMetadata(
                title = title,
                artist = artist,
                album = album,
            )
        } finally {
            retriever.release()
        }
    }
}