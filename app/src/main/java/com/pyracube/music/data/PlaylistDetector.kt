package com.pyracube.music.data

import android.net.Uri

class PlaylistDetector {

    fun isYouTubePlaylist(url: String): Boolean {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return false

        val uri = try {
            Uri.parse(trimmedUrl)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host?.lowercase() ?: ""
        if (!host.contains("youtube.com") && !host.contains("youtu.be")) {
            return false
        }

        val listParam = uri.getQueryParameter("list")
        if (listParam.isNullOrBlank()) {
            return false
        }

        val path = uri.path?.lowercase() ?: ""
        val videoParam = uri.getQueryParameter("v")

        if (path.contains("watch") || !videoParam.isNullOrBlank()) {
            return false
        }

        if (host.contains("youtu.be")) {
            return false
        }

        return path.contains("playlist") || path.contains("videoseries")
    }

    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        return try {
            val uri = Uri.parse(trimmed)
            val host = uri.host?.lowercase() ?: ""
            if (!host.contains("youtube.com") && !host.contains("youtu.be")) {
                return null
            }

            val vParam = uri.getQueryParameter("v")
            if (!vParam.isNullOrBlank()) {
                return vParam.trim()
            }

            if (host.contains("youtu.be")) {
                val segment = uri.pathSegments.firstOrNull()?.trim()
                if (!segment.isNullOrBlank()) return segment
            }

            val segments = uri.pathSegments
            val markerIndex = segments.indexOfFirst { it == "shorts" || it == "embed" || it == "v" }
            if (markerIndex != -1 && markerIndex + 1 < segments.size) {
                return segments[markerIndex + 1].trim()
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    fun canonicalVideoUrl(url: String): String {
        val videoId = extractVideoId(url)
        return if (videoId != null) {
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            url.trim()
        }
    }
}
