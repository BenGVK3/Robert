package au.com.benji.robert.screens.satellites

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextAlign
import au.com.benji.robert.R
import au.com.benji.robert.network.SatellitePosition
import au.com.benji.robert.network.SatellitePass
import au.com.benji.robert.network.SatelliteCommInfo
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import au.com.benji.robert.components.RobertHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatellitesScreen(
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    val position by viewModel.satellitePosition.collectAsStateWithLifecycle()
    val location by viewModel.locationFlow.collectAsStateWithLifecycle()
    val passes by viewModel.upcomingPasses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.satelliteSearchQuery.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedSatelliteId.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSatelliteIds.collectAsStateWithLifecycle()
    val available = viewModel.availableSatellites

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val userLat = (location?.first ?: -37.81).takeIf { !it.isNaN() } ?: -37.81
    val userLon = (location?.second ?: 144.96).takeIf { !it.isNaN() } ?: 144.96

    val categories = listOf("All", "Amateur", "ISS", "Weather", "Experimental")

    Scaffold(
        modifier = Modifier
            .padding(bottom = paddingValues.calculateBottomPadding())
            .statusBarsPadding(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium)
                    .padding(top = Spacing.Medium)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.satellites1),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(Spacing.Small))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SATELLITES",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Track amateur and weather spacecraft",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF03DAC6)
                            )
                        }
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, null)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(top = Spacing.Small))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- FIXED MAP AT TOP ---
                SatelliteMap(
                    position = position,
                    selectedId = selectedId,
                    userLat = userLat,
                    userLon = userLon,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp) // Adjusted height slightly for better peeking
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // --- SCROLLABLE CONTENT BELOW ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp), // Extra space at bottom
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                // Search and Filters Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Toggle / Field
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSatelliteSearchQuery(it) },
                                placeholder = { Text("Search...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        isSearchActive = false
                                        viewModel.updateSatelliteSearchQuery("")
                                    }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        } else {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Next Pass Card
                val nextPass = passes.firstOrNull { it.startTime > System.currentTimeMillis() / 1000 }
                if (nextPass != null) {
                    item {
                        NextPassCard(nextPass)
                    }
                }

                // Telemetry
                item { SectionHeader("Live Telemetry") }
                item { TelemetryGrid(position) }

                // Communications
                val selectedMetadata = available.find { it.id == selectedId }
                if (selectedMetadata?.commInfo != null) {
                    item { SectionHeader("Communications") }
                    item { CommunicationCard(selectedMetadata.commInfo, position?.rangeRate ?: 0.0) }
                }

                // Next Passes
                if (passes.isNotEmpty()) {
                    item { SectionHeader("Upcoming Passes") }
                    items(passes.take(3)) { pass ->
                        PassListItem(pass)
                    }
                }

                // List Selection
                item { SectionHeader("Available Satellites") }
                val filteredList = available.filter { 
                    (selectedCategory == "All" || it.category == selectedCategory) &&
                    (it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery))
                }
                items(filteredList) { sat ->
                    SatelliteListItem(
                        metadata = sat,
                        isSelected = selectedId == sat.id,
                        isFavorite = favorites.contains(sat.id),
                        isVisible = position?.isVisible == true && selectedId == sat.id,
                        onSelect = { viewModel.selectSatellite(sat.id) },
                        onToggleFavorite = { viewModel.toggleFavoriteSatellite(sat.id) }
                    )
                }
            }
        }

        // Bottom Gradient Fade for scroll indication
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            thickness = 1.dp
        )
    }
}

@Composable
fun NextPassCard(pass: SatellitePass) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    
    LaunchedEffect(pass) {
        while (true) {
            currentTime = System.currentTimeMillis() / 1000
            kotlinx.coroutines.delay(1000)
        }
    }

    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val countdown = (pass.startTime - currentTime).coerceAtLeast(0)
    val isInProgress = currentTime >= pass.startTime && currentTime <= pass.endTime
    val losCountdown = (pass.endTime - currentTime).coerceAtLeast(0)
    
    val mins = if (isInProgress) losCountdown / 60 else countdown / 60
    val secs = if (isInProgress) losCountdown % 60 else countdown % 60

    // Quality Calculation (Scale 0-1)
    val elevationFactor = (pass.maxElevation / 90.0).coerceIn(0.0, 1.0) * 0.5
    val durationFactor = (pass.duration / 900.0).coerceIn(0.0, 1.0) * 0.3
    val sunlitFactor = (if (pass.isDaylight) 1.0 else 0.5) * 0.1
    val qualityScore = (elevationFactor + durationFactor + sunlitFactor + 0.1).coerceIn(0.0, 1.0)
    val qualityPercent = (qualityScore * 100).toInt()
    val qualityColor = when {
        qualityScore > 0.7 -> Color(0xFF4CAF50)
        qualityScore > 0.4 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    val qualityText = when {
        qualityScore > 0.8 -> "Excellent for all antennas"
        qualityScore > 0.6 -> "Good for handheld"
        qualityScore > 0.4 -> "Fair - High elevation needed"
        else -> "Poor - Near horizon"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInProgress) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Sat Name + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pass.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = qualityColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = pass.quality.uppercase().ifEmpty { "FAIR" },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                
                Text(
                    if (isInProgress) "LIVE" else "NEXT PASS",
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isInProgress) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(8.dp))

            // Large Countdown Focus
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format(Locale.US, "%02dm %02ds", mins, secs),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isInProgress) "UNTIL LOSS OF SIGNAL" else "UNTIL ACQUISITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Next pass: ${pass.quality} visibility • Max elevation ${pass.maxElevation.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Pass Quality Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PASS QUALITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = (if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.5f)
                    )
                    Text(
                        "$qualityPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = qualityColor
                    )
                }
                
                // Visual Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background((if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(qualityScore.toFloat())
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(qualityColor)
                    )
                }
                
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp), 
                color = (if (isInProgress) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.1f)
            )
            
            // Four Columns with Icons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PassInfoItem("AOS", sdf.format(Date(pass.startTime * 1000)), Icons.Default.Launch)
                PassInfoItem("LOS", sdf.format(Date(pass.endTime * 1000)), Icons.Default.VerticalAlignBottom)
                PassInfoItem("DUR", "${pass.duration / 60}m", Icons.Default.Schedule)
                PassInfoItem("MAX EL", "${pass.maxElevation.toInt()}°", Icons.AutoMirrored.Filled.TrendingUp)
            }
        }
    }
}

