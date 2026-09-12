package com.pyracube.music.data

sealed class PlaylistResult {
    data class Success(
        val tasksCreated: Int,
        val duplicatesSkipped: Int,
        val unavailableSkipped: Int
    ) : PlaylistResult()

    data class Error(val message: String) : PlaylistResult()
}
