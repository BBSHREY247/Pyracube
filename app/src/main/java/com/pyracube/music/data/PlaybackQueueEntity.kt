package com.pyracube.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_queue")
data class PlaybackQueueEntity(

    @PrimaryKey
    val id: Long = ACTIVE_QUEUE_ID,

    val songIds: String = "[]",

    val currentIndex: Int = 0,

    val shuffleEnabled: Boolean = false,

    val repeatMode: String = REPEAT_OFF,

    val shuffleCompletedIds: String = "[]",

    val position: Long = 0L,

    val contextType: String = CONTEXT_LIBRARY,

    val contextId: String = ""
) {

    companion object {

        const val ACTIVE_QUEUE_ID = 1L

        const val REPEAT_OFF = "OFF"
        const val REPEAT_ALL = "ALL"
        const val REPEAT_ONE = "ONE"

        const val CONTEXT_LIBRARY = "LIBRARY"
        const val CONTEXT_PLAYLIST = "PLAYLIST"
        const val CONTEXT_ALBUM = "ALBUM"
    }
}
