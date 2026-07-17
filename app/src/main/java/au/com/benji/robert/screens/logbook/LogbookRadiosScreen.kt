package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.RadioProfile
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookRadiosScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radios", fontWeight = FontWeight.Bold) },
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
        if (radios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No radios added yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                items(radios) { radio ->
                    RadioItem(radio, onDelete = { viewModel.deleteRadio(radio) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddRadioDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mfg, power, modes, notes ->
                viewModel.addRadio(RadioProfile(
                    name = name, 
                    manufacturer = mfg, 
                    maxPower = power,
                    supportedModes = modes.split(",").map { it.trim() },
                    notes = notes
                ))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RadioItem(radio: RadioProfile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Radio, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(Spacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(radio.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${radio.manufacturer} • ${radio.maxPower}W", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddRadioDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mfg by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("100") }
    var modes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Radio Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Radio Name (e.g. IC-7300)") })
                OutlinedTextField(value = mfg, onValueChange = { mfg = it }, label = { Text("Manufacturer") })
                OutlinedTextField(value = power, onValueChange = { power = it }, label = { Text("Max Power (Watts)") })
                OutlinedTextField(value = modes, onValueChange = { modes = it }, label = { Text("Modes (CSV: SSB,CW,FT8)") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, mfg, power.toDoubleOrNull() ?: 100.0, modes, notes) },
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
