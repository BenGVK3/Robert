package au.com.benji.robert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import au.com.benji.robert.navigation.BottomNavItems

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {

    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    NavigationBar {
        BottomNavItems.forEach { item ->

            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route)
                },
                icon = {
                    if (item.title == "Propagation" && item.iconRes != null) {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title,
                            colorFilter = null,
                            modifier = Modifier
                                .size(42.dp)
                                .offset(x = 4.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (item.iconRes != null) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title
                        )
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}