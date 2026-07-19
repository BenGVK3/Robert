package au.com.benji.robert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.components.BottomNavigationBar
import au.com.benji.robert.components.CommandCenterSheet
import au.com.benji.robert.components.CommandActionType
import au.com.benji.robert.components.DialogType
import au.com.benji.robert.components.GlobalDialogs
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.navigation.RobertNavHost
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.ui.theme.RobertTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(DatabaseModule.cacheDao(application))

        enableEdgeToEdge()

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = "System")
            
            val useDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            RobertTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RobertApp()
                }
            }
        }
    }
}

@Composable
fun RobertApp() {
    val navController = rememberNavController()
    
    var showCommandCenter by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                onCommandCenterClick = { showCommandCenter = !showCommandCenter }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            RobertNavHost(
                navController = navController,
                paddingValues = innerPadding
            )

            AnimatedVisibility(
                visible = showCommandCenter,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                CommandCenterSheet(
                    onNavigate = { route -> 
                        if (navController.currentDestination?.route == route) {
                            showCommandCenter = false
                            return@CommandCenterSheet
                        }
                        
                        val popped = navController.popBackStack(route, inclusive = false)
                        if (!popped) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        showCommandCenter = false
                    },
                    onAction = { action ->
                        when (action) {
                            is CommandActionType.Dialog -> {
                                when (action.type) {
                                    DialogType.DX_SPOTS -> navController.navigate(Screen.DxSpots.route)
                                    DialogType.SHACK -> navController.navigate(Screen.Shack.route)
                                    else -> {}
                                }
                            }
                            else -> {}
                        }
                        showCommandCenter = false
                    },
                    onDismiss = { showCommandCenter = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RobertAppPreview() {
    RobertTheme {
        RobertApp()
    }
}
