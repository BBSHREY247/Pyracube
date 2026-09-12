package com.pyracube.music.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Controls download concurrency.
 *
 * Ensures only MAX_CONCURRENT_DOWNLOADS tasks run at a time.
 * All other eligible tasks remain PENDING in Room.
 *
 * Safe to call scheduleNext() from multiple places:
 * - After a task completes/fails/cancels
 * - After new tasks are added
 * - On app startup (to recover stale tasks)
 *
 * Uses Mutex to prevent duplicate WorkRequest launches.
 */
class DownloadQueueController private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val downloadDao = database.downloadTaskDao()
    private val downloadManager = DownloadManager(appContext)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    /**
     * Tracks task IDs that have an active WorkManager request.
     * Prevents launching the same task twice.
     */
    private val activeWorkIds = mutableSetOf<Long>()

    /**
     * Initialize the controller on app startup.
     * Resets any stale DOWNLOADING tasks and starts the queue.
     */
    fun initialize() {
        scope.launch {
            try {
                // Reset any tasks that were DOWNLOADING when the app was killed.
                // They need to be restarted cleanly.
                downloadDao.resetStaleStatus(
                    fromStatus = DownloadTaskEntity.STATUS_DOWNLOADING,
                    toStatus = DownloadTaskEntity.STATUS_PENDING
                )
                scheduleNext()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize queue controller", e)
            }
        }
    }

    /**
     * Find the next eligible PENDING task and start it
     * if we are below the concurrency limit.
     *
     * Safe to call multiple times concurrently — the Mutex
     * ensures only one scheduling pass runs at a time.
     */
    suspend fun scheduleNext() {
        mutex.withLock {
            try {
                val activeCount = downloadDao.countByStatus(
                    DownloadTaskEntity.STATUS_DOWNLOADING
                )

                Log.d(TAG, "scheduleNext: active=$activeCount limit=$MAX_CONCURRENT_DOWNLOADS")

                if (activeCount >= MAX_CONCURRENT_DOWNLOADS) {
                    return@withLock
                }

                val slotsAvailable = MAX_CONCURRENT_DOWNLOADS - activeCount

                for (i in 0 until slotsAvailable) {
                    val nextTask = downloadDao.getOldestTaskByStatus(
                        DownloadTaskEntity.STATUS_PENDING
                    )

                    if (nextTask == null) {
                        Log.d(TAG, "No pending tasks remaining")
                        return@withLock
                    }

                    // Avoid launching the same task twice
                    if (activeWorkIds.contains(nextTask.id)) {
                        Log.d(TAG, "Task ${nextTask.id} already has active work, skipping")
                        continue
                    }

                    Log.i(TAG, "Starting task ${nextTask.id}: ${nextTask.title}")

                    activeWorkIds.add(nextTask.id)
                    downloadManager.startDownload(nextTask.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in scheduleNext", e)
            }
        }
    }

    /**
     * Called when a task's WorkManager work completes (success, failure, or cancellation).
     * Removes the task from active tracking and schedules the next one.
     */
    fun onTaskFinished(taskId: Long) {
        activeWorkIds.remove(taskId)
        scope.launch {
            scheduleNext()
        }
    }

    /**
     * Remove a task from active tracking (e.g., when user cancels).
     */
    fun onTaskCancelled(taskId: Long) {
        activeWorkIds.remove(taskId)
        scope.launch {
            scheduleNext()
        }
    }

    /**
     * Mark that a task is now active (used when retry uses REPLACE).
     */
    fun onTaskStarted(taskId: Long) {
        activeWorkIds.add(taskId)
    }

    companion object {
        private const val TAG = "DownloadQueueCtrl"
        private const val MAX_CONCURRENT_DOWNLOADS = 1

        @Volatile
        private var INSTANCE: DownloadQueueController? = null

        fun getInstance(context: Context): DownloadQueueController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadQueueController(context).also {
                    INSTANCE = it
                }
            }
        }
    }
}
