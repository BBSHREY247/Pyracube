package com.pyracube.music.ui.components

import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pyracube.music.ui.theme.PyracubeAccent
import com.pyracube.music.ui.theme.PyracubeBackground
import com.pyracube.music.ui.theme.PyracubeTextMuted
import com.pyracube.music.ui.theme.PyracubeTextPrimary

data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val navigationItems = listOf(
    BottomNavItem("Home", Icons.Default.Home),
    BottomNavItem("Search", Icons.Default.Search),
    BottomNavItem("Library", Icons.Default.LibraryMusic),
    BottomNavItem("Downloads", Icons.Default.Download),
    BottomNavItem("More", Icons.Default.MoreHoriz)
)

@Composable
fun BottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = PyracubeBackground,
        tonalElevation = 0.dp
    ) {
        navigationItems.forEachIndexed { index, item ->

            NavigationBarItem(
                selected = selectedIndex == index,

                onClick = {
                    onItemSelected(index)
                },

                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },

                label = {
                    Text(item.label)
                },

                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = PyracubeAccent,
                    selectedTextColor = PyracubeAccent,
                    unselectedIconColor = PyracubeTextMuted,
                    unselectedTextColor = PyracubeTextMuted,
                    indicatorColor = Color(0xFF18352C)
                )
            )
        }
    }
}