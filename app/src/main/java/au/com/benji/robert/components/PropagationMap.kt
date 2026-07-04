package au.com.benji.robert.components

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.benji.robert.models.PskSpot
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.utils.TerminatorUtils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
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
    val context = LocalContext.current
    var selectedSpot by remember { mutableStateOf<PskSpot?>(null) }

    // Configure osmdroid
    SideEffect {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "Robert-Ham-App/1.0"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCollapse() }
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(text = "LIVE PROPAGATION MAP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Icon(if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = null)
            }

            AnimatedVisibility(visible = !isCollapsed) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
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
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                    setMultiTouchControls(true)
                                    controller.setZoom(3.0)
                                    controller.setCenter(GeoPoint(userLat ?: 0.0, userLon ?: 0.0))
                                }
                            },
                            update = { mapView ->
                                mapView.overlays.clear()

                                // User Location
                                if (userLat != null && userLon != null) {
                                    val userPoint = GeoPoint(userLat, userLon)
                                    val userMarker = Marker(mapView).apply {
                                        position = userPoint
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Your Station"
                                    }
                                    mapView.overlays.add(userMarker)

                                    // Coverage
                                    propagationData?.let { data ->
                                        val bandData = if (selectedBand == "All") data.bands.maxByOrNull { it.score } else data.bands.find { it.band == selectedBand }
                                        bandData?.let { band ->
                                            val colorInt = try { android.graphics.Color.parseColor(band.color) } catch (e: Exception) { android.graphics.Color.GREEN }
                                            val radiusKm = (band.score / 100f) * 15000.0 // Simplified coverage
                                            val circlePoints = Polygon.pointsAsCircle(userPoint, radiusKm * 1000.0)
                                            val polygon = Polygon(mapView).apply {
                                                points = circlePoints
                                                fillPaint.color = Color(colorInt).copy(alpha = 0.15f).toArgb()
                                                outlinePaint.color = Color(colorInt).copy(alpha = 0.4f).toArgb()
                                                outlinePaint.strokeWidth = 2f
                                            }
                                            mapView.overlays.add(polygon)
                                        }
                                    }
                                }

                                // Grey Line
                                val terminatorPoints = TerminatorUtils.calculateTerminator()
                                if (terminatorPoints.isNotEmpty()) {
                                    val polyline = Polyline(mapView).apply {
                                        val pts = terminatorPoints.map { GeoPoint(it.first, it.second) }
                                        setPoints(pts)
                                        outlinePaint.color = android.graphics.Color.BLACK
                                        outlinePaint.alpha = 100
                                        outlinePaint.strokeWidth = 5f
                                    }
                                    mapView.overlays.add(polyline)
                                }

                                // Spots (Simplified without cluster library for now to ensure stability)
                                spots.take(100).forEach { spot ->
                                    val spotMarker = Marker(mapView).apply {
                                        position = GeoPoint(spot.lat, spot.lon)
                                        title = spot.callsign
                                        snippet = "${spot.mode} | ${spot.frequency} MHz"
                                        setOnMarkerClickListener { m, _ ->
                                            selectedSpot = spot
                                            m.showInfoWindow()
                                            true
                                        }
                                    }
                                    mapView.overlays.add(spotMarker)
                                }
                                
                                mapView.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Legend
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.Small)
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
            confirmButton = { TextButton(onClick = { selectedSpot = null }) { Text("Close") } },
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

fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
           (this.red * 255.0f + 0.5f).toInt() shl 16 or
           (this.green * 255.0f + 0.5f).toInt() shl 8 or
           (this.blue * 255.0f + 0.5f).toInt()
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text("Band", fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            bands.forEach { band -> DropdownMenuItem(text = { Text(band) }, onClick = { onSelected(band); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeFilterDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("All", "FT8", "FT4", "CW", "SSB", "WSPR")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text("Mode", fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { mode -> DropdownMenuItem(text = { Text(mode) }, onClick = { onSelected(mode); expanded = false }) }
        }
    }
}

@Composable
fun DetailLine(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
