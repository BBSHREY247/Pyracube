package com.pyracube.music.data

import android.net.Uri

class MediaSourceDetector {

    fun detect(url: String): MediaSource {
        val trimmedUrl = url.trim()

        if (trimmedUrl.isBlank()) {
            return MediaSource.UNKNOWN
        }

        val uri = try {
            Uri.parse(trimmedUrl)
        } catch (_: Exception) {
            return MediaSource.UNKNOWN
        }

        val scheme = uri.scheme?.lowercase()

        if (scheme != "http" && scheme != "https") {
            return MediaSource.UNKNOWN
        }

        val path = uri.path?.lowercase() ?: ""

        return when {
            path.endsWith(".mp3") ||
                    path.endsWith(".m4a") ||
                    path.endsWith(".aac") ||
                    path.endsWith(".flac") ||
                    path.endsWith(".wav") ||
                    path.endsWith(".ogg") ||
                    path.endsWith(".opus") -> {
                MediaSource.DIRECT_AUDIO
            }

            else -> {
                MediaSource.WEB_MEDIA
            }
        }
    }
}