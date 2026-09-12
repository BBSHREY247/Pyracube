package com.pyracube.music.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class YouTubeThumbnailTest(
    private val context: Context
) {

    suspend fun run(url: String): Boolean =
        withContext(Dispatchers.IO) {

            val resolver = YouTubeThumbnailResolver()
            val thumbnailUrls = resolver.resolve(url)

            if (thumbnailUrls.isEmpty()) {
                Log.e(TAG, "Could not resolve thumbnail URLs")
                return@withContext false
            }

            val outputFile =
                File(
                    context.getExternalFilesDir(null),
                    "test_thumbnail.jpg"
                )

            val downloader = YouTubeThumbnailDownloader()

            for (thumbnailUrl in thumbnailUrls) {

                Log.d(TAG, "Trying thumbnail URL: $thumbnailUrl")

                val success =
                    downloader.download(
                        thumbnailUrl = thumbnailUrl,
                        destinationFile = outputFile
                    )

                if (success) {
                    Log.d(TAG, "Thumbnail download success: true")
                    Log.d(TAG, "Thumbnail path: ${outputFile.absolutePath}")
                    return@withContext true
                }

                Log.d(TAG, "Thumbnail failed: $thumbnailUrl")
            }

            Log.e(TAG, "All thumbnail URLs failed")
            false
        }

    companion object {
        private const val TAG = "YouTubeThumbnailTest"
    }
}