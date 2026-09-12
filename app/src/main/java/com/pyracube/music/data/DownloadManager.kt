package com.pyracube.music.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class DownloadManager(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val workManager =
        WorkManager.getInstance(appContext)

    /*
     * Start a download task.
     */
    fun startDownload(
        taskId: Long
    ) {

        val inputData =
            workDataOf(
                DownloadWorker.KEY_TASK_ID to taskId
            )

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

        /*
         * Each download gets its own unique work name.
         *
         * This prevents accidentally starting the
         * same queue item multiple times.
         */
        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /*
     * Cancel a running download.
     */
    fun cancelDownload(
        taskId: Long
    ) {

        workManager.cancelUniqueWork(
            "download_$taskId"
        )
    }

    /*
     * Retry a previously failed/cancelled download.
     * Uses ExistingWorkPolicy.REPLACE to guarantee a fresh WorkRequest is scheduled.
     */
    fun retryDownload(
        taskId: Long
    ) {
        val inputData =
            workDataOf(
                DownloadWorker.KEY_TASK_ID to taskId
            )

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}