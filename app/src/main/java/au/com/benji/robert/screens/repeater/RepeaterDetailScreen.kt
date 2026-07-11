package au.com.benji.robert.screens.repeater

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.theme.Spacing
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterDetailScreen(
    callsign: String,
    frequency: String,
    onBack: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: RepeaterViewModel = viewModel()
) {
    val allRepeaters by viewModel.repeaters.collectAsStateWithLifecycle()
    val repeater = remember(allRepeaters, callsign, frequency) {
        allRepeaters.find { it.callsign == callsign && it.frequency == frequency }
    }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(repeater) {
        repeater?.let { viewModel.markAsRecent(it) }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text(callsign) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        repeater?.let {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Repeater: ${it.callsign}\nFreq: ${it.frequency}\nLocation: ${it.town ?: it.location ?: ""}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Repeater"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { 
                        repeater?.let { viewModel.toggleFavorite(it) }
                    }) {
                        Icon(
                            if (repeater?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (repeater?.isFavorite == true) Color.Red else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (repeater == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading technical data...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                val formattedFreq = formatFreq(repeater.frequency)
                val formattedInput = if (!repeater.inputFreq.isNullOrBlank()) formatFreq(repeater.inputFreq) else "--"
                
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Large)) {
                        Text(text = repeater.callsign, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(text = repeater.name ?: repeater.location ?: "Unknown Site", style = MaterialTheme.typography.titleMedium)
                        
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailHeaderItem("OUTPUT", "$formattedFreq MHz")
                            DetailHeaderItem("INPUT", if (formattedInput != "--") "$formattedInput MHz" else "--")
                            
                            val calculatedOffset = remember(repeater) {
                                if (repeater.offset.isNotBlank()) {
                                    formatOffset(repeater.offset)
                                } else if (!repeater.inputFreq.isNullOrBlank()) {
                                    try {
                                        val out = repeater.frequency.toDouble()
                                        val inp = repeater.inputFreq.toDouble()
                                        val diff = inp - out
                                        val formatted = String.format("%.3f", diff)
                                        if (diff > 0) "+$formatted" else formatted
                                    } catch (e: Exception) { "--" }
                                } else {
                                    "--"
                                }
                            }
                            DetailHeaderItem("OFFSET", calculatedOffset)
                        }
                    }
                }

                // Technical Info
                SectionHeader("TECHNICAL DATA", Icons.Default.Settings)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        DetailRow("Frequency", "$formattedFreq MHz")
                        DetailRow("Input Freq", if (formattedInput != "--") "$formattedInput MHz" else "Standard Offset")
                        
                        val techOffset = remember(repeater) {
                            if (repeater.offset.isNotBlank()) {
                                formatOffset(repeater.offset)
                            } else if (!repeater.inputFreq.isNullOrBlank()) {
                                try {
                                    val out = repeater.frequency.toDouble()
                                    val inp = repeater.inputFreq.toDouble()
                                    val diff = inp - out
                                    val formatted = String.format("%.3f", diff)
                                    if (diff > 0) "+$formatted" else formatted
                                } catch (e: Exception) { "0.000" }
                            } else {
                                "0.000"
                            }
                        }
                        DetailRow("Offset", techOffset)

                        DetailRow("Band", repeater.band?.ifBlank { calculateBand(repeater.frequency) } ?: calculateBand(repeater.frequency))
                        DetailRow("Mode", repeater.mode?.ifBlank { "FM" } ?: "FM")
                        
                        val displayTone = remember(repeater.tone) {
                            val t = repeater.tone?.trim() ?: ""
                            if (t.isBlank() || t.equals("None", true) || t.equals("OFF", true) || t.equals("0", true) || t.equals("0.0", true)) {
                                "None"
                            } else {
                                if (t.all { it.isDigit() || it == '.' }) "$t Hz" else t
                            }
                        }
                        DetailRow("Tone / CTCSS", displayTone)

                        if (!repeater.dcs.isNullOrBlank()) DetailRow("DCS", repeater.dcs)
                        DetailRow("Status", repeater.status?.ifBlank { "Operational" } ?: "Operational")
                        
                        // Digital / Networking details
                        if (!repeater.irlpId.isNullOrBlank()) DetailRow("IRLP ID", repeater.irlpId)
                        if (!repeater.echolinkId.isNullOrBlank()) DetailRow("EchoLink ID", repeater.echolinkId)
                        if (!repeater.allstarNode.isNullOrBlank()) DetailRow("AllStar Node", repeater.allstarNode)
                        if (!repeater.wiresX.isNullOrBlank()) DetailRow("Wires-X ID", repeater.wiresX)
                        if (!repeater.dmrId.isNullOrBlank()) DetailRow("DMR ID", repeater.dmrId)
                        if (!repeater.colorCode.isNullOrBlank()) DetailRow("Color Code", repeater.colorCode)
                        if (!repeater.timeSlot.isNullOrBlank()) DetailRow("Time Slot", repeater.timeSlot)
                        if (!repeater.talkgroup.isNullOrBlank()) DetailRow("Talkgroup", repeater.talkgroup)
                    }
                }

                // Location Info
                SectionHeader("LOCATION", Icons.Default.LocationOn)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        DetailRow("Town / Locality", repeater.town ?: repeater.location ?: "Unknown")
                        DetailRow("Site Name", repeater.location ?: "Unknown")
                        
                        val displayState = repeater.state?.ifBlank { inferStateFromCallsign(repeater.callsign) } 
                            ?: inferStateFromCallsign(repeater.callsign) ?: "--"
                        DetailRow("State", displayState)

                        DetailRow("Position", "${String.format("%.4f", repeater.lat)}, ${String.format("%.4f", repeater.lng)}")
                        DetailRow("Grid Square", repeater.maidenhead.ifBlank { repeater.gridSquare ?: "Unknown" })
                        DetailRow("Elevation", if (!repeater.elevation.isNullOrBlank()) "${repeater.elevation} m" else "Not Specified")
                        DetailRow("Distance", "${String.format("%.1f", repeater.distance)} km")
                        DetailRow("Bearing", "${repeater.bearing.toInt()}° (${repeater.direction})")
                    }
                }

                // Actions
                Button(
                    onClick = {
                        val programStr = buildString {
                            append("CH: ${repeater.callsign}")
                            append(", RX: $formattedFreq")
                            if (formattedInput != "--") append(", TX: $formattedInput")
                            if (!repeater.tone.isNullOrBlank()) append(", TONE: ${repeater.tone}")
                            if (!repeater.mode.isNullOrBlank()) append(", MODE: ${repeater.mode}")
                            append(", OFFSET: ${repeater.offset}")
                        }
                        clipboardManager.setText(AnnotatedString(programStr))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text("COPY PROGRAMMING INFO")
                }
                
                Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
            }
        }
    }
}

