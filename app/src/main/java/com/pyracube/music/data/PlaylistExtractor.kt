package com.pyracube.music.data

import dev.ffmpegkit_maintained.ytdlp.YtDlp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PlaylistExtractor {

    suspend fun extract(url: String): List<PlaylistEntry> = withContext(Dispatchers.IO) {
        val jsonResult = try {
            YtDlp.extractPlaylistInfoAsync(url).get()
        } catch (e: Exception) {
            val causeMsg = e.cause?.message ?: e.message ?: "Playlist extraction failed"
            throw Exception(causeMsg)
        }

        if (jsonResult.isBlank() || jsonResult == "[]") {
            return@withContext emptyList()
        }

        val entries = mutableListOf<PlaylistEntry>()
        val jsonArray = JSONArray(jsonResult)
        val limit = minOf(jsonArray.length(), 500)

        for (i in 0 until limit) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val videoUrl = obj.optString("url", "").trim()
            val videoId = obj.optString("id", "").trim().takeIf { it.isNotBlank() }
            val title = obj.optString("title", "Unknown Title").trim().ifBlank { "Unknown Title" }
            val artist = obj.optString("uploader", "Unknown Artist").trim().ifBlank { "Unknown Artist" }

            if (videoUrl.isNotBlank()) {
                entries.add(
                    PlaylistEntry(
                        videoUrl = videoUrl,
                        videoId = videoId,
                        title = title,
                        artist = artist
                    )
                )
            }
        }

        entries
    }
}
