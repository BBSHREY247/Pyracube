package com.pyracube.music.data

import android.content.Context
import android.content.Intent
import android.net.Uri

class MusicStorageManager(
    private val context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "pyracube_storage",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val MUSIC_TREE_URI = "music_tree_uri"
    }

    fun saveMusicFolder(uri: Uri) {

        preferences.edit()
            .putString(
                MUSIC_TREE_URI,
                uri.toString()
            )
            .apply()
    }

    fun getMusicFolder(): Uri? {

        val uriString =
            preferences.getString(
                MUSIC_TREE_URI,
                null
            )

        return uriString?.let {
            Uri.parse(it)
        }
    }

    fun hasMusicFolder(): Boolean {
        return getMusicFolder() != null
    }

    fun takePersistablePermission(
        uri: Uri
    ) {

        val flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        context.contentResolver.takePersistableUriPermission(
            uri,
            flags
        )
    }

    fun clearMusicFolder() {

        val uri = getMusicFolder()

        if (uri != null) {

            try {

                context.contentResolver
                    .releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

            } catch (_: Exception) {
                // Permission may already be unavailable.
            }
        }

        preferences.edit()
            .remove(MUSIC_TREE_URI)
            .apply()
    }
}