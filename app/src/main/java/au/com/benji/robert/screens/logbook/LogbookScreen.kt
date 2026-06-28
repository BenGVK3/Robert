package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            Text(text = "Logbook", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Keep track of your contacts and signal reports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your logbook is empty. Log your first QSO!", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { entry ->
                        LogEntryItem(
                            entry = entry,
                            onDelete = { viewModel.deleteLog(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLogDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { callsign, freq, band, mode, notes ->
                viewModel.addLog(callsign, freq, band, mode, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LogEntryItem(
    entry: LogEntryEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = entry.callsign.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = entry.band,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = entry.mode,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(text = "${entry.frequency} MHz", style = MaterialTheme.typography.bodySmall)
                }

                if (entry.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = entry.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var callsign by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var band by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New QSO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(
                    value = callsign, 
                    onValueChange = { callsign = it }, 
                    label = { Text("Callsign") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = frequency, 
                        onValueChange = { frequency = it }, 
                        label = { Text("Freq (MHz)") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = band, 
                        onValueChange = { band = it }, 
                        label = { Text("Band") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = mode, 
                    onValueChange = { mode = it }, 
                    label = { Text("Mode (FT8, SSB...)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes, 
                    onValueChange = { notes = it }, 
                    label = { Text("Notes") }, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(callsign, frequency, band, mode, notes) },
                enabled = callsign.isNotBlank()
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
