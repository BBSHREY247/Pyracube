package com.pyracube.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PyracubeDarkColorScheme = darkColorScheme(
    primary = PyracubeAccent,
    onPrimary = PyracubeBlack,

    primaryContainer = PyracubeAccentDark,
    onPrimaryContainer = PyracubeTextPrimary,

    secondary = PyracubeAccentBright,
    onSecondary = PyracubeBlack,

    background = PyracubeBackground,
    onBackground = PyracubeTextPrimary,

    surface = PyracubeSurface,
    onSurface = PyracubeTextPrimary,

    surfaceVariant = PyracubeSurfaceVariant,
    onSurfaceVariant = PyracubeTextSecondary,

    outline = PyracubeDivider,

    error = PyracubeError,
    onError = PyracubeBlack
)

@Composable
fun PyracubeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(LocalContext.current)
        }

        else -> PyracubeDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PyracubeTypography,
        content = content
    )
}