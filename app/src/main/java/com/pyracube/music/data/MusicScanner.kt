package com.pyracube.music.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

class MusicScanner(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val storageManager: MusicStorageManager
) {

    fun scan(): List<MusicSong> {

        val selectedFolder =
            storageManager.getMusicFolder()

        /*
         * If the user selected a Pyracube music folder,
         * that folder becomes the primary and only
         * music source.
         */
        if (selectedFolder != null) {

            return scanSafFolder(
                selectedFolder
            )
                .distinctBy { it.uri }
                .sortedBy {
                    it.title.lowercase()
                }
        }

        /*
         * If the user has not selected a folder yet,
         * temporarily use Android MediaStore.
         */
        return scanMediaStore()
            .distinctBy { it.uri }
            .sortedBy {
                it.title.lowercase()
            }
    }

    /*
     * Scan music indexed by Android MediaStore.
     *
     * This is only used when the user has not
     * selected a Pyracube music folder.
     */
    private fun scanMediaStore(): List<MusicSong> {

        val songs =
            mutableListOf<MusicSong>()

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
            )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder =
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

            val titleColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

            val artistColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

            val albumColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                )

            val durationColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                )

            val dateAddedColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DATE_ADDED
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(idColumn)

                val title =
                    cursor.getString(titleColumn)
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown Title"

                val artist =
                    cursor.getString(artistColumn)
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown Artist"

                val album =
                    cursor.getString(albumColumn)
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown Album"

                val duration =
                    cursor.getLong(durationColumn)

                val dateAdded =
                    cursor.getLong(dateAddedColumn) * 1000L // MediaStore stores seconds, convert to ms

                val uri =
                    Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    ).toString()

                val artworkUri =
                    extractArtwork(
                        id = id,
                        audioUri = uri
                    )

                songs.add(
                    MusicSong(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        duration = duration,
                        artworkUri = artworkUri,
                        dateAdded = if (dateAdded > 0) dateAdded else System.currentTimeMillis()
                    )
                )
            }
        }

        return songs
    }

    /*
     * Scan the folder selected through Android SAF.
     *
     * This recursively scans subfolders as well.
     */
    private fun scanSafFolder(
        treeUri: Uri
    ): List<MusicSong> {

        val songs =
            mutableListOf<MusicSong>()

        val root =
            DocumentFile.fromTreeUri(
                context,
                treeUri
            )
                ?: return songs

        scanDocumentDirectory(
            directory = root,
            songs = songs
        )

        return songs
    }

    private fun scanDocumentDirectory(
        directory: DocumentFile,
        songs: MutableList<MusicSong>
    ) {

        val files =
            try {
                directory.listFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }

        for (file in files) {

            if (file.isDirectory) {

                scanDocumentDirectory(
                    directory = file,
                    songs = songs
                )

                continue
            }

            if (!file.isFile) {
                continue
            }

            if (!isSupportedAudioFile(file)) {
                continue
            }

            val song =
                readSafSong(file)

            if (song != null) {
                songs.add(song)
            }
        }
    }

    /*
     * Audio formats supported by Pyracube.
     */
    private fun isSupportedAudioFile(
        file: DocumentFile
    ): Boolean {

        val name =
            file.name
                ?.lowercase()
                ?: return false

        return name.endsWith(".mp3") ||
                name.endsWith(".m4a") ||
                name.endsWith(".aac") ||
                name.endsWith(".flac") ||
                name.endsWith(".wav") ||
                name.endsWith(".ogg") ||
                name.endsWith(".opus")
    }

    /*
     * Read metadata from an SAF audio file.
     */
    private fun readSafSong(
        file: DocumentFile
    ): MusicSong? {

        return try {

            val uri =
                file.uri.toString()

            val songId =
                stableId(uri)

            val retriever =
                MediaMetadataRetriever()

            try {

                retriever.setDataSource(
                    context,
                    file.uri
                )

                val title =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_TITLE
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: file.name
                            ?.substringBeforeLast(".")
                            ?.takeIf {
                                it.isNotBlank()
                            }
                        ?: "Unknown Title"

                val artist =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_ARTIST
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown Artist"

                val album =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_ALBUM
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unknown Album"

                val duration =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                        ?.toLongOrNull()
                        ?: 0L

                val artworkUri =
                    extractArtwork(
                        id = songId,
                        audioUri = uri
                    )

                val dateAdded = getFileLastModified(file.uri)
                    ?: System.currentTimeMillis()

                MusicSong(
                    id = songId,
                    title = title,
                    artist = artist,
                    album = album,
                    uri = uri,
                    duration = duration,
                    artworkUri = artworkUri,
                    dateAdded = dateAdded
                )

            } finally {

                retriever.release()
            }

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    /*
     * Extract embedded album artwork.
     */
    private fun extractArtwork(
        id: Long,
        audioUri: String
    ): String? {

        return try {

            val retriever =
                MediaMetadataRetriever()

            try {

                retriever.setDataSource(
                    context,
                    Uri.parse(audioUri)
                )

                val artworkBytes =
                    retriever.embeddedPicture
                        ?: return null

                val artworkDirectory =
                    File(
                        context.filesDir,
                        "artwork"
                    )

                if (!artworkDirectory.exists()) {
                    artworkDirectory.mkdirs()
                }

                val artworkFile =
                    File(
                        artworkDirectory,
                        "$id.jpg"
                    )

                artworkFile.writeBytes(
                    artworkBytes
                )

                artworkFile.absolutePath

            } finally {

                retriever.release()
            }

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    /*
     * Generate a stable ID from an SAF URI.
     */
    private fun stableId(
        uri: String
    ): Long {

        return uri.hashCode()
            .toLong()
            .let {
                if (it == Long.MIN_VALUE) {
                    1L
                } else {
                    kotlin.math.abs(it)
                }
            }
            .coerceAtLeast(1L)
    }

    /*
     * Query the actual last modified timestamp from
     * the SAF document provider. DocumentFile.lastModified()
     * is unreliable and often returns 0.
     */
    /*
     * Get file modification time via the file descriptor.
     * This works reliably across all SAF providers because
     * it reads the actual filesystem metadata.
     */
    private fun getFileLastModified(
        documentUri: Uri
    ): Long? {
        return try {
            contentResolver.openFileDescriptor(
                documentUri, "r"
            )?.use { pfd ->
                val stat = android.system.Os.fstat(pfd.fileDescriptor)
                if (stat.st_mtime > 0) {
                    stat.st_mtime * 1000L // seconds to ms
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}