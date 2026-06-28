package au.com.benji.robert.screens.aprs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.benji.robert.theme.Spacing

@Composable
fun AprsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        Text(text = "APRS Map", style = MaterialTheme.typography.headlineMedium)
        
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Map showing nearby stations")
            }
        }
        
        Text("Listening for APRS packets...", style = MaterialTheme.typography.bodySmall)
    }
}
