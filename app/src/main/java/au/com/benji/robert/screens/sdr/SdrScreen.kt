package au.com.benji.robert.screens.sdr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.benji.robert.theme.Spacing

@Composable
fun SdrScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "SDR Control", style = MaterialTheme.typography.headlineMedium)
        
        Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Waterfall Display Placeholder")
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Freq: 144.800 MHz", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {}) { Text("Tune") }
        }
        
        Text("Connect to an RTL-SDR or similar device to see real-time spectrum.", style = MaterialTheme.typography.bodyMedium)
    }
}
