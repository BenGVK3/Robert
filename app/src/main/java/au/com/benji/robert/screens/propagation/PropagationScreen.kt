package au.com.benji.robert.screens.propagation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.MufCalculator
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

import au.com.benji.robert.repository.propagation.PropagationState
import androidx.compose.foundation.lazy.LazyRow
import au.com.benji.robert.components.PropagationMap
import java.text.SimpleDateFormat
import java.util.*

import au.com.benji.robert.repository.propagation.PropagationSpot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropagationScreen(
    paddingValues: PaddingValues,
    dashboardViewModel: DashboardViewModel = viewModel(),
    propagationViewModel: PropagationViewModel = viewModel()
) {
    val solarData by dashboardViewModel.solarData.collectAsStateWithLifecycle()
    val mufResult by dashboardViewModel.mufResult.collectAsStateWithLifecycle()
    val bandConditions by dashboardViewModel.propagationData.collectAsStateWithLifecycle()
    
    val state by propagationViewModel.state.collectAsStateWithLifecycle()
    val selectedBand by propagationViewModel.selectedBand.collectAsStateWithLifecycle()
    val selectedMode by propagationViewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedTimeWindow by propagationViewModel.selectedTimeWindow.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()
    
    var showGreyLine by remember { mutableStateOf(true) }
    var selectedSpot by remember { mutableStateOf<PropagationSpot?>(null) }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { propagationViewModel.manualRefresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Image(
                        painter = painterResource(id = au.com.benji.robert.R.drawable.propagation),
                        contentDescription = null,
                        modifier = Modifier
                            .size(62.dp)
                            .offset(x = 5.dp),
                        colorFilter = null,
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Propagation Center",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Status Card
            item {
                StatusCard(state)
            }

            // Map and Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = "LIVE PROPAGATION MAP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Map Area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            PropagationMap(
                                spots = state.spots,
                                showGreyLine = showGreyLine,
                                modifier = Modifier.fillMaxSize(),
                                onSpotSelected = { selectedSpot = it }
                            )
                            
                            if (state.error != null) {
                                Text(
                                    text = "Map Error: ${state.error}",
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Grey Line Toggle
                            FilterChip(
                                selected = showGreyLine,
                                onClick = { showGreyLine = !showGreyLine },
                                label = { Text("GREY LINE", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    selectedSpot?.let { spot ->
                        SpotDetailCard(spot) { selectedSpot = null }
                    }

                    // Filters
                    BandFilterRow(selectedBand) { propagationViewModel.setBand(it) }
                    ModeFilterRow(selectedMode) { propagationViewModel.setMode(it) }
                    TimeFilterRow(selectedTimeWindow) { propagationViewModel.setTimeWindow(it) }
                }
            }

            item {
                solarData?.let { data ->
                    SolarDataCard(data, mufResult)
                }
            }

            item {
                Text(
                    text = "Live Band Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            bandConditions?.let { data ->
                items(data.bands) { band ->
                    BandConditionRow(band)
                }
            } ?: item { 
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text("Calculating band stability...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = "Data combines real-time NOAA solar indices and multi-provider live traffic density.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(Spacing.Large)) }
        }
    }
}

@Composable
fun SpotDetailCard(spot: PropagationSpot, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${spot.senderCallsign} ➔ ${spot.receiverCallsign}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailItem("Band", spot.band)
                DetailItem("Mode", spot.mode)
                DetailItem("Distance", "${spot.distance.toInt()} km")
                DetailItem("SNR", spot.snr?.let { "$it dB" } ?: "---")
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            val timeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(spot.timestamp))
            Text(text = "Time: $timeText UTC", style = MaterialTheme.typography.labelSmall)
            Text(text = "From: ${spot.senderLocator} | To: ${spot.receiverLocator}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusCard(state: PropagationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusItem("STATIONS", state.spots.map { it.senderCallsign }.distinct().size.toString())
                StatusItem("PATHS", state.spots.size.toString())
                StatusItem("PROVIDERS", state.activeProviders.size.toString())
                val lastUpdateText = if (state.lastUpdate > 0) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(state.lastUpdate))
                } else "---"
                StatusItem("UPDATED", lastUpdateText)
            }
            if (state.activeProviders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = "Sources: ${state.activeProviders.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun BandFilterRow(selectedBand: String, onSelect: (String) -> Unit) {
    val bands = listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bands) { band ->
            FilterChip(
                selected = selectedBand == band,
                onClick = { onSelect(band) },
                label = { Text(band) }
            )
        }
    }
}

@Composable
fun ModeFilterRow(selectedMode: String, onSelect: (String) -> Unit) {
    val modes = listOf("FT8", "FT4", "WSPR", "CW", "SSB", "Digital", "All")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(modes) { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode) }
            )
        }
    }
}

@Composable
fun TimeFilterRow(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val timeWindows = listOf(
        15 to "15m",
        30 to "30m",
        60 to "1h",
        180 to "3h",
        360 to "6h",
        720 to "12h",
        1440 to "24h"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(timeWindows) { (mins, label) ->
            FilterChip(
                selected = selectedMinutes == mins,
                onClick = { onSelect(mins) },
                label = { Text(label) }
            )
        }
    }
}


@Composable
fun SolarDataCard(data: SolarData, mufResult: MufCalculator.MufResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "HAMQSL SOLAR INDICES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("SFI", data.solarFlux.toString())
                MetricItem("SN", data.sunspots.toString())
                MetricItem("A-Idx", data.aIndex.toString())
                MetricItem("K-Idx", data.kIndex.toString())
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("X-Ray", data.xRay)
                    DetailRow("Wind", data.solarWind)
                    DetailRow("Magnetic", data.magneticField)
                }
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("Proton", data.protonFlux)
                    DetailRow("Electron", data.electronFlux)
                    DetailRow("Aurora", data.aurora)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Small)) {
                        Text(
                            text = if (mufResult.isEstimated) "Estimated MUF" else "Reported MUF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mufResult.isEstimated) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", mufResult.value),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " MHz",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Small)) {
                        Text("foF2", style = MaterialTheme.typography.labelSmall)
                        Text(data.foF2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text("VHF CONDITIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VHF Aurora: ${data.vhfAurora}", style = MaterialTheme.typography.bodySmall)
                Text("E-Skip: ${data.eSkip}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BandConditionRow(band: BandCondition) {
    val color = when (band.rating) {
        "Excellent" -> Color(0xFF4CAF50)
        "Good" -> Color(0xFF8BC34A)
        "Fair" -> Color(0xFFFFC107)
        "Poor" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val (trendIcon, trendColor) = when (band.trend) {
        "Improving" -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF4CAF50)
        "Declining" -> Icons.AutoMirrored.Filled.TrendingDown to Color(0xFFF44336)
        else -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = band.band, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = trendColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = band.trend,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor
                    )
                }
            }
            
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = band.rating.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
        }
    }
}
