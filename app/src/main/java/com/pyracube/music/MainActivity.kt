package com.pyracube.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import com.pyracube.music.data.DownloadRepository
import com.pyracube.music.data.DownloadQueueController
import com.pyracube.music.data.MusicRepository
import com.pyracube.music.data.MusicStorageManager
import com.pyracube.music.player.PlayerManager
import com.pyracube.music.permissions.musicPermission
import com.pyracube.music.ui.components.BottomNavigationBar
import com.pyracube.music.ui.components.MiniPlayer
import com.pyracube.music.ui.screens.DownloadsScreen
import com.pyracube.music.ui.screens.HomeScreen
import com.pyracube.music.ui.screens.LibraryScreen
import com.pyracube.music.ui.screens.MoreScreen
import com.pyracube.music.ui.screens.PlayerScreen
import com.pyracube.music.ui.screens.SearchScreen
import com.pyracube.music.ui.screens.StorageScreen
import com.pyracube.music.ui.theme.PyracubeTheme
import com.pyracube.music.data.YtDlpDownloadEngine
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import java.io.File
import androidx.lifecycle.lifecycleScope
import com.pyracube.music.data.YouTubeThumbnailTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import android.util.Log
import dev.ffmpegkit_maintained.ytdlp.YtDlp

private val PyracubeBackground = Color(0xFF080C0B)
private val PyracubeAccent = Color(0xFF39E6A5)
private val PyracubeText = Color(0xFFF1F7F5)
private val PyracubeSecondaryText = Color(0xFF91A09B)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        YtDlp.init(this)

        setContent {
            PyracubeTheme {
                PyracubeApp()
            }
        }
    }
}

@Composable
fun PyracubeApp() {

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var showPlayer by remember {
        mutableStateOf(false)
    }

    var showStorageScreen by remember {
        mutableStateOf(false)
    }

    /*
     * Used to trigger a new music scan
     * whenever the selected storage folder changes.
     */
    var musicSyncRequest by remember {
        mutableIntStateOf(0)
    }

    var isRefreshingLibrary by remember {
        mutableStateOf(false)
    }


    val context =
        androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // PLAYER
    // =========================================================

    val playerManager =
        remember {
            PlayerManager(context)
        }

    val playerState by
    playerManager.playerState.collectAsState()

    // =========================================================
    // MUSIC REPOSITORY
    // =========================================================

    val repository =
        remember {
            MusicRepository(context)
        }

    val songs by
    repository
        .observeAllSongs()
        .collectAsState(initial = emptyList())

    // =========================================================
    // DOWNLOAD REPOSITORY
    // =========================================================

    val downloadRepository =
        remember {
            DownloadRepository(context)
        }

    val downloads by
    downloadRepository
        .observeDownloads()
        .collectAsState(initial = emptyList())

    // =========================================================
    // DOWNLOAD QUEUE CONTROLLER
    // =========================================================

    LaunchedEffect(Unit) {
        DownloadQueueController.getInstance(context).initialize()
    }

    // =========================================================
    // STORAGE
    // =========================================================

    val storageManager =
        remember {
            MusicStorageManager(context)
        }

    // =========================================================
    // MUSIC SYNC
    // =========================================================

    LaunchedEffect(musicSyncRequest) {
        repository.syncMusic()
    }

    // =========================================================
    // MUSIC PERMISSION
    // =========================================================

    val musicPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {
            // Music scanning is handled by syncMusic().
        }

    LaunchedEffect(Unit) {

        val permission =
            musicPermission()

        val granted =
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) {
            musicPermissionLauncher.launch(permission)
        }
    }

    // =========================================================
    // MEDIA CONTROLLER
    // =========================================================

    var controller by remember {
        mutableStateOf<MediaController?>(null)
    }

    LaunchedEffect(playerManager) {

        val future =
            playerManager.controllerFuture

        future.addListener(
            {
                try {

                    controller =
                        future.get()

                    playerManager.setController(
                        controller!!
                    )

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // =========================================================
    // FULL PLAYER
    // =========================================================

    if (showPlayer) {

        PlayerScreen(
            playerState = playerState,

            onBack = {
                showPlayer = false
            },

            onPlayPause = {
                playerManager.playPause()
            },

            onPrevious = {
                playerManager.previous()
            },

            onNext = {
                playerManager.next()
            },

            onSeek = { position ->
                playerManager.seekTo(position)
            },

            onToggleShuffle = {
                playerManager.toggleShuffle()
            },

            onCycleRepeat = {
                playerManager.cycleRepeatMode()
            }
        )

        return
    }

    // =========================================================
    // STORAGE SCREEN
    // =========================================================

    if (showStorageScreen) {

        StorageScreen(
            storageManager = storageManager,

            onBack = {
                showStorageScreen = false
            },

            onFolderChanged = {
                musicSyncRequest++
            },

            modifier = Modifier.fillMaxSize()
        )

        return
    }

    // =========================================================
    // MAIN APP
    // =========================================================

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PyracubeBackground
    ) {

        Scaffold(
            containerColor = PyracubeBackground,

            bottomBar = {

                Column {

                    MiniPlayer(
                        playerState = playerState,

                        onClick = {
                            showPlayer = true
                        },

                        onPlayPause = {
                            playerManager.playPause()
                        }
                    )

                    BottomNavigationBar(
                        selectedIndex = selectedTab,

                        onItemSelected = {
                            selectedTab = it
                        }
                    )
                }
            }

        ) { paddingValues ->

            when (selectedTab) {

                // =================================================
                // HOME
                // =================================================

                0 -> {

                    HomeScreen(
                        modifier =
                            Modifier.padding(
                                paddingValues
                            )
                    )
                }

                // =================================================
                // SEARCH
                // =================================================

                1 -> {

                    SearchScreen(
                        modifier =
                            Modifier.padding(
                                paddingValues
                            )
                    )
                }

                // =================================================
                // LIBRARY
                // =================================================

                2 -> {

                    LibraryScreen(
                        songs = songs,

                        onSongClick = { song, sortedSongs ->

                            controller?.let {

                                playerManager.playSong(
                                    controller = it,
                                    songs = sortedSongs,
                                    selectedSong = song
                                )
                            }
                        },

                        isRefreshing = isRefreshingLibrary,

                        onRefresh = {
                            isRefreshingLibrary = true
                            scope.launch {
                                repository.syncMusic()
                                isRefreshingLibrary = false
                            }
                        },

                        modifier =
                            Modifier.padding(
                                paddingValues
                            )
                    )
                }

                // =================================================
                // DOWNLOADS
                // =================================================

                3 -> {

                    DownloadsScreen(
                        downloads = downloads,
                        downloadRepository = downloadRepository,
                        modifier = Modifier.padding(paddingValues)
                    )

                }

                // =================================================
                // MORE
                // =================================================

                4 -> {

                    MoreScreen(
                        storageManager = storageManager,

                        onMusicStorageClick = {
                            showStorageScreen = true
                        },

                        modifier =
                            Modifier.padding(
                                paddingValues
                            )
                    )
                }
            }
        }
    }
}

// =============================================================
// PLACEHOLDER SCREEN
// =============================================================

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    PyracubeBackground
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            modifier =
                Modifier.padding(30.dp)
        ) {

            Text(
                text = "♫",
                color = PyracubeAccent,
                fontSize = 52.sp
            )

            Text(
                text = title,
                color = PyracubeText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = PyracubeSecondaryText,
                fontSize = 15.sp
            )
        }
    }
}