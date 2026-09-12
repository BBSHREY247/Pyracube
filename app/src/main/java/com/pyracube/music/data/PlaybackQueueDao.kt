package com.pyracube.music.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackQueueDao {

    @Query("SELECT * FROM playback_queue WHERE id = :id LIMIT 1")
    fun observeQueue(id: Long): Flow<PlaybackQueueEntity?>

    @Query("SELECT * FROM playback_queue WHERE id = :id LIMIT 1")
    suspend fun getQueue(id: Long): PlaybackQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(queue: PlaybackQueueEntity)

    @Query("DELETE FROM playback_queue WHERE id = :id")
    suspend fun deleteQueue(id: Long)

    @Query("UPDATE playback_queue SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Long)

    @Query("UPDATE playback_queue SET currentIndex = :index WHERE id = :id")
    suspend fun updateCurrentIndex(id: Long, index: Int)

    @Query("UPDATE playback_queue SET shuffleEnabled = :enabled WHERE id = :id")
    suspend fun updateShuffleEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE playback_queue SET repeatMode = :mode WHERE id = :id")
    suspend fun updateRepeatMode(id: Long, mode: String)

    @Query("UPDATE playback_queue SET shuffleCompletedIds = :ids WHERE id = :id")
    suspend fun updateShuffleCompletedIds(id: Long, ids: String)
}
