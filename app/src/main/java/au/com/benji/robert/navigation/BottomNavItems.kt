package au.com.benji.robert.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null
)

val BottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        route = Screen.Dashboard.route,
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        title = "Logbook",
        route = Screen.Logbook.route,
        icon = Icons.Default.EditNote
    ),
    BottomNavItem(
        title = "Tools",
        route = Screen.Tools.route,
        icon = Icons.Default.Build
    ),
    BottomNavItem(
        title = "Settings",
        route = Screen.Settings.route,
        icon = Icons.Default.Settings
    )
)
