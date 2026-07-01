package au.com.benji.robert.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.theme.Spacing

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
                RobertTextField(value = frequency, onValueChange = { frequency = it }, label = "Frequency (kHz)")
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
