package com.pyracube.music.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
private val sourceDetector = MediaSourceDetector()

class DownloadRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val downloadDao = database.downloadTaskDao()
    private val songDao = database.songDao()
    private val downloadManager = DownloadManager(appContext)
    private val queueController = DownloadQueueController.getInstance(appContext)
    private val sourceDetector = MediaSourceDetector()
    private val playlistExtractor = PlaylistExtractor()
    private val playlistDetector = PlaylistDetector()
    private val metadataExtractor = BasicMetadataExtractor()
    fun observeDownloads():
            Flow<List<DownloadTaskEntity>> {

        return downloadDao.observeAllTasks()
    }

    /*
     * Add a new task to the queue.
     * The task is inserted as PENDING.
     * The queue controller will start it when a slot is available.
     */
    suspend fun addDownload(
        sourceUrl: String,
        title: String,
        artist: String = "Unknown Artist",
        thumbnailUrl: String? = null,
        fileName: String
    ): Long {
        return withContext(Dispatchers.IO) {

            val canonicalSourceUrl = playlistDetector.canonicalVideoUrl(sourceUrl)
            val videoId = playlistDetector.extractVideoId(canonicalSourceUrl)

            // Check if a task for this video already exists
            val existingTasks = downloadDao.getAllTasks()
            val existingTask = existingTasks.firstOrNull { task ->
                task.sourceUrl == canonicalSourceUrl ||
                        (videoId != null && playlistDetector.extractVideoId(task.sourceUrl) == videoId)
            }

            if (existingTask != null) {
                android.util.Log.i("DownloadRepository", "Reusing existing task id=${existingTask.id} for $canonicalSourceUrl")
                return@withContext existingTask.id
            }

            val safeFileName = fileName
                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                    .trim()
                    .ifBlank { "download" }

            val sourceType =
                sourceDetector
                    .detect(canonicalSourceUrl)
                    .name

            val metadata =
                if (title == "Pending metadata") {
                    try {
                        metadataExtractor.extract(canonicalSourceUrl)
                    } catch (_: Exception) {
                        MediaMetadata(
                            title = "Unknown Title"
                        )
                    }
                } else {
                    MediaMetadata(
                        title = title,
                        artist = artist,
                        thumbnailUrl = thumbnailUrl
                    )
                }

            val taskId = downloadDao.insertTask(
                DownloadTaskEntity(
                    sourceUrl = canonicalSourceUrl,
                    sourceType = sourceType,
                    title = metadata.title,
                    artist = metadata.artist,
                    thumbnailUrl = metadata.thumbnailUrl,
                    fileName = safeFileName
                )
            )

            // Let the queue controller decide when to start
            queueController.scheduleNext()

            taskId
        }
    }

    /*
     * Add a task and let the queue controller start it
     * when a slot is available.
     */
    suspend fun addAndStartDownload(
        sourceUrl: String,
        title: String,
        artist: String = "Unknown Artist",
        thumbnailUrl: String? = null,
        fileName: String
    ): Long {

        val taskId =
            addDownload(
                sourceUrl = sourceUrl,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                fileName = fileName
            )

        return taskId
    }

    /*
     * Start an existing queued task.
     * Bypasses the queue controller for explicit user actions.
     */
    fun startDownload(
        id: Long
    ) {

        downloadManager.startDownload(
            id
        )
    }

    /*
     * Update download progress.
     */
    suspend fun updateProgress(
        id: Long,
        progress: Int
    ) {

        withContext(Dispatchers.IO) {

            downloadDao.updateProgress(
                id = id,
                progress =
                    progress.coerceIn(0, 100),
                status =
                    DownloadTaskEntity
                        .STATUS_DOWNLOADING
            )
        }
    }

    /*
     * Mark a task completed.
     */
    suspend fun markCompleted(
        id: Long
    ) {

        withContext(Dispatchers.IO) {

            downloadDao.updateProgress(
                id = id,
                progress = 100,
                status =
                    DownloadTaskEntity
                        .STATUS_COMPLETED
            )
        }
    }

    /*
     * Mark a task failed.
     */
    suspend fun markFailed(
        id: Long,
        message: String
    ) {

        withContext(Dispatchers.IO) {

            downloadDao.markFailed(
                id = id,
                status =
                    DownloadTaskEntity
                        .STATUS_FAILED,
                errorMessage = message
            )
        }
    }

    /*
     * Cancel a download.
     * Stops the WorkManager work, marks CANCELLED,
     * and triggers the queue to start the next task.
     */
    suspend fun cancel(
        id: Long
    ) {
        withContext(Dispatchers.IO) {
            downloadManager.cancelDownload(id)
            downloadDao.updateStatus(id, DownloadTaskEntity.STATUS_CANCELLED)
            queueController.onTaskCancelled(id)
        }
    }

    /*
     * Retry a download.
     * Resets the task to PENDING and lets the queue controller
     * start it when a slot is available.
     * Uses REPLACE to guarantee a fresh WorkRequest for explicit user retry.
     */
    suspend fun retry(
        id: Long
    ) {
        withContext(Dispatchers.IO) {
            downloadDao.resetTaskForRetry(
                id = id,
                status = DownloadTaskEntity.STATUS_PENDING
            )
            downloadManager.retryDownload(id)
            queueController.onTaskStarted(id)
            queueController.scheduleNext()
        }
    }

    /*
     * Delete a task from the queue.
     */
    suspend fun delete(
        id: Long
    ) {
        withContext(Dispatchers.IO) {
            downloadManager.cancelDownload(id)
            downloadDao.deleteTaskById(id)
            queueController.onTaskCancelled(id)
        }
    }

    /*
     * Check if a URL is a YouTube playlist.
     */
    fun isPlaylist(url: String): Boolean {
        return playlistDetector.isYouTubePlaylist(url)
    }

    /*
     * Extract a YouTube playlist and enqueue each video as an individual download task.
     * All tasks are inserted as PENDING. The queue controller starts them
     * one at a time according to MAX_CONCURRENT_DOWNLOADS.
     */
    suspend fun addPlaylistToQueue(
        playlistUrl: String
    ): PlaylistResult = withContext(Dispatchers.IO) {
        try {
            val entries = playlistExtractor.extract(playlistUrl)
            if (entries.isEmpty()) {
                return@withContext PlaylistResult.Error("No videos found in playlist")
            }

            // Fetch all current tasks to check against existing active/failed/cancelled tasks
            val existingTasks = downloadDao.getAllTasks()
            val existingVideoIds = mutableSetOf<String>()
            val existingSourceUrls = mutableSetOf<String>()

            for (task in existingTasks) {
                existingSourceUrls.add(task.sourceUrl)
                val vId = playlistDetector.extractVideoId(task.sourceUrl)
                if (vId != null) {
                    existingVideoIds.add(vId)
                }
            }

            val seenVideoIdsInBatch = mutableSetOf<String>()
            val seenUrlsInBatch = mutableSetOf<String>()

            var tasksCreated = 0
            var duplicatesSkipped = 0
            var unavailableSkipped = 0

            for (entry in entries) {
                val trimmedUrl = entry.videoUrl.trim()
                val trimmedTitle = entry.title.trim()

                if (trimmedUrl.isBlank() ||
                    trimmedTitle.equals("[Deleted video]", ignoreCase = true) ||
                    trimmedTitle.equals("[Private video]", ignoreCase = true)
                ) {
                    unavailableSkipped++
                    continue
                }

                val videoId = entry.videoId ?: playlistDetector.extractVideoId(trimmedUrl)
                val canonicalUrl = if (videoId != null) {
                    "https://www.youtube.com/watch?v=$videoId"
                } else {
                    trimmedUrl
                }

                // Deduplicate within the playlist itself
                if (videoId != null && seenVideoIdsInBatch.contains(videoId)) {
                    android.util.Log.d("DownloadRepository", "Duplicate video ID in playlist skipped: $videoId")
                    duplicatesSkipped++
                    continue
                }
                if (seenUrlsInBatch.contains(canonicalUrl)) {
                    android.util.Log.d("DownloadRepository", "Duplicate URL in playlist skipped: $canonicalUrl")
                    duplicatesSkipped++
                    continue
                }

                // Deduplicate against existing non-completed tasks in Room
                if (videoId != null && existingVideoIds.contains(videoId)) {
                    android.util.Log.d("DownloadRepository", "Existing task found for video ID $videoId; keeping existing task.")
                    duplicatesSkipped++
                    continue
                }
                if (existingSourceUrls.contains(canonicalUrl) || existingSourceUrls.contains(trimmedUrl)) {
                    android.util.Log.d("DownloadRepository", "Existing task found for URL $canonicalUrl; keeping existing task.")
                    duplicatesSkipped++
                    continue
                }

                val safeFileName = "${trimmedTitle}.mp3"
                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                    .trim()
                    .ifBlank { "download_${System.currentTimeMillis()}" }

                val sourceType = sourceDetector.detect(canonicalUrl).name

                // Insert as PENDING — do NOT start WorkManager yet.
                // The queue controller will start it when a slot is available.
                downloadDao.insertTask(
                    DownloadTaskEntity(
                        sourceUrl = canonicalUrl,
                        sourceType = sourceType,
                        title = trimmedTitle,
                        artist = entry.artist.trim().ifBlank { "Unknown Artist" },
                        thumbnailUrl = null,
                        fileName = safeFileName
                    )
                )

                tasksCreated++

                if (videoId != null) {
                    seenVideoIdsInBatch.add(videoId)
                }
                seenUrlsInBatch.add(canonicalUrl)
            }

            // Now that all tasks are in Room, let the queue controller start the first one
            queueController.scheduleNext()

            PlaylistResult.Success(
                tasksCreated = tasksCreated,
                duplicatesSkipped = duplicatesSkipped,
                unavailableSkipped = unavailableSkipped
            )
        } catch (e: Exception) {
            PlaylistResult.Error(e.message ?: "Failed to process playlist")
        }
    }
}
