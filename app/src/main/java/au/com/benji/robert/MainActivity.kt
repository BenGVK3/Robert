package au.com.benji.robert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import au.com.benji.robert.components.BottomNavigationBar
import au.com.benji.robert.components.CommandCenterSheet
import au.com.benji.robert.components.CommandActionType
import au.com.benji.robert.components.DialogType
import au.com.benji.robert.components.GlobalDialogs
import au.com.benji.robert.navigation.RobertNavHost
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.ui.theme.RobertTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(this)

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
    val dashboardViewModel: DashboardViewModel = viewModel()
    
    var showCommandCenter by remember { mutableStateOf(false) }
    var showDxSpots by remember { mutableStateOf(false) }
    var showShack by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                onCommandCenterClick = { showCommandCenter = true }
            )
        }
    ) { innerPadding ->
        RobertNavHost(
            navController = navController,
            paddingValues = innerPadding
        )
        
        if (showCommandCenter) {
            CommandCenterSheet(
                onNavigate = { route -> navController.navigate(route) },
                onAction = { action ->
                    when (action) {
                        is CommandActionType.Dialog -> {
                            when (action.type) {
                                DialogType.DX_SPOTS -> showDxSpots = true
                                DialogType.SHACK -> showShack = true
                                else -> {}
                            }
                        }
                        else -> {}
                    }
                },
                onDismiss = { showCommandCenter = false }
            )
        }

        GlobalDialogs(
            viewModel = dashboardViewModel,
            showDxSpots = showDxSpots,
            onDismissDxSpots = { showDxSpots = false },
            showShack = showShack,
            onDismissShack = { showShack = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RobertAppPreview() {
    RobertTheme {
        RobertApp()
    }
}
