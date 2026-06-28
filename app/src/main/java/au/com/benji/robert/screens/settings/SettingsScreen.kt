package au.com.benji.robert.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import au.com.benji.robert.theme.Spacing

@Composable
fun SettingsScreen() {
    var callsign by remember { mutableStateOf("VK3XYZ") }
    var name by remember { mutableStateOf("Benji") }
    var gridSquare by remember { mutableStateOf("QF22og") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(text = "Station Information", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = callsign,
            onValueChange = { callsign = it },
            label = { Text("Callsign") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = gridSquare,
            onValueChange = { gridSquare = it },
            label = { Text("Grid Square") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Save settings */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}