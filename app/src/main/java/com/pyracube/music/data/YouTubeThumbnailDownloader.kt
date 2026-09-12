package com.pyracube.music.data

import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class YouTubeThumbnailDownloader {

    fun download(
        thumbnailUrl: String,
        destinationFile: File
    ): Boolean {
        val connection =
            URL(thumbnailUrl).openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return false
            }

            connection.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            destinationFile.exists() &&
                    destinationFile.length() > 0L &&
                    BitmapFactory.decodeFile(destinationFile.absolutePath) != null

        } finally {
            connection.disconnect()
        }
    }
}