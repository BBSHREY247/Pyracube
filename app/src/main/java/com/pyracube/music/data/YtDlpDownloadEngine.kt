package com.pyracube.music.data

import android.content.Context
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import java.io.File
import java.util.concurrent.Future

class YtDlpDownloadEngine(
    context: Context
) {
    private val appContext = context.applicationContext

    fun download(
        url: String,
        outputDirectory: File = File(
            appContext.getExternalFilesDir(null),
            "downloads"
        ),
        onProgress: (Int) -> Unit = {}
    ): Future<*> {

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputTemplate = File(
            outputDirectory,
            "%(title)s.%(ext)s"
        ).absolutePath

        val request =
            YtDlpRequest(url)
                .setOutputTemplate(outputTemplate)
                .addOption("-f", "bestaudio/best")
                .addOption("--no-playlist")

        return YtDlp.executeAsync(
            request
        ) { progress, _, _ ->

            onProgress(
                progress
                    .toInt()
                    .coerceIn(0, 100)
            )
        }
    }
    fun getYtDlpDiagnostics(): String {
        return try {
            val python = com.chaquo.python.Python.getInstance()

            val modules = listOf(
                "yt_dlp_ejs",
                "quickjs",
                "deno",
                "nodejs"
            )

            val result = StringBuilder()

            for (name in modules) {
                val available =
                    try {
                        python.getModule(name)
                        true
                    } catch (_: Exception) {
                        false
                    }

                result.append("$name = $available\n")
            }

            result.toString().trim()

        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
}