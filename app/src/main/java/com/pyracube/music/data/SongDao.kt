package com.pyracube.music.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun observeAllSongsByDateAddedDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded ASC")
    fun observeAllSongsByDateAddedAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE ASC")
    fun observeAllSongsByArtist(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("UPDATE songs SET dateAdded = :dateAdded WHERE id = :id")
    suspend fun updateDateAdded(id: Long, dateAdded: Long)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
}