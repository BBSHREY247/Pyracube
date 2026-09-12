package com.pyracube.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pyracube.music.data.PlaybackQueueEntity
import com.pyracube.music.data.PlaybackQueueRepository
import com.pyracube.music.data.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private val sessionToken = SessionToken(
        appContext,
        ComponentName(
            appContext,
            MusicPlaybackService::class.java
        )
    )

    val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            appContext,
            sessionToken
        ).buildAsync()

    private val _playerState =
        MutableStateFlow(PlayerState())

    val playerState: StateFlow<PlayerState> =
        _playerState.asStateFlow()

    private var controller: MediaController? = null

    private val scope =
        CoroutineScope(Dispatchers.Main)

    private var positionJob: Job? = null

    private val queueRepository =
        PlaybackQueueRepository(appContext)

    private var songDao = com.pyracube.music.data.AppDatabase
        .getInstance(appContext).songDao()

    // Track whether the current transition was user-initiated
    private var userInitiatedTransition = false

    // In-memory cache of current queue song IDs for quick access
    private var currentQueueSongIds = listOf<Long>()

    // Position save job
    private var positionSaveJob: Job? = null

    fun setController(
        mediaController: MediaController
    ) {
        controller = mediaController

        mediaController.addListener(
            object : Player.Listener {

                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {
                    updateState()
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int
                ) {
                    handleMediaItemTransition(reason)
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {
                    handlePlaybackStateChange(playbackState)
                    updateState()
                }
            }
        )

        updateState()
        startPositionUpdates()
        startPositionSaveJob()
        restoreState()
    }

    /**
     * Play a song from the Library with the given queue order.
     */
    fun playSong(
        controller: MediaController,
        songs: List<SongEntity>,
        selectedSong: SongEntity
    ) {
        val mediaItems = songs.map { song ->
            buildMediaItem(song)
        }

        val selectedIndex = songs.indexOfFirst {
            it.id == selectedSong.id
        }

        if (selectedIndex == -1) return

        currentQueueSongIds = songs.map { it.id }

        // Save queue to Room
        scope.launch {
            queueRepository.startQueue(
                songIds = currentQueueSongIds,
                startIndex = selectedIndex,
                contextType = PlaybackQueueEntity.CONTEXT_LIBRARY,
                contextId = "all_songs"
            )
        }

        controller.setMediaItems(
            mediaItems,
            selectedIndex,
            0L
        )

        controller.prepare()
        controller.play()
    }

    fun playPause() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
                scope.launch {
                    queueRepository.savePosition(it.currentPosition)
                }
            } else {
                it.play()
            }
        }
    }

    fun next() {
        userInitiatedTransition = true
        val ctrl = controller ?: return
        ctrl.seekToNextMediaItem()
        ctrl.play()
    }

    fun previous() {
        userInitiatedTransition = true
        val ctrl = controller ?: return
        ctrl.seekToPreviousMediaItem()
        ctrl.play()
    }

    fun pause() {
        controller?.pause()
        scope.launch {
            controller?.let {
                queueRepository.savePosition(it.currentPosition)
            }
        }
    }

    fun resume() {
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    // TODO: Shuffle is disabled pending fix
    fun toggleShuffle() {
        // Shuffle functionality temporarily disabled
    }

    fun cycleRepeatMode() {
        scope.launch {
            queueRepository.cycleRepeatMode()
            updateState()
        }
    }

    private fun handleMediaItemTransition(reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
            return // Initial setMediaItems, not a real transition
        }

        val ctrl = controller ?: return

        scope.launch {
            val queue = queueRepository.getQueue() ?: return@launch
            val allIds = queue.songIds.fromJsonArray()
            val currentId = getCurrentSongId(ctrl) ?: return@launch
            val newIndex = allIds.indexOf(currentId)

            if (newIndex >= 0) {
                queueRepository.getQueue()?.let { q ->
                    com.pyracube.music.data.AppDatabase
                        .getInstance(appContext)
                        .playbackQueueDao()
                        .updateCurrentIndex(
                            PlaybackQueueEntity.ACTIVE_QUEUE_ID,
                            newIndex
                        )
                }
            }

            // Update shuffle history for non-user transitions
            if (!userInitiatedTransition && queue.shuffleEnabled) {
                // Natural transition: add previous song to history
            }

            userInitiatedTransition = false
            updateState()
        }
    }

    private fun handlePlaybackStateChange(playbackState: Int) {
        if (playbackState != Player.STATE_ENDED) return

        val ctrl = controller ?: return

        scope.launch {
            val queue = queueRepository.getQueue() ?: return@launch
            val allIds = queue.songIds.fromJsonArray()
            if (allIds.isEmpty()) return@launch

            when (queue.repeatMode) {
                PlaybackQueueEntity.REPEAT_ONE -> {
                    ctrl.seekTo(0L)
                    ctrl.play()
                }
                PlaybackQueueEntity.REPEAT_ALL -> {
                    ctrl.seekTo(0L)
                    ctrl.play()
                }
                else -> {
                    val nextIndex = queue.currentIndex + 1
                    if (nextIndex >= allIds.size) {
                        Log.i("PlayerManager", "Queue finished")
                        queueRepository.savePosition(0L)
                    }
                }
            }
        }
    }

    private fun updateState() {
        val mediaController = controller ?: return
        val metadata = mediaController.mediaMetadata

        scope.launch {
            val queue = queueRepository.getQueue()
            _playerState.value = PlayerState(
                title = metadata.title?.toString() ?: "Nothing Playing",
                artist = metadata.artist?.toString() ?: "Choose a song to start listening",
                artworkUri = metadata.artworkUri?.toString(),
                isPlaying = mediaController.isPlaying,
                position = mediaController.currentPosition,
                duration = if (mediaController.duration > 0) mediaController.duration else 0L,
                shuffleEnabled = false, // TODO: temporarily disabled
                repeatMode = queue?.repeatMode ?: PlaybackQueueEntity.REPEAT_OFF,
                hasQueue = queue != null
            )
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    private fun startPositionSaveJob() {
        positionSaveJob?.cancel()
        positionSaveJob = scope.launch {
            while (isActive) {
                delay(5000)
                controller?.let { ctrl ->
                    if (ctrl.isPlaying) {
                        queueRepository.updatePosition(ctrl.currentPosition)
                    }
                }
            }
        }
    }

    private fun restoreState() {
        scope.launch {
            val queue = queueRepository.getQueue() ?: return@launch
            val allIds = queue.songIds.fromJsonArray()
            if (allIds.isEmpty()) return@launch

            currentQueueSongIds = allIds

            // Load songs from Room
            val songs = mutableListOf<SongEntity>()
            for (id in allIds) {
                val song = songDao.getSongById(id)
                if (song != null) songs.add(song)
            }

            if (songs.isEmpty()) return@launch

            val mediaItems = songs.map { buildMediaItem(it) }
            val restoreIndex = queue.currentIndex.coerceIn(0, songs.lastIndex)
            val restorePosition = queue.position.coerceAtLeast(0L)

            val ctrl = controller ?: return@launch
            ctrl.setMediaItems(mediaItems, restoreIndex, restorePosition)
            ctrl.prepare()

            updateState()
        }
    }

    private fun getCurrentSongId(ctrl: MediaController): Long? {
        val mediaId = ctrl.currentMediaItem?.mediaId ?: return null
        return mediaId.toLongOrNull()
    }

    private fun buildMediaItem(song: SongEntity): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)

        if (!song.artworkUri.isNullOrBlank()) {
            val artworkFile = File(song.artworkUri)
            if (artworkFile.exists()) {
                metadataBuilder.setArtworkUri(Uri.fromFile(artworkFile))
            }
        }

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun String.fromJsonArray(): List<Long> {
        return try {
            val array = org.json.JSONArray(this)
            (0 until array.length()).map { array.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
