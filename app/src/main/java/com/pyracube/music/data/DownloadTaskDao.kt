package com.pyracube.music.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query(
        "SELECT * FROM download_tasks " +
                "ORDER BY createdAt ASC"
    )
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query(
        "SELECT * FROM download_tasks " +
                "WHERE status = :status " +
                "ORDER BY createdAt ASC"
    )
    fun observeTasksByStatus(
        status: String
    ): Flow<List<DownloadTaskEntity>>

    @Query(
        "SELECT * FROM download_tasks " +
                "WHERE id = :id LIMIT 1"
    )
    suspend fun getTask(
        id: Long
    ): DownloadTaskEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertTask(
        task: DownloadTaskEntity
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertTasks(
        tasks: List<DownloadTaskEntity>
    )

    @Query(
        "UPDATE download_tasks " +
                "SET status = :status " +
                "WHERE id = :id"
    )
    suspend fun updateStatus(
        id: Long,
        status: String
    )

    @Query(
        "UPDATE download_tasks " +
                "SET progress = :progress, status = :status " +
                "WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Long,
        progress: Int,
        status: String
    )

    @Query(
        "UPDATE download_tasks " +
                "SET status = :status, errorMessage = :errorMessage " +
                "WHERE id = :id"
    )
    suspend fun markFailed(
        id: Long,
        status: String,
        errorMessage: String?
    )

    @Query(
        "UPDATE download_tasks " +
                "SET progress = 0, errorMessage = NULL, status = :status " +
                "WHERE id = :id"
    )
    suspend fun resetTaskForRetry(
        id: Long,
        status: String = DownloadTaskEntity.STATUS_PENDING
    )

    @Query(
        "SELECT * FROM download_tasks"
    )
    suspend fun getAllTasks(): List<DownloadTaskEntity>

    @Query(
        "SELECT sourceUrl FROM download_tasks " +
                "WHERE status IN (:activeStatuses)"
    )
    suspend fun getActiveSourceUrls(
        activeStatuses: List<String>
    ): List<String>

    @Delete
    suspend fun deleteTask(
        task: DownloadTaskEntity
    )

    @Query(
        "DELETE FROM download_tasks " +
                "WHERE id = :id"
    )
    suspend fun deleteTaskById(
        id: Long
    )

    @Query(
        "DELETE FROM download_tasks"
    )
    suspend fun deleteAllTasks()

    @Query("""
    UPDATE download_tasks
    SET title = :title,
        artist = :artist,
        fileName = :fileName,
        errorMessage = NULL
    WHERE id = :id
""")
    suspend fun updateMetadata(
        id: Long,
        title: String,
        artist: String,
        fileName: String
    )

    @Query(
        "SELECT COUNT(*) FROM download_tasks " +
                "WHERE status = :status"
    )
    suspend fun countByStatus(
        status: String
    ): Int

    @Query(
        "SELECT * FROM download_tasks " +
                "WHERE status = :status " +
                "ORDER BY createdAt ASC " +
                "LIMIT 1"
    )
    suspend fun getOldestTaskByStatus(
        status: String
    ): DownloadTaskEntity?

    @Query(
        "UPDATE download_tasks " +
                "SET status = :toStatus " +
                "WHERE status = :fromStatus"
    )
    suspend fun resetStaleStatus(
        fromStatus: String,
        toStatus: String
    )
}