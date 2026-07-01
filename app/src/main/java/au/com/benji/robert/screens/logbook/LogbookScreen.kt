package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.*
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@Composable
fun LogbookScreen(
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    
    var showAddLogDialog by remember { mutableStateOf(false) }
    var selectedLogEntry by remember { mutableStateOf<LogEntryEntity?>(null) }
    var logToDelete by remember { mutableStateOf<LogEntryEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Text(
                    text = "Logbook",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep track of your contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLogDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add QSO")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.Medium)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptySectionCard("Your logbook is empty. Tap + to add your first contact!")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    for (entry in logs) {
                        LogEntryItem(
                            entry = entry,
                            onClick = { selectedLogEntry = entry },
                            onDelete = { logToDelete = entry }
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
                }
            }
        }
    }

    if (showAddLogDialog) {
        LogDialog(
            onDismiss = { showAddLogDialog = false },
            onConfirm = { entry ->
                viewModel.addLog(
                    callsign = entry.callsign,
                    name = entry.name,
                    qth = entry.qth,
                    frequency = entry.frequency,
                    band = entry.band,
                    mode = entry.mode,
                    rstSent = entry.rstSent,
                    rstReceived = entry.rstReceived,
                    power = entry.power,
                    timestamp = entry.timestamp,
                    notes = entry.notes,
                    sotaRef = entry.sotaRef,
                    potaRef = entry.potaRef,
                    wwffRef = entry.wwffRef,
                    hemaRef = entry.hemaRef,
                    siotaRef = entry.siotaRef,
                    vkShireRef = entry.vkShireRef
                )
                showAddLogDialog = false
            }
        )
    }

    selectedLogEntry?.let { entry ->
        LogDialog(
            existingEntry = entry,
            onDismiss = { selectedLogEntry = null },
            onConfirm = { updatedEntry ->
                viewModel.updateLog(updatedEntry)
                selectedLogEntry = null
            }
        )
    }

    logToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Log Entry?") },
            text = { Text("Are you sure you want to delete the QSO with ${entry.callsign}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLog(entry)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
