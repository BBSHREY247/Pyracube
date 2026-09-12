package com.pyracube.music.data

import android.net.Uri

class YouTubeThumbnailResolver {

    fun resolve(url: String): List<String> {
        val uri = Uri.parse(url)

        val videoId = when {
            uri.host == "youtu.be" ->
                uri.pathSegments.firstOrNull()

            uri.host == "www.youtube.com" ||
                    uri.host == "youtube.com" ->
                uri.getQueryParameter("v")

            else -> null
        }

        if (videoId == null || videoId.length != 11) {
            return emptyList()
        }

        return listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/sddefault.jpg",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/default.jpg"
        )
    }
}