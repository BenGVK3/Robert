package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.repository.LogRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogbookScreen() {
    val context = LocalContext.current
    val repository = remember { LogRepository(DatabaseModule.logDao(context)) }
    val viewModel: LogbookViewModel = viewModel(
        factory = RobertViewModelFactory { LogbookViewModel(repository) }
    )

    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Log")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.Medium)
        ) {
            Text(
                text = "Logbook",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { entry ->
                    LogEntryItem(entry)
                }
            }
        }
    }

    if (showAddDialog) {
        AddLogDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { callsign, freq, band, mode ->
                viewModel.addLog(callsign, freq, band, mode)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LogEntryItem(entry: LogEntryEntity) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = entry.callsign, style = MaterialTheme.typography.titleLarge)
                Text(text = dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${entry.frequency} MHz (${entry.band}) • ${entry.mode}")
            if (entry.notes.isNotBlank()) {
                Text(text = entry.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var callsign by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var band by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Log Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                TextField(value = callsign, onValueChange = { callsign = it }, label = { Text("Callsign") })
                TextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency") })
                TextField(value = band, onValueChange = { band = it }, label = { Text("Band") })
                TextField(value = mode, onValueChange = { mode = it }, label = { Text("Mode") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(callsign, frequency, band, mode) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
