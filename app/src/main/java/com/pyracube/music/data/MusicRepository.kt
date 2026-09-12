package com.pyracube.music.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val database =
        AppDatabase.getInstance(appContext)

    private val songDao =
        database.songDao()

    private val storageManager =
        MusicStorageManager(appContext)

    private val scanner =
        MusicScanner(
            context = appContext,
            contentResolver = appContext.contentResolver,
            storageManager = storageManager
        )

    /*
     * Observe all songs currently stored in
     * Pyracube's Room database.
     */
    fun observeAllSongs(): Flow<List<SongEntity>> =
        songDao.observeAllSongs()

    /*
     * Observe songs sorted by date added (newest first).
     */
    fun observeAllSongsByDateAddedDesc(): Flow<List<SongEntity>> =
        songDao.observeAllSongsByDateAddedDesc()

    /*
     * Observe songs sorted by date added (oldest first).
     */
    fun observeAllSongsByDateAddedAsc(): Flow<List<SongEntity>> =
        songDao.observeAllSongsByDateAddedAsc()

    /*
     * Observe songs sorted by artist.
     */
    fun observeAllSongsByArtist(): Flow<List<SongEntity>> =
        songDao.observeAllSongsByArtist()

    /*
     * Get the current number of songs.
     */
    suspend fun getSongCount(): Int =
        withContext(Dispatchers.IO) {
            songDao.getSongCount()
        }

    /*
     * Reconcile Room database with actual music files in storage:
     * - file exists + DB exists -> keep (preserves dateAdded and metadata)
     * - file exists + DB missing -> insert as new record with current dateAdded
     * - file missing + DB exists -> delete from DB (and clean up cached artwork)
     * - file missing + DB missing -> nothing
     *
     * Never calls deleteAllSongs(). Never deletes actual user audio files.
     */
    suspend fun syncMusic() {
        withContext(Dispatchers.IO) {
            val scannedSongs = scanner.scan()

            val existingSongs = songDao.getAllSongs()
            val existingByUri = existingSongs.associateBy { it.uri }
            val existingById = existingSongs.associateBy { it.id }

            val scannedByUri = scannedSongs.associateBy { it.uri }
            val scannedById = scannedSongs.associateBy { it.id }

            // 1. Files missing from storage + DB exists -> remove DB entry
            val songsToRemove = existingSongs.filter { existing ->
                existing.uri !in scannedByUri && existing.id !in scannedById
            }

            for (song in songsToRemove) {
                songDao.deleteSong(song)
                song.artworkUri?.let { path ->
                    try {
                        val artFile = java.io.File(path)
                        if (artFile.exists()) {
                            artFile.delete()
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            // 2. Files exists in storage + DB missing -> insert
            // 3. Files exists + DB exists -> update dateAdded if scanner has a better value
            val songsToInsert = mutableListOf<SongEntity>()
            for (scanned in scannedSongs) {
                val existing = existingByUri[scanned.uri] ?: existingById[scanned.id]
                if (existing == null) {
                    songsToInsert.add(
                        scanned.copy(dateAdded = System.currentTimeMillis()).toEntity()
                    )
                } else if (scanned.dateAdded != existing.dateAdded &&
                    scanned.dateAdded < System.currentTimeMillis()
                ) {
                    // Scanner got a real filesystem date — update if different
                    songDao.updateDateAdded(existing.id, scanned.dateAdded)
                }
            }

            if (songsToInsert.isNotEmpty()) {
                songDao.insertSongs(songsToInsert)
            }
        }
    }
}