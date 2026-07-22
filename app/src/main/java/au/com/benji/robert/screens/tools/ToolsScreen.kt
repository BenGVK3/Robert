package au.com.benji.robert.screens.tools

import au.com.benji.robert.utils.calculateMaidenhead
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.R
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.components.RobertMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    navController: androidx.navigation.NavController,
    paddingValues: PaddingValues,
    initialTool: String? = null,
    viewModel: DashboardViewModel = viewModel()
) {
    var activeTool by remember { mutableStateOf<String?>(initialTool) }
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            val title = activeTool ?: "Radio Tools"
            val iconRes = when (activeTool) {
                "Maidenhead Locator" -> R.drawable.maidenhead1
                "Antenna Calculator" -> R.drawable.antcalc1
                "Prefix Map" -> R.drawable.prefixmap1
                "Callsign Lookup" -> R.drawable.cslookup1
                "Band Plan" -> R.drawable.bandplan1
                "Club Nets" -> R.drawable.nets1
                "Glossary" -> R.drawable.glossary1
                else -> R.drawable.tools1
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (activeTool != null) {
                        IconButton(
                            onClick = { activeTool = null },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(if (activeTool == null) 48.dp else 40.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            style = if (activeTool == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()
        ) {
            if (activeTool == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = paddingValues.calculateBottomPadding() + 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false // Prevent scrolling to keep it on one page
                ) {
                    val tools = listOf(
                        "Maidenhead Locator" to R.drawable.maidenhead1,
                        "Antenna Calculator" to R.drawable.antcalc1,
                        "Prefix Map" to R.drawable.prefixmap1,
                        "Callsign Lookup" to R.drawable.cslookup1,
                        "Band Plan" to R.drawable.bandplan1,
                        "Club Nets" to R.drawable.nets1,
                        "Glossary" to R.drawable.glossary1
                    )
                    
                    items(tools) { (title, image) ->
                        ToolGridCard(
                            title = title,
                            imageRes = image,
                            onClick = { activeTool = title }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTool) {
                        "Maidenhead Locator" -> Box(Modifier.padding(Spacing.Medium)) { 
                            Column(Modifier.fillMaxSize()) {
                                MaidenheadTool(viewModel)
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                            }
                        }
                        "Antenna Calculator" -> Box(Modifier.padding(Spacing.Medium)) { 
                            Column(Modifier.fillMaxSize()) {
                                AntennaCalculatorScreen()
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                            }
                        }
                        "Prefix Map" -> Box(Modifier.padding(Spacing.Medium)) { 
                            PrefixMapTool(paddingValues = paddingValues)
                        }
                        "Callsign Lookup" -> Box(Modifier.padding(Spacing.Medium)) { 
                            Column(Modifier.fillMaxSize()) {
                                CallsignLookupTool()
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                            }
                        }
                        "Band Plan" -> BandPlanScreen(
                            onBack = { activeTool = null },
                            paddingValues = paddingValues
                        )
                        "Club Nets" -> Box(Modifier.padding(Spacing.Medium)) { 
                            Column(Modifier.fillMaxSize()) {
                                ClubNetsScreen()
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                            }
                        }
                        "Glossary" -> GlossaryScreen(paddingValues = paddingValues)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolGridCard(title: String, imageRes: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MaidenheadTool(viewModel: DashboardViewModel) {
    val location by viewModel.locationFlow.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    
    val lat = location?.first ?: -37.8136
    val lon = location?.second ?: 144.9631
    val grid = calculateMaidenhead(lat, lon)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        // Top Card: Primary Grid Square
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = Spacing.Large, horizontal = Spacing.Medium), 
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CURRENT GRID SQUARE", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = grid, 
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp), 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(grid)) },
                    modifier = Modifier.padding(top = Spacing.Small),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY GRID", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Location Intel Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "LOCATION INTEL", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.primary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Latitude", style = MaterialTheme.typography.bodyMedium)
                    Text(String.format(java.util.Locale.US, "%.4f°", lat), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Longitude", style = MaterialTheme.typography.bodyMedium)
                    Text(String.format(java.util.Locale.US, "%.4f°", lon), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (location != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (location != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (location != null) "GPS Active & Synchronized" else "Using Last Known Location",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (location != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Live Map Card (Replaced About section)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val mapUrl = "https://levinecentral.com/ham/grid_square.php?Grid=$grid"
                RobertMap(
                    url = mapUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.Large))
    }
}

@Composable
fun CallsignLookupTool() {
    var callsign by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    text = "QRZ.com CALLSIGN SEARCH",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it },
                    label = { Text("Enter Callsign (e.g. VK3ESE)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null) }
                )
                
                Button(
                    onClick = { 
                        if (callsign.isNotBlank()) {
                            uriHandler.openUri("https://www.qrz.com/db/${callsign.trim().uppercase()}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = callsign.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEARCH ON QRZ.COM")
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))
        
        Text(
            text = "This will open the operator's profile in your default web browser.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PrefixMapTool(paddingValues: PaddingValues = PaddingValues()) {
    var query by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<PrefixRegion?>(null) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var mapScale by remember { mutableStateOf(1f) }
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Prefix (e.g. VK3, W4)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            Button(
                onClick = {
                    val result = findRegionForPrefix(query)
                    if (result != null) {
                        selectedRegion = result.region
                        mapOffset = result.offset
                        mapScale = result.scale
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("GO")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Small))

        if (selectedRegion == null) {
            // Zone Selection Grid
            Text("Select a region to view map:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = Spacing.Small))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                PrefixRegion.entries.sortedBy { it.displayName }.chunked(2).forEach { rowRegions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        rowRegions.forEach { region ->
                            Card(
                                onClick = { 
                                    selectedRegion = region
                                    mapOffset = Offset.Zero
                                    mapScale = 1f
                                },
                                modifier = Modifier.weight(1f).height(100.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    Text(region.displayName, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        if (rowRegions.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
            }
        } else {
            // Map View
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedRegion = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BACK TO ZONES")
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = null)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isExpanded) 20f else 1f)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                mapScale = (mapScale * zoom).coerceIn(1f, 15f)
                                mapOffset += pan
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = selectedRegion!!.resId),
                        contentDescription = selectedRegion!!.displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = mapScale,
                                scaleY = mapScale,
                                translationX = mapOffset.x,
                                translationY = mapOffset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Reset Button
                    IconButton(
                        onClick = { mapScale = 1f; mapOffset = Offset.Zero },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.Small).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                    }
                }
            }
            
            if (!isExpanded) {
                Text(
                    "Pinch to zoom, drag to pan. Map: ${selectedRegion!!.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = Spacing.Small)
                )
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

enum class PrefixRegion(val displayName: String, val resId: Int) {
    WORLD_CQ("CQ Zones (World)", R.drawable.cqzas),
    NORTH_AMERICA("North America", R.drawable.north_america),
    USA_EAST("USA East (W1-4, 8, 9)", R.drawable.usa_east),
    USA_WEST("USA West (W0, 5, 6, 7)", R.drawable.usa_west),
    CANADA_EAST("Canada East", R.drawable.canada_east),
    CANADA_WEST("Canada West", R.drawable.canada_west),
    CARIBBEAN_WEST("Caribbean West", R.drawable.caribbean_west),
    CARIBBEAN_EAST("Caribbean East", R.drawable.caribbean_east),
    SOUTH_AMERICA("South America", R.drawable.south_america),
    SOUTH_AMERICA_EAST("South America East", R.drawable.south_america_east),
    SOUTH_AMERICA_WEST("South America West", R.drawable.south_america_west),
    SOUTH_AMERICA_SOUTH("South America South", R.drawable.south_america_south),
    EUROPE("Europe (All)", R.drawable.europe),
    WEST_EUROPE("Western Europe", R.drawable.western_europe),
    EAST_EUROPE("Eastern Europe", R.drawable.eastern_europe),
    SCANDINAVIA("Scandinavia", R.drawable.scandinavia),
    MED_WEST("Mediterranean West", R.drawable.mediterranean_west),
    MED_EAST("Mediterranean East", R.drawable.mediterranean_east),
    AFRICA("Africa (All)", R.drawable.africa),
    AFRICA_SOUTH("Southern Africa", R.drawable.southern_africa),
    AFRICA_EAST("East Africa", R.drawable.east_africa),
    AFRICA_WEST("West Africa", R.drawable.west_africa),
    RUSSIA("Russia (All)", R.drawable.russia),
    RUSSIA_WEST("Russia West", R.drawable.russia_west),
    RUSSIA_CENTRAL("Russia Central", R.drawable.russia_central),
    RUSSIA_EAST("Russia East", R.drawable.russia_east),
    MIDDLE_EAST("Middle East", R.drawable.middle_east),
    ASIA("Asia (All)", R.drawable.asia),
    ASIA_EAST("East Asia (JA, HL, BY)", R.drawable.asia_east),
    ASIA_SOUTH("South Asia", R.drawable.south_asia),
    ASIA_SOUTH_EAST("South-East Asia", R.drawable.south_east_asia),
    CHINA("China", R.drawable.china),
    OCEANIA("Oceania (All)", R.drawable.oceania),
    OCEANIA_SOUTH("Australia / NZ", R.drawable.oceania_south),
    OCEANIA_NORTH("Pacific North", R.drawable.oceania_north),
    OCEANIA_EAST("Pacific East", R.drawable.oceania_east),
    ANTARCTICA("Antarctica", R.drawable.antarctica)
}

data class SearchResult(val region: PrefixRegion, val offset: Offset, val scale: Float)

fun findRegionForPrefix(query: String): SearchResult? {
    if (query.isBlank()) return null
    val q = query.uppercase().trim()
    
    // Detailed matching based on official ITU prefix allocations and specific region maps
    return when {
        // Australia / NZ / Oceania
        q.startsWith("VK1") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(150f, 300f), 6f)
        q.startsWith("VK2") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(150f, 250f), 6f)
        q.startsWith("VK3") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(150f, 450f), 7f)
        q.startsWith("VK4") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(100f, 0f), 5f)
        q.startsWith("VK5") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(-50f, 250f), 6f)
        q.startsWith("VK6") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(-450f, 200f), 5f)
        q.startsWith("VK7") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(150f, 650f), 8f)
        q.startsWith("VK8") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(-100f, -100f), 5f)
        q.startsWith("VK") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(0f, 0f), 2f)
        q.startsWith("ZL") -> SearchResult(PrefixRegion.OCEANIA_SOUTH, Offset(600f, 700f), 5f)
        
        // Pacific North / East
        q.startsWith("KH6") || q.startsWith("WH6") -> SearchResult(PrefixRegion.OCEANIA_NORTH, Offset(0f, 0f), 3f)
        q.startsWith("KH") || q.startsWith("NH") -> SearchResult(PrefixRegion.OCEANIA_NORTH, Offset(0f, 0f), 2f)
        q.startsWith("FK") -> SearchResult(PrefixRegion.OCEANIA_EAST, Offset(0f, 0f), 3f)
        
        // USA
        q.startsWith("W1") || q.startsWith("K1") || q.startsWith("N1") || q.startsWith("A1") -> SearchResult(PrefixRegion.USA_EAST, Offset(500f, -300f), 7f)
        q.startsWith("W2") || q.startsWith("K2") || q.startsWith("N2") || q.startsWith("A2") -> SearchResult(PrefixRegion.USA_EAST, Offset(400f, -200f), 7f)
        q.startsWith("W3") || q.startsWith("K3") || q.startsWith("N3") || q.startsWith("A3") -> SearchResult(PrefixRegion.USA_EAST, Offset(350f, -100f), 7f)
        q.startsWith("W4") || q.startsWith("K4") || q.startsWith("N4") || q.startsWith("A4") -> SearchResult(PrefixRegion.USA_EAST, Offset(100f, 300f), 5f)
        q.startsWith("W8") || q.startsWith("K8") || q.startsWith("N8") || q.startsWith("A8") -> SearchResult(PrefixRegion.USA_EAST, Offset(150f, -300f), 7f)
        q.startsWith("W9") || q.startsWith("K9") || q.startsWith("N9") || q.startsWith("A9") -> SearchResult(PrefixRegion.USA_EAST, Offset(-100f, -200f), 7f)
        
        q.startsWith("W5") || q.startsWith("K5") || q.startsWith("N5") -> SearchResult(PrefixRegion.USA_WEST, Offset(400f, 300f), 5f)
        q.startsWith("W6") || q.startsWith("K6") || q.startsWith("N6") -> SearchResult(PrefixRegion.USA_WEST, Offset(-500f, 300f), 5f)
        q.startsWith("W7") || q.startsWith("K7") || q.startsWith("N7") -> SearchResult(PrefixRegion.USA_WEST, Offset(-400f, -200f), 5f)
        q.startsWith("W0") || q.startsWith("K0") || q.startsWith("N0") -> SearchResult(PrefixRegion.USA_WEST, Offset(400f, -200f), 5f)
        
        // Canada
        q.startsWith("VE1") || q.startsWith("VE2") || q.startsWith("VE3") || q.startsWith("VO") -> SearchResult(PrefixRegion.CANADA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("VE") || q.startsWith("VY") || q.startsWith("CY") -> SearchResult(PrefixRegion.CANADA_WEST, Offset(0f, 0f), 2f)
        
        // Caribbean
        q.startsWith("KP4") || q.startsWith("NP4") || q.startsWith("WP4") -> SearchResult(PrefixRegion.CARIBBEAN_EAST, Offset(0f, 0f), 3f)
        q.startsWith("CO") || q.startsWith("CM") -> SearchResult(PrefixRegion.CARIBBEAN_WEST, Offset(0f, 0f), 3f)
        
        // South America
        q.startsWith("PY") || q.startsWith("PP") || q.startsWith("PT") || q.startsWith("PU") -> SearchResult(PrefixRegion.SOUTH_AMERICA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("LU") || q.startsWith("LW") || q.startsWith("AY") -> SearchResult(PrefixRegion.SOUTH_AMERICA_SOUTH, Offset(0f, 0f), 2f)
        q.startsWith("CE") || q.startsWith("CA") || q.startsWith("CB") -> SearchResult(PrefixRegion.SOUTH_AMERICA_WEST, Offset(0f, 0f), 2f)
        q.startsWith("YV") || q.startsWith("YY") || q.startsWith("YX") || q.startsWith("HK") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(0f, -500f), 3f)
        
        // Europe
        q.startsWith("G") || q.startsWith("M") || q.startsWith("2") || q.startsWith("F") || q.startsWith("DL") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(0f, 0f), 2f)
        q.startsWith("I") || q.startsWith("IS0") -> SearchResult(PrefixRegion.MED_WEST, Offset(0f, 0f), 3f)
        q.startsWith("SV") || q.startsWith("SY") || q.startsWith("LZ") || q.startsWith("YO") -> SearchResult(PrefixRegion.EAST_EUROPE, Offset(0f, 0f), 2f)
        q.startsWith("SV") || q.startsWith("5B") || q.startsWith("TA") -> SearchResult(PrefixRegion.MED_EAST, Offset(0f, 0f), 3f)
        
        // Scandinavia
        q.startsWith("LA") || q.startsWith("SM") || q.startsWith("OH") || q.startsWith("OZ") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(0f, 0f), 2f)
        
        // Russia
        q.startsWith("UA9") || q.startsWith("RA9") || q.startsWith("RZ9") -> SearchResult(PrefixRegion.RUSSIA_CENTRAL, Offset(0f, 0f), 2f)
        q.startsWith("UA0") || q.startsWith("RA0") || q.startsWith("RZ0") -> SearchResult(PrefixRegion.RUSSIA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("UA") || q.startsWith("RA") || q.startsWith("RZ") -> SearchResult(PrefixRegion.RUSSIA_WEST, Offset(0f, 0f), 2f)
        
        // China / Asia
        q.startsWith("BY") || q.startsWith("BT") || q.startsWith("BV") -> SearchResult(PrefixRegion.CHINA, Offset(0f, 0f), 2f)
        q.startsWith("JA") || q.startsWith("JH") || q.startsWith("JR") -> SearchResult(PrefixRegion.ASIA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("VU") || q.startsWith("4S") || q.startsWith("9N") -> SearchResult(PrefixRegion.ASIA_SOUTH, Offset(0f, 0f), 2f)
        q.startsWith("HS") || q.startsWith("9V") || q.startsWith("YB") -> SearchResult(PrefixRegion.ASIA_SOUTH_EAST, Offset(0f, 0f), 2f)
        
        // Africa
        q.startsWith("ZS") || q.startsWith("ZR") || q.startsWith("ZT") -> SearchResult(PrefixRegion.AFRICA_SOUTH, Offset(0f, 0f), 2f)
        q.startsWith("5Z") || q.startsWith("ET") || q.startsWith("ST") -> SearchResult(PrefixRegion.AFRICA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("EL") || q.startsWith("TU") || q.startsWith("9G") -> SearchResult(PrefixRegion.AFRICA_WEST, Offset(0f, 0f), 2f)
        
        // Middle East
        q.startsWith("HZ") || q.startsWith("7Z") || q.startsWith("4X") || q.startsWith("JY") -> SearchResult(PrefixRegion.MIDDLE_EAST, Offset(0f, 0f), 3f)
        
        // Antarctica
        q.startsWith("KC4") || q.startsWith("8J1") || q.startsWith("DP0") || q.startsWith("RI1") -> SearchResult(PrefixRegion.ANTARCTICA, Offset(0f, 0f), 2f)

        else -> null
    }
}
