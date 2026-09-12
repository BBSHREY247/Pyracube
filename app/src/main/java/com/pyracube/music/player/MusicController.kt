package com.pyracube.music.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MusicController(context: Context) {

    private val sessionToken = SessionToken(
        context,
        ComponentName(
            context,
            MusicPlaybackService::class.java
        )
    )

    val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            context,
            sessionToken
        ).buildAsync()
}