private fun formatFreq(freq: String): String {
    return try {
        val bd = BigDecimal(freq.trim())
        val scaled = bd.stripTrailingZeros()
        if (scaled.scale() <= 3) {
            // Ensure at least 3 decimal places if it's simpler
            bd.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().let {
                if (!it.contains(".")) "$it.000"
                else {
                    val parts = it.split(".")
                    val decimal = parts[1].padEnd(3, '0')
                    "${parts[0]}.$decimal"
                }
            }
        } else {
            scaled.toPlainString()
        }
    } catch (e: Exception) {
        freq
    }
}

private fun calculateBand(freqStr: String): String {
    val freq = freqStr.toDoubleOrNull() ?: return "Unknown"
    return when {
        freq in 28.0..29.7 -> "10m"
        freq in 50.0..54.0 -> "6m"
        freq in 144.0..148.0 -> "2m"
        freq in 420.0..450.0 -> "70cm"
        freq in 1240.0..1300.0 -> "23cm"
        freq > 2300.0 -> "SHF"
        else -> "${freq.toInt()} MHz"
    }
}

private fun formatOffset(offset: String): String {
    if (offset.isBlank()) return "0.000"
    val clean = offset.replace("mhz", "", ignoreCase = true).trim()
    if (clean.startsWith("+") || clean.startsWith("-")) return clean
    return try {
        val value = clean.toDouble()
        if (value > 0) "+$clean" else clean
    } catch (e: Exception) {
        clean
    }
}

private fun inferStateFromCallsign(callsign: String): String? {
    val upper = callsign.uppercase()
    return when {
        upper.startsWith("VK1") -> "ACT"
        upper.startsWith("VK2") -> "NSW"
        upper.startsWith("VK3") -> "VIC"
        upper.startsWith("VK4") -> "QLD"
        upper.startsWith("VK5") -> "SA"
        upper.startsWith("VK6") -> "WA"
        upper.startsWith("VK7") -> "TAS"
        upper.startsWith("VK8") -> "NT"
        upper.startsWith("VK9") -> "External Territory"
        upper.startsWith("VK0") -> "Antarctica"
        else -> null
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun DetailHeaderItem(label: String, value: String) {
    Column {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.titleSmall, // Smaller to ensure it fits
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}
