package au.com.benji.robert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import au.com.benji.robert.components.BottomNavigationBar
import au.com.benji.robert.navigation.RobertNavHost
import au.com.benji.robert.repository.SettingsRepository
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RobertNavHost(navController)
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
