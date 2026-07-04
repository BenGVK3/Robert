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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.R
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    navController: androidx.navigation.NavController,
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    var activeTool by remember { mutableStateOf<String?>(null) }
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text(activeTool ?: "Radio Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (activeTool != null) {
                        IconButton(onClick = { activeTool = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
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
            if (activeTool == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ToolGridCard(
                            title = "Locator",
                            icon = Icons.Default.MyLocation,
                            onClick = { activeTool = "Maidenhead Locator" }
                        )
                    }
                    item {
                        ToolGridCard(
                            title = "Prefix Map",
                            icon = Icons.Default.Map,
                            onClick = { activeTool = "Prefix Map" }
                        )
                    }
                    item {
                        ToolGridCard(
                            title = "Lookup",
                            icon = Icons.Default.Search,
                            onClick = { activeTool = "Callsign Lookup" }
                        )
                    }
                    item {
                        ToolGridCard(
                            title = "Band Plan",
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            onClick = { activeTool = "Band Plan" }
                        )
                    }
                    item {
                        ToolGridCard(
                            title = "Glossary",
                            icon = Icons.Default.Book,
                            onClick = { activeTool = "Glossary" }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
                    when (activeTool) {
                        "Maidenhead Locator" -> MaidenheadTool(viewModel)
                        "Prefix Map" -> PrefixMapTool()
                        "Callsign Lookup" -> CallsignLookupTool()
                        "Band Plan" -> BandPlanScreen()
                        "Glossary" -> GlossaryScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun ToolGridCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.9f),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(Spacing.ExtraLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT GRID SQUARE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(text = grid, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(grid)) },
                    modifier = Modifier.padding(top = Spacing.Medium),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY GRID")
                }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Text("LOCATION DATA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Latitude", style = MaterialTheme.typography.bodyMedium)
                    Text(lat.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Longitude", style = MaterialTheme.typography.bodyMedium)
                    Text(lon.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))
                
                Text(
                    text = if (location != null) "✓ GPS active and accurate" else "⚠ Using last known location",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (location != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))
        Text(
            text = "Pull down to refresh GPS coordinates",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
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
fun PrefixMapTool() {
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
                PrefixRegion.entries.chunked(2).forEach { rowRegions ->
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
            }
        }
    }
}

enum class PrefixRegion(val displayName: String, val resId: Int) {
    OCEANIA_SOUTH("Australia / NZ", R.drawable.oceania_south),
    USA_EAST("USA East (W1-4, 8, 9)", R.drawable.usa_east),
    USA_WEST("USA West (W0, 5, 6, 7)", R.drawable.usa_west),
    CANADA_EAST("Canada East", R.drawable.canada_east),
    CANADA_WEST("Canada West", R.drawable.canada_west),
    CHINA("China", R.drawable.china),
    ASIA_EAST("East Asia (JA, HL, BY)", R.drawable.asia_east),
    ASIA_SOUTH_EAST("South-East Asia", R.drawable.south_east_asia),
    WEST_EUROPE("Western Europe", R.drawable.western_europe),
    EAST_EUROPE("Eastern Europe", R.drawable.eastern_europe),
    SCANDINAVIA("Scandinavia", R.drawable.scandinavia),
    RUSSIA_WEST("Russia West", R.drawable.russia_west),
    RUSSIA_CENTRAL("Russia Central", R.drawable.russia_central),
    RUSSIA_EAST("Russia East", R.drawable.russia_east),
    SOUTH_AMERICA("South America", R.drawable.south_america),
    CARIBBEAN("Caribbean", R.drawable.caribbean_west),
    AFRICA_NORTH("North Africa", R.drawable.africa),
    AFRICA_SOUTH("Southern Africa", R.drawable.southern_africa),
    MIDDLE_EAST("Middle East", R.drawable.middle_east),
    ANTARCTICA("Antarctica", R.drawable.antarctica)
}

data class SearchResult(val region: PrefixRegion, val offset: Offset, val scale: Float)

fun findRegionForPrefix(query: String): SearchResult? {
    if (query.isBlank()) return null
    val q = query.uppercase().trim()
    
    // Detailed matching based on official ITU prefix allocations and specific region maps
    return when {
        // Australia / NZ
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
        
        // China
        q.startsWith("BY") || q.startsWith("BT") || q.startsWith("BV") -> SearchResult(PrefixRegion.CHINA, Offset(0f, 0f), 2f)
        
        // Japan
        q.startsWith("JA") || q.startsWith("JH") || q.startsWith("JR") -> SearchResult(PrefixRegion.ASIA_EAST, Offset(500f, 300f), 6f)
        
        // UK & Europe
        q.startsWith("G") || q.startsWith("M") || q.startsWith("2") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(-300f, -400f), 6f)
        q.startsWith("F") || q.startsWith("TM") || q.startsWith("TK") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(-100f, 100f), 6f)
        q.startsWith("DL") || q.startsWith("DA") || q.startsWith("DF") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(300f, -200f), 6f)
        q.startsWith("EA") || q.startsWith("EB") || q.startsWith("ED") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(-400f, 600f), 5f)
        q.startsWith("CT") || q.startsWith("CU") || q.startsWith("CR") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(-600f, 500f), 6f)
        q.startsWith("PA") || q.startsWith("PI") || q.startsWith("PD") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(100f, -400f), 7f)
        q.startsWith("ON") || q.startsWith("OO") || q.startsWith("OR") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(100f, -300f), 7f)
        q.startsWith("HB") || q.startsWith("HE") -> SearchResult(PrefixRegion.WEST_EUROPE, Offset(200f, 0f), 7f)
        
        // Scandinavia
        q.startsWith("LA") || q.startsWith("LB") || q.startsWith("LI") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(-400f, -200f), 4f)
        q.startsWith("SM") || q.startsWith("SK") || q.startsWith("SL") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(-100f, 0f), 4f)
        q.startsWith("OH") || q.startsWith("OI") || q.startsWith("OF") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(300f, -100f), 4f)
        q.startsWith("OH") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(300f, -100f), 4f)
        q.startsWith("OZ") || q.startsWith("OU") || q.startsWith("OV") -> SearchResult(PrefixRegion.SCANDINAVIA, Offset(-200f, 400f), 5f)
        
        // Russia
        q.startsWith("UA9") || q.startsWith("RA9") || q.startsWith("RZ9") -> SearchResult(PrefixRegion.RUSSIA_CENTRAL, Offset(0f, 0f), 2f)
        q.startsWith("UA0") || q.startsWith("RA0") || q.startsWith("RZ0") -> SearchResult(PrefixRegion.RUSSIA_EAST, Offset(0f, 0f), 2f)
        q.startsWith("UA") || q.startsWith("RA") || q.startsWith("RZ") -> SearchResult(PrefixRegion.RUSSIA_WEST, Offset(0f, 0f), 2f)
        
        // Africa
        q.startsWith("ZS") || q.startsWith("ZR") || q.startsWith("ZT") -> SearchResult(PrefixRegion.AFRICA_SOUTH, Offset(0f, 0f), 2f)
        q.startsWith("D2") || q.startsWith("D3") -> SearchResult(PrefixRegion.AFRICA_SOUTH, Offset(-200f, -300f), 4f)
        q.startsWith("V5") -> SearchResult(PrefixRegion.AFRICA_SOUTH, Offset(-200f, 0f), 4f)
        
        // South America
        q.startsWith("PY") || q.startsWith("PP") || q.startsWith("PT") || q.startsWith("PU") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(200f, 0f), 2f)
        q.startsWith("LU") || q.startsWith("LW") || q.startsWith("AY") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(-100f, 500f), 3f)
        q.startsWith("CE") || q.startsWith("CA") || q.startsWith("CB") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(-300f, 400f), 3f)
        q.startsWith("YV") || q.startsWith("YY") || q.startsWith("YX") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(-100f, -500f), 4f)
        q.startsWith("HK") || q.startsWith("HJ") -> SearchResult(PrefixRegion.SOUTH_AMERICA, Offset(-300f, -500f), 4f)
        
        // Middle East
        q.startsWith("HZ") || q.startsWith("7Z") || q.startsWith("8Z") -> SearchResult(PrefixRegion.MIDDLE_EAST, Offset(0f, 0f), 3f)
        q.startsWith("YI") -> SearchResult(PrefixRegion.MIDDLE_EAST, Offset(0f, -300f), 5f)
        q.startsWith("EP") || q.startsWith("EQ") -> SearchResult(PrefixRegion.MIDDLE_EAST, Offset(400f, -300f), 4f)
        
        // Mexico
        q.startsWith("XE") || q.startsWith("XF") -> SearchResult(PrefixRegion.USA_WEST, Offset(0f, 600f), 3f)

        else -> null
    }
}
