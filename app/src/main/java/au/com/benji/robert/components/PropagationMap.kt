package au.com.benji.robert.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.benji.robert.models.PskSpot
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.utils.TerminatorUtils
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import java.util.*

@Composable
fun PropagationMap(
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    userLat: Double?,
    userLon: Double?,
    propagationData: PropagationData?,
    spots: List<PskSpot>,
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    var selectedSpot by remember { mutableStateOf<PskSpot?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCollapse() }
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = "LIVE PROPAGATION MAP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = !isCollapsed) {
                Column {
                    // Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        BandFilterDropdown(selectedBand, onBandSelected, modifier = Modifier.weight(1f))
                        ModeFilterDropdown(selectedMode, onModeSelected, modifier = Modifier.weight(1f))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    ) {
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(
                                LatLng(userLat ?: 0.0, userLon ?: 0.0),
                                2f
                            )
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                myLocationButtonEnabled = true
                            ),
                            properties = MapProperties(
                                mapType = MapType.NORMAL,
                                isMyLocationEnabled = userLat != null
                            )
                        ) {
                            // User Location Marker
                            if (userLat != null && userLon != null) {
                                Marker(
                                    state = rememberMarkerState(position = LatLng(userLat, userLon)),
                                    title = "Your Station",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                                
                                // Propagation Coverage Regions
                                propagationData?.let { data ->
                                    val currentBandData = if (selectedBand == "All") {
                                        data.bands.maxByOrNull { it.score }
                                    } else {
                                        data.bands.find { it.band == selectedBand }
                                    }
                                    
                                    currentBandData?.let { band ->
                                        val color = try {
                                            Color(android.graphics.Color.parseColor(band.color))
                                        } catch (e: Exception) {
                                            Color.Green
                                        }
                                        
                                        // Draw coverage circles
                                        val radius = (band.score / 100f) * 20000000.0 // meters
                                        Circle(
                                            center = LatLng(userLat, userLon),
                                            radius = radius,
                                            fillColor = color.copy(alpha = 0.1f),
                                            strokeColor = color.copy(alpha = 0.3f),
                                            strokeWidth = 2f
                                        )
                                        
                                        if (band.score > 40) {
                                            Circle(
                                                center = LatLng(userLat, userLon),
                                                radius = radius * 0.6,
                                                fillColor = color.copy(alpha = 0.15f),
                                                strokeWidth = 0f
                                            )
                                        }
                                    }
                                }
                            }

                            // Grey Line Overlay
                            val terminatorPoints = remember { TerminatorUtils.calculateTerminator() }
                            if (terminatorPoints.isNotEmpty()) {
                                val latLngs = terminatorPoints.map { LatLng(it.first, it.second) }
                                Polyline(
                                    points = latLngs,
                                    color = Color.Black.copy(alpha = 0.4f),
                                    width = 10f,
                                    geodesic = true
                                )
                            }

                            // PSK Spots with Clustering
                            val clusterItems = remember(spots) {
                                spots.map { PskClusterItem(it) }
                            }
                            Clustering(
                                items = clusterItems,
                                onClusterItemClick = { item ->
                                    selectedSpot = item.spot
                                    true
                                }
                            )
                        }
                        
                        // Legend
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(Spacing.Small)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(Spacing.ExtraSmall)
                        ) {
                            Column {
                                LegendItem("Excellent", Color(0xFF4CAF50))
                                LegendItem("Good", Color(0xFF8BC34A))
                                LegendItem("Fair", Color(0xFFFFC107))
                                LegendItem("Poor", Color(0xFFF44336))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSpot?.let { spot ->
        AlertDialog(
            onDismissRequest = { selectedSpot = null },
            confirmButton = {
                TextButton(onClick = { selectedSpot = null }) {
                    Text("Close")
                }
            },
            title = { Text(spot.callsign, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    DetailLine("Grid", spot.grid)
                    DetailLine("Mode", spot.mode)
                    DetailLine("Frequency", "${spot.frequency} MHz")
                    DetailLine("Distance", "${String.format("%.0f", spot.distance)} km")
                    DetailLine("Bearing", "${String.format("%.1f", spot.bearing)}°")
                    val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(spot.reportTime))
                    DetailLine("Heard at", time)
                }
            }
        )
    }
}

@Composable
fun DetailLine(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

class PskClusterItem(val spot: PskSpot) : com.google.maps.android.clustering.ClusterItem {
    override fun getPosition(): LatLng = LatLng(spot.lat, spot.lon)
    override fun getTitle(): String = spot.callsign
    override fun getSnippet(): String = "${spot.mode} | ${spot.frequency} MHz"
    override fun getZIndex(): Float? = null
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandFilterDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val bands = listOf("All", "160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Band", fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            bands.forEach { band ->
                DropdownMenuItem(
                    text = { Text(band) },
                    onClick = {
                        onSelected(band)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeFilterDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("All", "FT8", "FT4", "CW", "SSB", "WSPR")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mode", fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}
