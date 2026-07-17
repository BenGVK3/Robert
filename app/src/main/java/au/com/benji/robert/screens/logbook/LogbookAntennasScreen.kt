package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.AntennaProfile
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookAntennasScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val antennas by viewModel.antennas.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antennas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (antennas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No antennas added yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                items(antennas) { antenna ->
                    AntennaItem(antenna, onDelete = { viewModel.deleteAntenna(antenna) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddAntennaDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, gain, pol, notes ->
                viewModel.addAntenna(AntennaProfile(
                    name = name, 
                    type = type, 
                    gain = gain,
                    polarisation = pol,
                    notes = notes
                ))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AntennaItem(antenna: AntennaProfile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SettingsInputAntenna, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(Spacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(antenna.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${antenna.type} • ${antenna.gain} dBi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddAntennaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var gain by remember { mutableStateOf("0.0") }
    var pol by remember { mutableStateOf("Horizontal") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Antenna Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Antenna Name (e.g. EFHW)") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. Wire, Yagi)") })
                OutlinedTextField(value = gain, onValueChange = { gain = it }, label = { Text("Gain (dBi)") })
                OutlinedTextField(value = pol, onValueChange = { pol = it }, label = { Text("Polarisation") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, type, gain.toDoubleOrNull() ?: 0.0, pol, notes) },
                enabled = name.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
