package com.pyracube.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeSurface
import com.pyracube.music.ui.theme.PyracubeSurfaceVariant
import com.pyracube.music.ui.theme.PyracubeTextPrimary
import com.pyracube.music.ui.theme.PyracubeTextSecondary

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {

    val recentSongs = listOf(
        "Midnight Drive",
        "Afterglow",
        "Neon Skies",
        "Lost in Time"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PyracubeBackground),

        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        )
    ) {

        item {

            Text(
                text = "Good evening",
                color = PyracubeTextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Pyracube",
                color = PyracubeTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Quick Access",
                color = PyracubeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                QuickAccessCard(
                    icon = "♡",
                    title = "Favorites",
                    modifier = Modifier.weight(1f)
                )

                QuickAccessCard(
                    icon = "♫",
                    title = "Playlists",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        item {

            Text(
                text = "Recently Played",
                color = PyracubeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                items(recentSongs) { song ->

                    RecentSongCard(song)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        item {

            Text(
                text = "Your Music",
                color = PyracubeTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            MusicActionRow(
                icon = "♫",
                title = "All Songs",
                subtitle = "Browse your offline collection"
            )

            HorizontalDivider(
                color = Color(0xFF202927),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            MusicActionRow(
                icon = "▣",
                title = "Albums",
                subtitle = "Explore albums"
            )

            HorizontalDivider(
                color = Color(0xFF202927),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            MusicActionRow(
                icon = "♬",
                title = "Artists",
                subtitle = "Explore artists"
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    icon: String,
    title: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PyracubeSurface)
            .padding(16.dp),

        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = icon,
            color = PyracubeAccent,
            fontSize = 26.sp
        )

        Text(
            text = title,
            color = PyracubeTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecentSongCard(
    title: String
) {

    Column(
        modifier = Modifier.width(145.dp)
    ) {

        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PyracubeSurfaceVariant),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "♫",
                color = PyracubeAccent,
                fontSize = 42.sp
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = title,
            color = PyracubeTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Pyracube Artist",
            color = PyracubeTextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MusicActionRow(
    icon: String,
    title: String,
    subtitle: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(PyracubeSurface),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = icon,
                color = PyracubeAccent,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = PyracubeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                color = PyracubeTextSecondary,
                fontSize = 13.sp
            )
        }

        Text(
            text = "›",
            color = PyracubeTextSecondary,
            fontSize = 26.sp
        )
    }
}