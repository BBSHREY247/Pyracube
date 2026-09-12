package com.pyracube.music.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random

class PlaybackQueueRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val queueDao = database.playbackQueueDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observeQueue(): Flow<PlaybackQueueEntity?> {
        return queueDao.observeQueue(PlaybackQueueEntity.ACTIVE_QUEUE_ID)
    }

    suspend fun getQueue(): PlaybackQueueEntity? {
        return queueDao.getQueue(PlaybackQueueEntity.ACTIVE_QUEUE_ID)
    }

    /**
     * Start a new queue from the given songs.
     * This replaces any existing active queue.
     */
    suspend fun startQueue(
        songIds: List<Long>,
        startIndex: Int,
        contextType: String = PlaybackQueueEntity.CONTEXT_LIBRARY,
        contextId: String = ""
    ) = withContext(Dispatchers.IO) {
        val queue = PlaybackQueueEntity(
            id = PlaybackQueueEntity.ACTIVE_QUEUE_ID,
            songIds = songIds.toJsonArray(),
            currentIndex = startIndex,
            shuffleEnabled = false,
            repeatMode = PlaybackQueueEntity.REPEAT_OFF,
            shuffleCompletedIds = "[]",
            position = 0L,
            contextType = contextType,
            contextId = contextId
        )
        queueDao.saveQueue(queue)
    }

    /**
     * Get the ordered song IDs from the queue.
     */
    suspend fun getSongIds(): List<Long> {
        val queue = getQueue() ?: return emptyList()
        return queue.songIds.fromJsonArray()
    }

    /**
     * Get the current song ID.
     */
    suspend fun getCurrentSongId(): Long? {
        val queue = getQueue() ?: return null
        val ids = queue.songIds.fromJsonArray()
        if (queue.currentIndex !in ids.indices) return null
        return ids[queue.currentIndex]
    }

    /**
     * Mark a song as completed in the shuffle cycle.
     * This removes it from the remaining pool.
     */
    suspend fun markShuffleCompleted(songId: Long) = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext
        val completed = queue.shuffleCompletedIds.fromJsonArray().toMutableList()
        if (!completed.contains(songId)) {
            completed.add(songId)
            queueDao.saveQueue(
                queue.copy(
                    shuffleCompletedIds = completed.toJsonArray()
                )
            )
        }
    }

    /**
     * Get the next song ID for shuffle mode.
     * Implements the shuffle-bag algorithm:
     * - Pick a random song from remaining (not completed) songs
     * - If pool is empty, start a new cycle
     * - Returns null if queue is empty
     */
    suspend fun getNextShuffleSongId(): Long? = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext null
        val allIds = queue.songIds.fromJsonArray()
        if (allIds.isEmpty()) return@withContext null

        val completedIds = queue.shuffleCompletedIds.fromJsonArray().toSet()

        // Filter to remaining songs
        val remaining = allIds.filter { it !in completedIds }

        if (remaining.isEmpty()) {
            // Pool exhausted — start new cycle
            Log.i("PlaybackQueueRepo", "Shuffle pool exhausted, starting new cycle")
            queueDao.saveQueue(
                queue.copy(shuffleCompletedIds = "[]")
            )
            // All songs are now eligible again
            val nextIndex = Random.nextInt(allIds.size)
            queueDao.saveQueue(
                queue.copy(currentIndex = nextIndex)
            )
            return@withContext allIds[nextIndex]
        }

        // Pick random from remaining
        val nextId = remaining[Random.nextInt(remaining.size)]
        val nextIndex = allIds.indexOf(nextId)
        queueDao.saveQueue(
            queue.copy(currentIndex = nextIndex)
        )
        nextId
    }

    /**
     * Get the previous song ID (for history navigation).
     * In shuffle mode, returns the previously completed song.
     * In normal mode, returns the previous index.
     */
    suspend fun getPreviousSongId(): Long? = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext null
        val allIds = queue.songIds.fromJsonArray()
        if (allIds.isEmpty()) return@withContext null

        if (queue.shuffleEnabled) {
            val completed = queue.shuffleCompletedIds.fromJsonArray()
            if (completed.isEmpty()) return@withContext null

            // Previous is the last completed song
            val previousId = completed.last()
            val previousIndex = allIds.indexOf(previousId)

            // Remove from completed so it becomes eligible again
            val newCompleted = completed.dropLast(1)
            queueDao.saveQueue(
                queue.copy(
                    currentIndex = previousIndex,
                    shuffleCompletedIds = newCompleted.toJsonArray()
                )
            )
            return@withContext previousId
        }

        // Normal mode: just go to previous index
        val prevIndex = (queue.currentIndex - 1).coerceAtLeast(0)
        queueDao.saveQueue(queue.copy(currentIndex = prevIndex))
        allIds[prevIndex]
    }

    /**
     * Advance to the next song in the queue.
     * Returns the next song ID or null if queue is done.
     */
    suspend fun advanceToNext(): Long? = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext null
        val allIds = queue.songIds.fromJsonArray()
        if (allIds.isEmpty()) return@withContext null

        if (queue.shuffleEnabled) {
            return@withContext getNextShuffleSongId()
        }

        // Normal mode
        val nextIndex = queue.currentIndex + 1

        when {
            nextIndex < allIds.size -> {
                queueDao.saveQueue(queue.copy(currentIndex = nextIndex))
                allIds[nextIndex]
            }
            queue.repeatMode == PlaybackQueueEntity.REPEAT_ALL -> {
                queueDao.saveQueue(queue.copy(currentIndex = 0))
                allIds[0]
            }
            else -> null // Queue finished
        }
    }

    /**
     * Go back to the previous song.
     * Returns the previous song ID.
     */
    suspend fun goToPrevious(): Long? = withContext(Dispatchers.IO) {
        getPreviousSongId()
    }

    /**
     * Toggle shuffle mode.
     */
    suspend fun toggleShuffle() = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext
        val newShuffleState = !queue.shuffleEnabled
        queueDao.saveQueue(
            queue.copy(
                shuffleEnabled = newShuffleState,
                shuffleCompletedIds = if (!newShuffleState) "[]" else queue.shuffleCompletedIds
            )
        )
    }

    /**
     * Cycle repeat mode: OFF → ALL → ONE → OFF
     */
    suspend fun cycleRepeatMode() = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext
        val newMode = when (queue.repeatMode) {
            PlaybackQueueEntity.REPEAT_OFF -> PlaybackQueueEntity.REPEAT_ALL
            PlaybackQueueEntity.REPEAT_ALL -> PlaybackQueueEntity.REPEAT_ONE
            else -> PlaybackQueueEntity.REPEAT_OFF
        }
        queueDao.saveQueue(queue.copy(repeatMode = newMode))
    }

    /**
     * Update the current playback position.
     * Throttled to avoid excessive writes.
     */
    private var lastPositionSave = 0L

    suspend fun updatePosition(position: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastPositionSave < 3000) return@withContext
        lastPositionSave = now
        queueDao.updatePosition(PlaybackQueueEntity.ACTIVE_QUEUE_ID, position)
    }

    /**
     * Force-save position (for pause/stop events).
     */
    suspend fun savePosition(position: Long) = withContext(Dispatchers.IO) {
        lastPositionSave = System.currentTimeMillis()
        queueDao.updatePosition(PlaybackQueueEntity.ACTIVE_QUEUE_ID, position)
    }

    /**
     * Clear the active queue.
     */
    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        queueDao.deleteQueue(PlaybackQueueEntity.ACTIVE_QUEUE_ID)
    }

    /**
     * Check if a song is in the remaining shuffle pool.
     */
    suspend fun isSongInShufflePool(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext false
        val allIds = queue.songIds.fromJsonArray()
        val completed = queue.shuffleCompletedIds.fromJsonArray().toSet()
        songId in allIds && songId !in completed
    }

    /**
     * Remove a song from the queue (e.g., if deleted from library).
     */
    suspend fun removeSongFromQueue(songId: Long) = withContext(Dispatchers.IO) {
        val queue = getQueue() ?: return@withContext
        val ids = queue.songIds.fromJsonArray().toMutableList()
        val removedIndex = ids.indexOf(songId)
        if (removedIndex == -1) return@withContext

        ids.remove(songId)

        val completed = queue.shuffleCompletedIds.fromJsonArray().toMutableList()
        completed.remove(songId)

        val newIndex = when {
            queue.currentIndex > removedIndex -> queue.currentIndex - 1
            queue.currentIndex == removedIndex && queue.currentIndex >= ids.size -> (ids.size - 1).coerceAtLeast(0)
            else -> queue.currentIndex
        }

        queueDao.saveQueue(
            queue.copy(
                songIds = ids.toJsonArray(),
                currentIndex = newIndex,
                shuffleCompletedIds = completed.toJsonArray()
            )
        )
    }

    // --- JSON helpers ---

    private fun List<Long>.toJsonArray(): String {
        val array = JSONArray()
        forEach { array.put(it) }
        return array.toString()
    }

    private fun String.fromJsonArray(): List<Long> {
        return try {
            val array = JSONArray(this)
            (0 until array.length()).map { array.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
