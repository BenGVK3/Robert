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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import au.com.benji.robert.components.BottomNavigationBar
import au.com.benji.robert.navigation.RobertNavHost
import au.com.benji.robert.ui.theme.RobertTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            RobertTheme {
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
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier.padding(innerPadding)
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