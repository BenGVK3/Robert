package au.com.benji.robert.screens.propagation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropagationScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // PSK Reporter URL with grey line and 80m FT8 anyone last 15 mins settings
    val pskReporterUrl = "https://pskreporter.info/pskmap.html?show-daynight=1&band=3500000&mode=FT8&timerange=900"
    var isMapFullscreen by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }

    // Auto refresh timer
    var timeLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(timeLeft, isRefreshing) {
        if (isRefreshing) {
            timeLeft = 60
        } else if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else {
            viewModel.refresh()
            timeLeft = 60
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
        item {
            Text(
                text = "Propagation Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PROPAGATION MAP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isMapExpanded) {
                        TextButton(
                            onClick = { isMapExpanded = false },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("HIDE MAP")
                            Icon(
                                Icons.Default.ExpandLess,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isMapExpanded) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            RobertMap(url = pskReporterUrl, modifier = Modifier.fillMaxSize())
                        }
                        
                        IconButton(
                            onClick = { isMapFullscreen = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                        }
                    }
                    
                    Text(
                        text = "Live FT8/Digital reception reports with integrated Grey Line.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Card(
                        onClick = { isMapExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.Medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text("SHOW MAP: Tap to view live digital traffic map", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            solarData?.let { data ->
                SolarDataCard(data)
            }
        }

        propagationData?.ducting?.let { ducting ->
            item {
                Text(
                    text = "Atmospheric Ducting",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ducting.isActive) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = if (ducting.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(
                                text = if (ducting.isActive) "ENHANCED PROPAGATION" else "NORMAL CONDITIONS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text(text = ducting.description, style = MaterialTheme.typography.bodyLarge)
                        if (ducting.isActive) {
                            Spacer(modifier = Modifier.height(Spacing.Small))
                            Text(
                                text = "Intensity: ${ducting.intensity}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Live Band Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Auto-refresh in ${timeLeft}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }

        propagationData?.let { data ->
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
                        text = "Data combines real-time NOAA solar indices and PSK Reporter live traffic density.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(Spacing.Large)) }
    }
}

    if (isMapFullscreen) {
        Dialog(
            onDismissRequest = { isMapFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    RobertMap(url = pskReporterUrl, modifier = Modifier.fillMaxSize())
                    
                    IconButton(
                        onClick = { isMapFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SolarDataCard(data: SolarData) {
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
                        Text("MUF", style = MaterialTheme.typography.labelSmall)
                        Text(data.muf, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
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
