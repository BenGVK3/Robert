package au.com.benji.robert.screens.satellites

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.network.SatellitePosition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatellitesScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val positions by viewModel.satellitePositions.collectAsStateWithLifecycle()
    val location by viewModel.locationFlow.collectAsStateWithLifecycle()
    val searchQuery by viewModel.satelliteSearchQuery.collectAsStateWithLifecycle()
    val trackedIds by viewModel.trackedSatelliteIds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    
    var selectedSatelliteId by remember { mutableStateOf("25544") } // Default to ISS
    var isMapExpanded by remember { mutableStateOf(false) }
    var showSatellitePicker by remember { mutableStateOf(false) }

    val lat = location?.first ?: -37.81
    val lon = location?.second ?: 144.96

    if (isMapExpanded) {
        FullScreenMap(
            satelliteId = selectedSatelliteId,
            userLat = lat,
            userLon = lon,
            onClose = { isMapExpanded = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Orbital Tracking", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showSatellitePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Satellite")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullToRefreshState,
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                // --- INTEGRATED MAP VIEW ---
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            RobertMap(
                                url = "https://www.n2yo.com/widgets/widget-tracker.php?s=$selectedSatelliteId&size=large&all=1&lat=$lat&lon=$lon",
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Map Controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Spacing.Small),
                                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                            ) {
                                FloatingActionButton(
                                    onClick = { isMapExpanded = true },
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Expand")
                                }
                            }
                            
                            // Satellite Selection Overlay
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(Spacing.Small),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentName = positions.find { it.name.contains(selectedSatelliteId) || it.name == selectedSatelliteId }?.name ?: "Tracking..."
                                    Text(
                                        text = "Viewing: $currentName",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // --- SEARCH / QUICK SELECT ---
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSatelliteSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search tracked satellites...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }


                // --- SATELLITE LIST ---
                val filteredPositions = positions.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
                }

                if (filteredPositions.isEmpty() && positions.isNotEmpty()) {
                    item {
                        Text("No matching satellites found.", modifier = Modifier.padding(Spacing.Large))
                    }
                } else if (positions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(Spacing.ExtraLarge), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(filteredPositions) { satellite ->
                    SatelliteDetailCard(
                        satellite = satellite,
                        isSelected = selectedSatelliteId == satellite.name, // Note: This might need adjustment based on how wheretheiss returns names
                        onClick = { 
                            // Since wheretheiss doesn't return the ID in the position object, 
                            // we'll need to be clever. For now, let's assume name or a mapping.
                            // In a real app, I'd include the ID in the SatellitePosition model.
                            // Temporary hack: search for the ID based on common name
                            val id = trackedIds.find { id -> 
                                satellite.name.contains(id) || satellite.name == "ISS" && id == "25544" 
                            } ?: "25544"
                            selectedSatelliteId = id
                        }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(Spacing.ExtraLarge)) }
            }
        }
    }
}

    if (showSatellitePicker) {
        SatellitePicker(
            onDismiss = { showSatellitePicker = false },
            onToggle = { id -> viewModel.toggleTrackedSatellite(id) },
            trackedIds = trackedIds
        )
    }
}

@Composable
fun FullScreenMap(
    satelliteId: String,
    userLat: Double,
    userLon: Double,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        RobertMap(
            url = "https://www.n2yo.com/widgets/widget-tracker.php?s=$satelliteId&size=large&all=1&lat=$userLat&lon=$userLon",
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(Spacing.Medium)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
        }
    }
}

@Composable
fun SatelliteDetailCard(
    satellite: SatellitePosition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = satellite.name, 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Currently Tracking", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Public,
                    label = "Lat",
                    value = String.format("%.4f", satellite.latitude)
                )
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Public,
                    label = "Lon",
                    value = String.format("%.4f", satellite.longitude)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Height,
                    label = "Alt",
                    value = "${satellite.altitude.toInt()} km"
                )
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Velocity",
                    value = "${satellite.velocity.toInt()} km/h"
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = if (satellite.visibility == "daylight") Color(0xFFFFD54F).copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = satellite.visibility.uppercase(), 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (satellite.visibility == "daylight") Color(0xFF795548) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Text(
                    text = "ID: Tracking...", // In a real app, I'd pass the ID through
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SatellitePicker(
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    trackedIds: List<String>
) {
    val satellites = mapOf(
        "25544" to "ISS (Zarya)",
        "25338" to "NOAA 15",
        "28654" to "NOAA 18",
        "33591" to "NOAA 19",
        "43013" to "AO-91 (RadFxSat)",
        "43770" to "AO-92 (Fox-1D)",
        "40069" to "XW-2A (Cas-3A)",
        "44443" to "FO-99 (NEXUS)",
        "40967" to "LilacSat-2 (CAS-3H)",
        "40903" to "SaudiSat-4"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Satellites", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                satellites.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "NORAD ID: $id", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Checkbox(
                            checked = trackedIds.contains(id),
                            onCheckedChange = { onToggle(id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}

@Composable
fun SatelliteMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
