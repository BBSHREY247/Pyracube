package com.pyracube.music.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.Future

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getInstance(appContext)
    private val downloadDao = database.downloadTaskDao()
    private val storageManager = MusicStorageManager(appContext)
    private val playlistDetector = PlaylistDetector()
    private val queueController = DownloadQueueController.getInstance(appContext)

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)

        if (taskId == -1L) {
            return Result.failure()
        }

        val task = downloadDao.getTask(taskId)
            ?: return Result.failure()

        // If task was already cancelled or worker is stopped before starting
        if (isStopped || task.status == DownloadTaskEntity.STATUS_CANCELLED) {
            downloadDao.updateStatus(taskId, DownloadTaskEntity.STATUS_CANCELLED)
            queueController.onTaskFinished(taskId)
            return Result.failure()
        }

        downloadDao.updateProgress(
            taskId,
            0,
            DownloadTaskEntity.STATUS_DOWNLOADING
        )

        return try {
            val result = when (MediaSourceDetector().detect(task.sourceUrl)) {
                MediaSource.DIRECT_AUDIO -> downloadDirectAudio(task)

                MediaSource.WEB_MEDIA -> {
                    if (task.sourceUrl.contains("youtube.com") ||
                        task.sourceUrl.contains("youtu.be")
                    ) {
                        downloadYouTube(task)
                    } else {
                        fail(taskId, "This website is not supported yet.")
                    }
                }

                MediaSource.UNKNOWN -> fail(
                    taskId,
                    "Unsupported or invalid URL."
                )
            }
            queueController.onTaskFinished(taskId)
            result
        } catch (e: Exception) {
            if (isStopped || e is CancellationException || e is InterruptedException) {
                downloadDao.updateStatus(taskId, DownloadTaskEntity.STATUS_CANCELLED)
                queueController.onTaskFinished(taskId)
                Result.failure()
            } else {
                val result = fail(
                    taskId,
                    e.message ?: "Download failed"
                )
                queueController.onTaskFinished(taskId)
                result
            }
        }
    }

    private suspend fun downloadDirectAudio(
        task: DownloadTaskEntity
    ): Result = withContext(Dispatchers.IO) {

        // Task-isolated temporary directory: downloads/task_<taskId>/
        val taskDirectory = File(
            applicationContext.getExternalFilesDir(null),
            "downloads/task_${task.id}"
        )

        // Protect against stale files from a previous or cancelled run
        if (taskDirectory.exists()) {
            taskDirectory.deleteRecursively()
        }
        taskDirectory.mkdirs()

        val extension = getExtensionFromUrl(task.sourceUrl)
        val temporaryFile = File(
            taskDirectory,
            "${task.id}_download.$extension"
        )

        val connection = URL(task.sourceUrl).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw Exception("HTTP ${connection.responseCode}")
            }

            val totalBytes = connection.contentLengthLong

            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var lastProgressTime = 0L
                    var lastProgressValue = -1

                    while (true) {
                        if (isStopped) {
                            taskDirectory.deleteRecursively()
                            downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
                            return@withContext Result.failure()
                        }

                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) {
                            break
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            val now = System.currentTimeMillis()
                            if (progress != lastProgressValue && (now - lastProgressTime >= 300 || progress == 100)) {
                                lastProgressTime = now
                                lastProgressValue = progress
                                if (!isStopped) {
                                    downloadDao.updateProgress(
                                        task.id,
                                        progress,
                                        DownloadTaskEntity.STATUS_DOWNLOADING
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            taskDirectory.deleteRecursively()
            if (isStopped || e is CancellationException || e is InterruptedException) {
                downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
                return@withContext Result.failure()
            }
            throw e
        } finally {
            connection.disconnect()
        }

        if (isStopped) {
            taskDirectory.deleteRecursively()
            downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
            return@withContext Result.failure()
        }

        if (!temporaryFile.exists() || temporaryFile.length() == 0L) {
            taskDirectory.deleteRecursively()
            throw Exception("Downloaded file is empty.")
        }

        val metadataExtractor = DownloadedFileMetadataExtractor()
        val metadata = metadataExtractor.extract(
            file = temporaryFile,
            fallbackTitle = task.title,
            fallbackArtist = task.artist
        )

        val metadataFileName = buildSafeFileName(metadata.title, extension)

        downloadDao.updateMetadata(
            id = task.id,
            title = metadata.title,
            artist = metadata.artist,
            fileName = metadataFileName
        )

        val musicFolder = storageManager.getMusicFolder()
            ?: run {
                taskDirectory.deleteRecursively()
                throw Exception("Please choose a Music Storage folder first.")
            }

        val destinationDirectory = DocumentFile.fromTreeUri(
            applicationContext,
            musicFolder
        ) ?: run {
            taskDirectory.deleteRecursively()
            throw Exception("Selected Music Storage folder is unavailable.")
        }

        val finalDestinationFile = resolveDestinationFile(
            destinationDirectory = destinationDirectory,
            cleanTitle = metadata.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "Downloaded Song" },
            extension = extension,
            sourceIdentifier = "task_${task.id}"
        ) ?: run {
            taskDirectory.deleteRecursively()
            throw Exception("Could not create destination file.")
        }

        try {
            applicationContext.contentResolver.openOutputStream(finalDestinationFile.uri)?.use { output ->
                temporaryFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (isStopped) {
                            if (finalDestinationFile.length() == 0L) finalDestinationFile.delete()
                            taskDirectory.deleteRecursively()
                            downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
                            return@withContext Result.failure()
                        }
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                    }
                }
            } ?: run {
                if (finalDestinationFile.length() == 0L) finalDestinationFile.delete()
                taskDirectory.deleteRecursively()
                throw Exception("Could not open destination file.")
            }
        } catch (e: Exception) {
            if (finalDestinationFile.length() == 0L) finalDestinationFile.delete()
            taskDirectory.deleteRecursively()
            throw e
        }

        taskDirectory.deleteRecursively()

        try {
            MusicRepository(applicationContext).syncMusic()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Music sync failed after direct audio download", e)
        }

        downloadDao.deleteTaskById(task.id)
        Result.success()
    }

    private suspend fun downloadYouTube(
        task: DownloadTaskEntity
    ): Result = withContext(Dispatchers.IO) {

        // Task-isolated temporary directory: downloads/task_<taskId>/
        val taskDirectory = File(
            applicationContext.getExternalFilesDir(null),
            "downloads/task_${task.id}"
        )

        // Stale file protection: wipe and recreate directory before starting
        if (taskDirectory.exists()) {
            taskDirectory.deleteRecursively()
        }
        taskDirectory.mkdirs()

        val engine = YtDlpDownloadEngine(applicationContext)
        var future: Future<*>? = null
        val workerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        var lastProgressTime = 0L
        var lastProgressValue = -1

        try {
            future = engine.download(
                url = task.sourceUrl,
                outputDirectory = taskDirectory,
                onProgress = { progress ->
                    if (!isStopped) {
                        val now = System.currentTimeMillis()
                        if (progress != lastProgressValue && (now - lastProgressTime >= 300 || progress == 100)) {
                            lastProgressTime = now
                            lastProgressValue = progress
                            workerScope.launch {
                                if (!isStopped) {
                                    downloadDao.updateProgress(
                                        task.id,
                                        progress,
                                        DownloadTaskEntity.STATUS_DOWNLOADING
                                    )
                                }
                            }
                        }
                    }
                }
            )

            future.get()
        } catch (e: Exception) {
            future?.cancel(true)
            workerScope.cancel()
            taskDirectory.deleteRecursively()

            if (isStopped || e is CancellationException || e is InterruptedException) {
                Log.i("DownloadWorker", "Task ${task.id} was stopped or cancelled.")
                downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
                return@withContext Result.failure()
            }

            Log.e("DownloadWorker", "YouTube download failed for task ${task.id}", e)
            return@withContext fail(
                task.id,
                e.cause?.message ?: e.message ?: "YouTube download failed"
            )
        } finally {
            workerScope.cancel()
        }

        if (isStopped) {
            taskDirectory.deleteRecursively()
            downloadDao.updateStatus(task.id, DownloadTaskEntity.STATUS_CANCELLED)
            return@withContext Result.failure()
        }

        // Stale file protection: find output file ONLY inside this task's directory
        val supportedExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "webm")
        val downloadedFiles = taskDirectory.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val audioFile = downloadedFiles.firstOrNull()
            ?: run {
                taskDirectory.deleteRecursively()
                return@withContext fail(
                    task.id,
                    "yt-dlp finished but no audio file was found in task directory."
                )
            }

        val extension = audioFile.extension.lowercase()
        val extractedTitle = audioFile.nameWithoutExtension.takeIf { it.isNotBlank() } ?: task.title

        val metadata = DownloadedFileMetadataExtractor().extract(
            file = audioFile,
            fallbackTitle = extractedTitle,
            fallbackArtist = task.artist
        )

        val cleanTitle = extractedTitle
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .ifBlank { "Downloaded Song" }

        val safeFileName = "$cleanTitle.$extension"

        downloadDao.updateMetadata(
            id = task.id,
            title = extractedTitle,
            artist = metadata.artist,
            fileName = safeFileName
        )

        val musicFolder = storageManager.getMusicFolder()
            ?: run {
                taskDirectory.deleteRecursively()
                return@withContext fail(task.id, "Please choose a Music Storage folder first.")
            }

        val destinationDirectory = DocumentFile.fromTreeUri(
            applicationContext,
            musicFolder
        ) ?: run {
            taskDirectory.deleteRecursively()
            return@withContext fail(task.id, "Selected Music Storage folder is unavailable.")
        }

        val videoId = playlistDetector.extractVideoId(task.sourceUrl)
        val sourceIdentifier = videoId ?: "task_${task.id}"

        Log.d("DownloadWorker", "Writing destination file: name=$safeFileName, taskId=${task.id}, sourceId=$sourceIdentifier")

        val destinationFile = resolveDestinationFile(
            destinationDirectory = destinationDirectory,
            cleanTitle = cleanTitle,
            extension = extension,
            sourceIdentifier = sourceIdentifier
        ) ?: run {
            taskDirectory.deleteRecursively()
            return@withContext fail(task.id, "Could not create destination file.")
        }

        try {
            applicationContext.contentResolver.openOutputStream(destinationFile.uri)?.use { output ->
                audioFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: run {
                if (destinationFile.length() == 0L) destinationFile.delete()
                taskDirectory.deleteRecursively()
                return@withContext fail(task.id, "Could not open destination file.")
            }
        } catch (e: Exception) {
            if (destinationFile.length() == 0L) destinationFile.delete()
            taskDirectory.deleteRecursively()
            return@withContext fail(task.id, e.message ?: "Failed copying to Music Storage")
        }

        // Clean up task temporary directory
        taskDirectory.deleteRecursively()

        // Synchronize Library
        try {
            MusicRepository(applicationContext).syncMusic()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Music sync failed after YouTube download", e)
        }

        downloadDao.deleteTaskById(task.id)
        Result.success()
    }

    /**
     * Resolves the destination file deterministically.
     * Prevents overwriting unrelated files and avoids silent SAF (1).mp3 creation.
     */
    private fun resolveDestinationFile(
        destinationDirectory: DocumentFile,
        cleanTitle: String,
        extension: String,
        sourceIdentifier: String
    ): DocumentFile? {
        val targetFileName = "$cleanTitle.$extension"
        val existing = destinationDirectory.findFile(targetFileName)

        if (existing == null) {
            return destinationDirectory.createFile(getMimeType(extension), targetFileName)
        }

        // Clean up 0-byte orphan from a prior interrupted copy
        if (existing.length() == 0L) {
            Log.w("DownloadWorker", "Found 0-byte orphan file: $targetFileName. Deleting orphan.")
            existing.delete()
            return destinationDirectory.createFile(getMimeType(extension), targetFileName)
        }

        // If file already exists with non-zero length, use deterministic collision-safe name
        // containing source identifier (e.g. video ID or task ID) to avoid overwriting and avoid SAF (1).mp3
        val collisionSafeName = "$cleanTitle [$sourceIdentifier].$extension"
        Log.i("DownloadWorker", "File $targetFileName already exists. Using deterministic collision-safe name: $collisionSafeName")

        val alternateExisting = destinationDirectory.findFile(collisionSafeName)
        if (alternateExisting != null && alternateExisting.length() == 0L) {
            alternateExisting.delete()
        }

        return destinationDirectory.createFile(getMimeType(extension), collisionSafeName)
    }

    private fun getExtensionFromUrl(url: String): String {
        val path = Uri.parse(url).path?.substringAfterLast(".", "")?.lowercase()
        return when (path) {
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus" -> path
            else -> "mp3"
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            else -> "audio/mpeg"
        }
    }

    private fun buildSafeFileName(
        title: String,
        extension: String
    ): String {
        val cleanTitle = title
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .ifBlank { "Downloaded Song" }
        return "$cleanTitle.$extension"
    }

    private suspend fun fail(
        taskId: Long,
        message: String
    ): Result {
        downloadDao.markFailed(
            id = taskId,
            status = DownloadTaskEntity.STATUS_FAILED,
            errorMessage = message
        )
        return Result.failure()
    }

    companion object {
        const val KEY_TASK_ID = "download_task_id"
    }
}