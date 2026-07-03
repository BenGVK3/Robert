package au.com.benji.robert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import au.com.benji.robert.navigation.BottomNavItems
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.theme.RobertColors

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    onCommandCenterClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val firstHalf = BottomNavItems.take(2)
    val secondHalf = BottomNavItems.drop(2)

    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .height(80.dp),
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        // First two items
        firstHalf.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (navController.currentDestination?.route == item.route) return@NavigationBarItem
                    
                    if (!navController.popBackStack(item.route, inclusive = false)) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    item.icon?.let { Icon(imageVector = it, contentDescription = item.title) }
                        ?: item.iconRes?.let {
                            if (it == au.com.benji.robert.R.drawable.logbook1 || 
                                it == au.com.benji.robert.R.drawable.home1 ||
                                it == au.com.benji.robert.R.drawable.tools1 ||
                                it == au.com.benji.robert.R.drawable.settings1) {
                                Image(
                                    painter = painterResource(id = it),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(painter = painterResource(id = it), contentDescription = item.title)
                            }
                        }
                },
                label = { Text(text = item.title, fontSize = 11.sp) }
            )
        }

        // Center Command Center Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onCommandCenterClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Command Center",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Last two items
        secondHalf.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (navController.currentDestination?.route == item.route) return@NavigationBarItem
                    
                    if (!navController.popBackStack(item.route, inclusive = false)) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    item.icon?.let { Icon(imageVector = it, contentDescription = item.title) }
                        ?: item.iconRes?.let {
                            if (it == au.com.benji.robert.R.drawable.logbook1 || 
                                it == au.com.benji.robert.R.drawable.home1 ||
                                it == au.com.benji.robert.R.drawable.tools1 ||
                                it == au.com.benji.robert.R.drawable.settings1) {
                                Image(
                                    painter = painterResource(id = it),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(painter = painterResource(id = it), contentDescription = item.title)
                            }
                        }
                },
                label = { Text(text = item.title, fontSize = 11.sp) }
            )
        }
    }
}
