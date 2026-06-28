package au.com.benji.robert.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val viewModel: SettingsViewModel = viewModel(
        factory = RobertViewModelFactory { SettingsViewModel(repository) }
    )

    val savedCallsign by viewModel.callsign.collectAsStateWithLifecycle()
    val savedName by viewModel.name.collectAsStateWithLifecycle()
    val savedGridSquare by viewModel.gridSquare.collectAsStateWithLifecycle()

    var callsign by remember(savedCallsign) { mutableStateOf(savedCallsign) }
    var name by remember(savedName) { mutableStateOf(savedName) }
    var gridSquare by remember(savedGridSquare) { mutableStateOf(savedGridSquare) }

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
            onClick = { viewModel.saveSettings(callsign, name, gridSquare) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}
