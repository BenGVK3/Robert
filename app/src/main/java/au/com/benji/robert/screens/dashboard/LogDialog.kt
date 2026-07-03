package au.com.benji.robert.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.benji.robert.utils.BandUtils
import au.com.benji.robert.components.RobertTextField
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDialog(
    existingEntry: LogEntryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (LogEntryEntity) -> Unit
) {
    var callsign by remember { mutableStateOf(existingEntry?.callsign ?: "") }
    var name by remember { mutableStateOf(existingEntry?.name ?: "") }
    var qth by remember { mutableStateOf(existingEntry?.qth ?: "") }
    var frequency by remember { mutableStateOf(existingEntry?.frequency ?: "") }
    var band by remember { mutableStateOf(existingEntry?.band ?: "") }
    var mode by remember { mutableStateOf(existingEntry?.mode ?: "") }
    var rstSent by remember { mutableStateOf(existingEntry?.rstSent ?: "59") }
    var rstReceived by remember { mutableStateOf(existingEntry?.rstReceived ?: "59") }
    var power by remember { mutableStateOf(existingEntry?.power ?: "") }
    var notes by remember { mutableStateOf(existingEntry?.notes ?: "") }
    
    var sotaRef by remember { mutableStateOf(existingEntry?.sotaRef ?: "") }
    var potaRef by remember { mutableStateOf(existingEntry?.potaRef ?: "") }
    var wwffRef by remember { mutableStateOf(existingEntry?.wwffRef ?: "") }
    var hemaRef by remember { mutableStateOf(existingEntry?.hemaRef ?: "") }
    var siotaRef by remember { mutableStateOf(existingEntry?.siotaRef ?: "") }
    var vkShireRef by remember { mutableStateOf(existingEntry?.vkShireRef ?: "") }
    
    var timestamp by remember { mutableLongStateOf(existingEntry?.timestamp ?: System.currentTimeMillis()) }
    
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val zuluFormat = remember { SimpleDateFormat("HH:mm'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val localFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = { 
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (existingEntry == null) "Log New QSO" else "Edit Log Entry") 
                        }
                    },
                    actions = {
                        if (existingEntry != null) {
                            IconButton(onClick = { uriHandler.openUri("https://www.qrz.com/db/${existingEntry.callsign}") }) {
                                Icon(Icons.Default.Public, contentDescription = "QRZ")
                            }
                        }
                        TextButton(onClick = {
                            val newEntry = LogEntryEntity(
                                id = existingEntry?.id ?: 0,
                                callsign = callsign.uppercase().trim(),
                                name = name.trim(),
                                qth = qth.trim(),
                                frequency = frequency.trim(),
                                band = band.trim(),
                                mode = mode.trim(),
                                rstSent = rstSent.trim(),
                                rstReceived = rstReceived.trim(),
                                power = power.trim(),
                                timestamp = timestamp,
                                notes = notes.trim(),
                                sotaRef = sotaRef.trim(),
                                potaRef = potaRef.trim(),
                                wwffRef = wwffRef.trim(),
                                hemaRef = hemaRef.trim(),
                                siotaRef = siotaRef.trim(),
                                vkShireRef = vkShireRef.trim()
                            )
                            onConfirm(newEntry)
                        }, enabled = callsign.isNotBlank()) {
                            Text("SAVE")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.History, contentDescription = "Back")
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    // Callsign Section
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Box(modifier = Modifier.weight(1f)) {
                                RobertTextField(value = callsign, onValueChange = { callsign = it }, label = "Callsign")
                            }
                            Button(onClick = { if (!callsign.endsWith("/P")) callsign += "/P" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("/P")
                            }
                            Button(onClick = { if (!callsign.endsWith("/M")) callsign += "/M" }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("/M")
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Box(modifier = Modifier.weight(1f)) { RobertTextField(value = name, onValueChange = { name = it }, label = "Name") }
                            Box(modifier = Modifier.weight(1f)) { RobertTextField(value = qth, onValueChange = { qth = it }, label = "QTH") }
                        }
                    }

                    // Radio Details
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Box(modifier = Modifier.weight(1f)) { 
                            RobertTextField(
                                value = frequency, 
                                onValueChange = { 
                                    frequency = it
                                    it.toDoubleOrNull()?.let { freq ->
                                        val detectedBand = BandUtils.getBandFromFrequency(freq)
                                        if (detectedBand.isNotEmpty()) {
                                            band = detectedBand
                                        }
                                    }
                                }, 
                                label = "Freq (kHz)"
                            ) 
                        }
                        Box(modifier = Modifier.weight(1f)) { RobertTextField(value = band, onValueChange = { band = it }, label = "Band") }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Box(modifier = Modifier.weight(1f)) { RobertTextField(value = mode, onValueChange = { mode = it }, label = "Mode") }
                        Box(modifier = Modifier.weight(1f)) { RobertTextField(value = power, onValueChange = { power = it }, label = "Power (W)") }
                    }

                    // RST Section
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Box(modifier = Modifier.weight(1f)) { RobertTextField(value = rstSent, onValueChange = { if (it.length <= 3) rstSent = it }, label = "Sent") }
                        Box(modifier = Modifier.weight(1f)) { RobertTextField(value = rstReceived, onValueChange = { if (it.length <= 3) rstReceived = it }, label = "Rcvd") }
                    }

                    // References Section
                    Text("Activity References", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    var refExpanded by remember { mutableStateOf(false) }
                    var selectedRefType by remember { 
                        mutableStateOf(
                            when {
                                existingEntry?.sotaRef?.isNotEmpty() == true -> "SOTA"
                                existingEntry?.potaRef?.isNotEmpty() == true -> "POTA"
                                existingEntry?.wwffRef?.isNotEmpty() == true -> "WWFF"
                                existingEntry?.hemaRef?.isNotEmpty() == true -> "HEMA"
                                existingEntry?.siotaRef?.isNotEmpty() == true -> "SiOTA"
                                existingEntry?.vkShireRef?.isNotEmpty() == true -> "VK Shire"
                                else -> "None"
                            }
                        )
                    }
                    val refTypes = listOf("None", "SOTA", "POTA", "WWFF", "HEMA", "SiOTA", "VK Shire")

                    ExposedDropdownMenuBox(
                        expanded = refExpanded,
                        onExpandedChange = { refExpanded = !refExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRefType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reference Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = refExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = refExpanded, onDismissRequest = { refExpanded = false }) {
                            refTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedRefType = type
                                        refExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    when (selectedRefType) {
                        "SOTA" -> RobertTextField(value = sotaRef, onValueChange = { sotaRef = it }, label = "SOTA Reference")
                        "POTA" -> RobertTextField(value = potaRef, onValueChange = { potaRef = it }, label = "POTA Reference")
                        "WWFF" -> RobertTextField(value = wwffRef, onValueChange = { wwffRef = it }, label = "WWFF Reference")
                        "HEMA" -> RobertTextField(value = hemaRef, onValueChange = { hemaRef = it }, label = "HEMA Reference")
                        "SiOTA" -> RobertTextField(value = siotaRef, onValueChange = { siotaRef = it }, label = "SiOTA Reference")
                        "VK Shire" -> RobertTextField(value = vkShireRef, onValueChange = { vkShireRef = it }, label = "VK Shire Reference")
                    }

                    // Time Section
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text("Time & Date (Local)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        
                        var dateTimeStr by remember { mutableStateOf(fullDateFormat.format(Date(timestamp))) }
                        
                        RobertTextField(
                            value = dateTimeStr, 
                            onValueChange = { 
                                dateTimeStr = it
                                try {
                                    val parsedDate = fullDateFormat.parse(it)
                                    if (parsedDate != null) {
                                        timestamp = parsedDate.time
                                    }
                                } catch (e: Exception) {
                                    // Invalid format, don't update timestamp yet
                                }
                            }, 
                            label = "YYYY-MM-DD HH:MM"
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Zulu: ${zuluFormat.format(Date(timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text("Local: ${localFormat.format(Date(timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    RobertTextField(value = notes, onValueChange = { notes = it }, label = "Notes")
                    
                    Spacer(modifier = Modifier.height(Spacing.Large))
                }
            }
        }
    }
}