@Composable
fun PassInfoItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Black)
    }
}

@Composable
fun TelemetryGrid(position: SatellitePosition?) {
    val pos = position
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryCard(Modifier.weight(1f), "Altitude", if (pos != null && !pos.altitude.isNaN()) "${pos.altitude.toInt()} km" else "---", Icons.Default.Height)
            TelemetryCard(Modifier.weight(1f), "Velocity", if (pos != null && !pos.velocity.isNaN()) "${String.format(Locale.US, "%,d", pos.velocity.toInt())} km/h" else "---", Icons.Default.Speed)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryCard(Modifier.weight(1f), "Azimuth", if (pos != null && !pos.azimuth.isNaN()) "${String.format(Locale.US, "%.1f", pos.azimuth)}°" else "---", Icons.Default.Explore)
            TelemetryCard(Modifier.weight(1f), "Elevation", if (pos != null && !pos.elevation.isNaN()) "${String.format(Locale.US, "%.1f", pos.elevation)}°" else "---", Icons.Default.VerticalAlignTop)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryCard(Modifier.weight(1f), "Lat", if (pos != null && !pos.latitude.isNaN()) String.format(Locale.US, "%.4f", pos.latitude) else "---", Icons.Default.LocationOn)
            TelemetryCard(Modifier.weight(1f), "Lon", if (pos != null && !pos.longitude.isNaN()) String.format(Locale.US, "%.4f", pos.longitude) else "---", Icons.Default.LocationOn)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryCard(Modifier.weight(1f), "Dist", if (pos != null && !pos.distance.isNaN()) "${pos.distance.toInt()} km" else "---", Icons.Default.Straighten)
            TelemetryCard(Modifier.weight(1f), "Grid", pos?.gridLocator?.takeIf { it.isNotEmpty() } ?: "---", Icons.Default.GridOn)
        }
    }
}

@Composable
fun TelemetryCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun CommunicationCard(info: SatelliteCommInfo, rangeRate: Double = 0.0) {
    val c = 299792.458 // Speed of light in km/s
    
    fun calculateDoppler(freqStr: String, isUplink: Boolean): String {
        val freq = freqStr.toDoubleOrNull() ?: return freqStr
        // Downlink: f_ground = f_sat * (1 - vr/c)
        // Uplink: f_tx = f_sat * (1 + vr/c) -> To compensate so sat receives nominal
        val factor = if (isUplink) (1.0 + (rangeRate / c)) else (1.0 - (rangeRate / c))
        val corrected = freq * factor
        return String.format(Locale.US, "%.4f", corrected)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CommItem("Downlink", "${info.downlink} MHz", Modifier.weight(1f), calculateDoppler(info.downlink, false))
                CommItem("Uplink", "${info.uplink} MHz", Modifier.weight(1f), calculateDoppler(info.uplink, true))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CommItem("Mode", info.mode, Modifier.weight(1f))
                CommItem("Tone/Pol", "${info.plTone} / ${info.polarization}", Modifier.weight(1f))
            }
            
            if (rangeRate != 0.0) {
                val shiftKhz = (rangeRate / c) * 145.8 * 1000.0
                Text(
                    text = "Live Doppler Correction Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Relative Velocity: ${String.format(Locale.US, "%.2f", rangeRate)} km/s • Shift: ${String.format(Locale.US, "%.2f", shiftKhz)} kHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CommItem(label: String, value: String, modifier: Modifier, subValue: String? = null) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (subValue != null && subValue != value.replace(" MHz", "")) {
            Text(
                text = "$subValue MHz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SatelliteListItem(
    metadata: au.com.benji.robert.repository.SatelliteMetadata,
    isSelected: Boolean,
    isFavorite: Boolean,
    isVisible: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onSelect() },
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isVisible) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SatelliteAlt, 
                    contentDescription = null, 
                    tint = if (isVisible) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = metadata.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (isVisible) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "LIVE", 
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(text = metadata.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PassListItem(pass: SatellitePass) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text(text = sdf.format(Date(pass.startTime * 1000)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                Text(text = "${pass.maxElevation.toInt()}°", style = MaterialTheme.typography.bodySmall)
            }
            
            Text(
                text = "${pass.duration / 60}m",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
