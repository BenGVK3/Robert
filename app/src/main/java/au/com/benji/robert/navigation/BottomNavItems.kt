package au.com.benji.robert.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
        iconRes = au.com.benji.robert.R.drawable.home1
    ),
    BottomNavItem(
        title = "Logbook",
        route = Screen.Logbook.route,
        iconRes = au.com.benji.robert.R.drawable.logbook1
    ),
    BottomNavItem(
        title = "Tools",
        route = Screen.Tools.route,
        iconRes = au.com.benji.robert.R.drawable.tools1
    ),
    BottomNavItem(
        title = "Settings",
        route = Screen.Settings.route,
        iconRes = au.com.benji.robert.R.drawable.settings1
    )
)
