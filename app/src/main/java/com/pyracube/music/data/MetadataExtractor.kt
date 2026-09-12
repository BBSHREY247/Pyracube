package com.pyracube.music.data

interface MetadataExtractor {

    suspend fun extract(
        url: String
    ): MediaMetadata
}