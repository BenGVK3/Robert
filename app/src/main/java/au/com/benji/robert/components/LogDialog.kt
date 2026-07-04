package au.com.benji.robert.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import au.com.benji.robert.utils.BandUtils
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun LogDetailDialog(
    entry: LogEntryEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val zuluFormat = remember { SimpleDateFormat("HH:mm'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "QSO DETAIL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = entry.callsign, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    text = fullDateFormat.format(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    DetailInfoItem(label = "FREQUENCY", value = "${entry.frequency} kHz", modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "MODE", value = entry.mode, modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    DetailInfoItem(label = "BAND", value = entry.band, modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "TIME", value = zuluFormat.format(entry.timestamp), modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    DetailInfoItem(label = "SENT", value = entry.rstSent, modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "RECEIVED", value = entry.rstReceived, modifier = Modifier.weight(1f))
                }

                if (entry.power.isNotEmpty()) {
                    DetailInfoItem(label = "POWER", value = "${entry.power} W")
                }

                if (entry.name.isNotEmpty() || entry.qth.isNotEmpty()) {
                    val info = listOf(entry.name, entry.qth).filter { it.isNotEmpty() }.joinToString(" - ")
                    DetailInfoItem(label = "OPERATOR / QTH", value = info)
                }

                if (entry.notes.isNotEmpty()) {
                    DetailInfoItem(label = "NOTES", value = entry.notes)
                }

                // References Section if any exist
                val refs = listOf(
                    "SOTA" to entry.sotaRef,
                    "POTA" to entry.potaRef,
                    "WWFF" to entry.wwffRef,
                    "HEMA" to entry.hemaRef,
                    "SIOTA" to entry.siotaRef,
                    "VK SHIRE" to entry.vkShireRef
                ).filter { it.second.isNotEmpty() }

                if (refs.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)
                    refs.forEach { (label, value) ->
                        DetailInfoItem(label = label, value = value)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Small))

                Button(
                    onClick = { uriHandler.openUri("https://www.qrz.com/db/${entry.callsign}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW QRZ PROFILE")
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("EDIT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

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
    var band by remember { mutableStateOf(existingEntry?.band ?: "20m") }
    var mode by remember { mutableStateOf(existingEntry?.mode ?: "SSB") }
    var rstSent by remember { mutableStateOf(existingEntry?.rstSent ?: "59") }
    var rstReceived by remember { mutableStateOf(existingEntry?.rstReceived ?: "59") }
    var power by remember { mutableStateOf(existingEntry?.power ?: "") }
    var notes by remember { mutableStateOf(existingEntry?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEntry == null) "Log QSO" else "Edit QSO", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                RobertTextField(value = callsign, onValueChange = { callsign = it }, label = "Callsign")
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
                    label = "Frequency (kHz)"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    RobertTextField(value = band, onValueChange = { band = it }, label = "Band", modifier = Modifier.weight(1f))
                    RobertTextField(value = mode, onValueChange = { mode = it }, label = "Mode", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    RobertTextField(value = rstSent, onValueChange = { rstSent = it }, label = "Sent", modifier = Modifier.weight(1f))
                    RobertTextField(value = rstReceived, onValueChange = { rstReceived = it }, label = "Recv", modifier = Modifier.weight(1f))
                }
                RobertTextField(value = power, onValueChange = { power = it }, label = "Power (W)")
                RobertTextField(value = name, onValueChange = { name = it }, label = "Name")
                RobertTextField(value = qth, onValueChange = { qth = it }, label = "QTH / Location")
                RobertTextField(value = notes, onValueChange = { notes = it }, label = "Notes")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        (existingEntry ?: LogEntryEntity(
                            callsign = callsign,
                            frequency = frequency,
                            band = band,
                            mode = mode,
                            rstSent = rstSent,
                            rstReceived = rstReceived,
                            timestamp = System.currentTimeMillis()
                        )).copy(
                            callsign = callsign,
                            name = name,
                            qth = qth,
                            frequency = frequency,
                            band = band,
                            mode = mode,
                            rstSent = rstSent,
                            rstReceived = rstReceived,
                            power = power,
                            notes = notes
                        )
                    )
                },
                enabled = callsign.isNotBlank() && band.isNotBlank()
            ) {
                Text(if (existingEntry == null) "Log contact" else "Update contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
