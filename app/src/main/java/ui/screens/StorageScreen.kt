package com.pyracube.music.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.pyracube.music.data.MusicStorageManager
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeTextPrimary

@Composable
fun StorageScreen(
    storageManager: MusicStorageManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFolderChanged: () -> Unit = {}
) {

    BackHandler {
        onBack()
    }

    var selectedFolder by remember {
        mutableStateOf(
            storageManager.getMusicFolder()
        )
    }

    val folderPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    /*
                     * Keep the permission that Android
                     * actually granted to us.
                     */
                    val takeFlags =
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    storageManager.takePersistablePermission(
                        uri
                    )

                    storageManager.saveMusicFolder(
                        uri
                    )

                    selectedFolder = uri

                    /*
                     * Tell MainActivity to rescan
                     * the music library immediately.
                     */
                    onFolderChanged()

                } catch (e: Exception) {

                    e.printStackTrace()
                }
            }
        }

    val folderName =
        selectedFolder?.let { uri ->

            DocumentFile
                .fromTreeUri(
                    androidx.compose.ui.platform.LocalContext.current,
                    uri
                )
                ?.name

        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PyracubeBackground)
            .padding(20.dp)
    ) {

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            androidx.compose.material3.IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PyracubeTextPrimary
                )
            }

            Text(
                text = "Music Storage",
                color = PyracubeTextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // -----------------------------------------------------
        // DESCRIPTION
        // -----------------------------------------------------

        Text(
            text = "Choose the folder Pyracube uses for your music.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // -----------------------------------------------------
        // SELECTED FOLDER
        // -----------------------------------------------------

        if (selectedFolder != null) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = PyracubeAccent
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {

                    Text(
                        text = "Music Folder",
                        color = PyracubeTextPrimary,
                        style =
                            MaterialTheme.typography.labelLarge
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            folderName
                                ?: "Selected folder",
                        color = PyracubeTextPrimary,
                        style =
                            MaterialTheme.typography.titleMedium
                    )
                }
            }

        } else {

            Text(
                text = "No music folder selected",
                color = PyracubeTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // -----------------------------------------------------
        // BUTTON
        // -----------------------------------------------------

        Button(
            onClick = {
                folderPicker.launch(null)
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (selectedFolder != null) {
                        "Change Music Folder"
                    } else {
                        "Choose Music Folder"
                    }
            )
        }
    }
}