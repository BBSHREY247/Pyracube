package com.pyracube.music.data

class BasicMetadataExtractor : MetadataExtractor {

    override suspend fun extract(
        url: String
    ): MediaMetadata {

        val fileName = url
            .substringAfterLast("/")
            .substringBefore("?")
            .substringBefore("#")

        val title =
            fileName
                .substringBeforeLast(".")
                .takeIf { it.isNotBlank() }
                ?: "Unknown Title"

        return MediaMetadata(
            title = title
        )
    }
}