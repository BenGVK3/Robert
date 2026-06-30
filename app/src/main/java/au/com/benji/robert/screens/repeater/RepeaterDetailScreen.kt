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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterDetailScreen(
    callsign: String,
    frequency: String,
    onBack: () -> Unit,
    viewModel: RepeaterViewModel = viewModel()
) {
    val repeaters by viewModel.filteredRepeaters.collectAsStateWithLifecycle()
    val repeater = remember(repeaters, callsign, frequency) {
        repeaters.find { it.callsign == callsign && it.frequency == frequency }
    }
    val equipment by viewModel.userEquipment.collectAsStateWithLifecycle()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(repeater) {
        repeater?.let { viewModel.markAsRecent(it) }
    }

    Scaffold(
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Large)) {
                        Text(text = repeater.callsign, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(text = repeater.name ?: "Unknown Name", style = MaterialTheme.typography.titleMedium)
                        
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailHeaderItem("OUTPUT", repeater.frequency)
                            DetailHeaderItem("INPUT", repeater.inputFreq ?: "--")
                            DetailHeaderItem("OFFSET", repeater.offset)
                        }
                    }
                }

                // Robert Integration: Recommended Radio
                RobertIntegrationSection(repeater, equipment, viewModel)

                // Technical Info
                SectionHeader("TECHNICAL DATA", Icons.Default.Settings)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        DetailRow("Band", repeater.band ?: "--")
                        DetailRow("Mode", repeater.mode ?: "--")
                        DetailRow("Tone / CTCSS", repeater.tone ?: "None")
                        DetailRow("DCS", repeater.dcs ?: "None")
                        DetailRow("Status", repeater.status ?: "Unknown")
                    }
                }

                // Location Info
                SectionHeader("LOCATION", Icons.Default.LocationOn)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        DetailRow("Town / State", "${repeater.town ?: "--"}, ${repeater.state ?: ""}")
                        DetailRow("Position", "${String.format("%.4f", repeater.lat)}, ${String.format("%.4f", repeater.lng)}")
                        DetailRow("Grid Square", repeater.gridSquare ?: "--")
                        DetailRow("Elevation", "${repeater.elevation ?: "0"} m")
                        DetailRow("Distance", "${String.format("%.1f", repeater.distance)} km")
                        DetailRow("Bearing", "${repeater.bearing.toInt()}° (${repeater.direction})")
                    }
                }

                // Notes
                if (!repeater.notes.isNullOrBlank()) {
                    SectionHeader("NOTES", Icons.Default.Notes)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = repeater.notes,
                            modifier = Modifier.padding(Spacing.Medium),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Actions
                Button(
                    onClick = {
                        val programStr = "CH: ${repeater.callsign}, RX: ${repeater.frequency}, TX: ${repeater.inputFreq ?: ""}, TONE: ${repeater.tone ?: ""}"
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

@Composable
fun RobertIntegrationSection(repeater: Repeater, equipment: List<ShackEntity>, viewModel: RepeaterViewModel) {
    val reachability = viewModel.getReachability(repeater, equipment)
    val recommended = viewModel.getRecommendedEquipment(repeater, equipment)
    
    SectionHeader("ROBERT INTEGRATION", Icons.Default.AutoAwesome)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Likelihood of reach: ", style = MaterialTheme.typography.titleSmall)
                ReachabilityIndicator(reachability)
            }
            
            if (recommended != null) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text("Recommended Radio: ", style = MaterialTheme.typography.labelSmall)
                Text("${recommended.manufacturer} ${recommended.model} (${recommended.nickname})", fontWeight = FontWeight.Bold)
                
                // Suggested Antenna (Dummy logic for now)
                Text("Suggested Antenna: ", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = Spacing.Small))
                val antenna = if (repeater.band?.contains("2m") == true) "VHF J-Pole or 5/8 Wave" else "Dual-band Collinear"
                Text(antenna, fontWeight = FontWeight.Bold)
            } else {
                Text("Add your radios in the Shack tool for range estimates.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ReachabilityIndicator(reachability: Reachability) {
    val (text, color) = when (reachability) {
        Reachability.EXCELLENT -> "🟢 Excellent" to Color(0xFF4CAF50)
        Reachability.POSSIBLE -> "🟡 Possible" to Color(0xFFFFC107)
        Reachability.UNLIKELY -> "🔴 Unlikely" to Color(0xFFF44336)
        Reachability.UNKNOWN -> "⚪ Unknown" to Color.Gray
    }
    Text(text = text, color = color, fontWeight = FontWeight.Bold)
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailHeaderItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}
