package au.com.benji.robert.screens.dashboard

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import au.com.benji.robert.components.*
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.models.*
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.theme.RobertColors
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val callsign by viewModel.callsign.collectAsStateWithLifecycle()
    val locationData by viewModel.locationFlow.collectAsStateWithLifecycle()
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val dxSpots by viewModel.dxSpots.collectAsStateWithLifecycle()
    val isRefreshingDx by viewModel.isRefreshingDx.collectAsStateWithLifecycle()
    val recommendation by viewModel.recommendation.collectAsStateWithLifecycle()
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showAddLogDialog by remember { mutableStateOf(false) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var showManageLogsDialog by remember { mutableStateOf(false) }
    var showManageShackDialog by remember { mutableStateOf(false) }
    var showManageDxDialog by remember { mutableStateOf(false) }
    
    var selectedShackItem by remember { mutableStateOf<ShackEntity?>(null) }
    var selectedDxSpot by remember { mutableStateOf<DxSpot?>(null) }
    var selectedLogEntry by remember { mutableStateOf<LogEntryEntity?>(null) }
    var logToDelete by remember { mutableStateOf<LogEntryEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ShackEntity?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    val dxPullToRefreshState = rememberPullToRefreshState()

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            viewModel.refresh()
        } else if (!locationPermissionState.shouldShowRationale) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                // --- MODERN HERO HEADER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = au.com.benji.robert.R.drawable.robertheader),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Dark overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(top = Spacing.Medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "R.O.B.E.R.T",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Text(
                            text = "Radio Operator's Band Exploration & Resource Tool",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.Large))
                        
                        // Status Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeaderBadge(text = callsign, icon = Icons.Default.Person)
                            HeaderBadge(text = locationData?.fourth ?: "---", icon = Icons.Default.LocationOn)
                            HeaderBadge(
                                text = "OPERATIONAL", 
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    // --- PREMIUM OPERATOR INSIGHT ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                            ) {
                                Icon(
                                    Icons.Default.Insights, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "OPERATOR'S INSIGHT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // --- COMPACT SPACE WEATHER ---
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            DashboardSectionTitle("Space Weather")
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                                // Important Values Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ImportantMetric(
                                        modifier = Modifier.weight(1.2f),
                                        title = "Solar Flux",
                                        value = solarData.solarFlux.toString(),
                                        icon = Icons.Default.WbSunny
                                    )
                                    ImportantMetric(
                                        modifier = Modifier.weight(1f),
                                        title = "K-Index",
                                        value = solarData.kIndex.toString(),
                                        icon = Icons.Default.Public
                                    )
                                    ImportantMetric(
                                        modifier = Modifier.weight(1.2f),
                                        title = "MUF",
                                        value = solarData.muf.replace(" MHz", ""),
                                        unit = "MHz",
                                        icon = Icons.Default.Wifi
                                    )
                                }
                                
                                // Secondary Values Grid
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactMetric(modifier = Modifier.weight(1f), label = "Sunspots", value = solarData.sunspots.toString())
                                    CompactMetric(modifier = Modifier.weight(1f), label = "A-Index", value = solarData.aIndex.toString())
                                    CompactMetric(modifier = Modifier.weight(1f), label = "X-Ray", value = solarData.xRay)
                                }
                            }
                        }
                        
                        // Propagation Summary
                        val summary = remember(propagationData) {
                            val goodBands = propagationData?.bands?.filter { it.rating == "Excellent" || it.rating == "Good" } ?: emptyList()
                            if (goodBands.isEmpty()) "Conditions stable across most bands"
                            else "${goodBands.take(2).joinToString(" and ") { it.band }} currently active"
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = Spacing.Small)
                        )
                    }

                    // --- LOCAL STATION WEATHER ---
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        DashboardSectionTitle("Local Weather: ${weatherData?.locationName ?: "Locating..."}")
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.Large)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${weatherData?.temperature ?: "--"}${weatherData?.unit ?: "°C"}",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = weatherData?.condition?.uppercase() ?: "CHECKING...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(Spacing.Large))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(Spacing.Large))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WeatherDetailItem("Feels like", "${weatherData?.apparentTemperature ?: "--"}°", Icons.Default.Thermostat)
                                    WeatherDetailItem("Humidity", "${weatherData?.humidity ?: "--"}%", Icons.Default.WaterDrop)
                                    WeatherDetailItem("Wind", "${weatherData?.windSpeed ?: "--"} km/h", Icons.Default.Air)
                                }
                            }
                        }
                    }

                    // --- COMMAND CENTER ---
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        DashboardSectionTitle("Command Center")
                        
                        val controlActions = listOf(
                            ControlAction("Propagation", Icons.Default.SignalCellularAlt, { navController.navigate(Screen.Propagation.route) }),
                            ControlAction("DX Spots", Icons.Default.Radar, { showManageDxDialog = true }),
                            ControlAction("Logbook", Icons.Default.EditNote, { showManageLogsDialog = true }),
                            ControlAction("The Shack", Icons.Default.HomeWork, { showManageShackDialog = true }),
                            ControlAction("KiwiSDR", Icons.Default.Radio, { navController.navigate(Screen.Sdr.route) }),
                            ControlAction("APRS Map", Icons.Default.LocationOn, { navController.navigate(Screen.Aprs.route) }),
                            ControlAction("Repeaters", Icons.Default.CellTower, { navController.navigate(Screen.RepeaterList.route) }),
                            ControlAction("Satellites", Icons.Default.Explore, { navController.navigate(Screen.Satellites.route) }),
                            ControlAction("Radio Tools", Icons.Default.Construction, { navController.navigate(Screen.Tools.route) }),
                            ControlAction("Settings", Icons.Default.Settings, { navController.navigate(Screen.Settings.route) })
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            for (rowActions in controlActions.chunked(2)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                                ) {
                                    for (action in rowActions) {
                                        PremiumActionCard(
                                            modifier = Modifier.weight(1f),
                                            icon = action.icon,
                                            title = action.title,
                                            onClick = action.onClick
                                        )
                                    }
                                    if (rowActions.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                
                Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
            }
        }
    }

    // --- DIALOGS (DX, LOGS, SHACK) ---
    if (showManageDxDialog) {
        Dialog(
            onDismissRequest = { showManageDxDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live DX Spots", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showManageDxDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Text("POTA, SOTA and Global Clusters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    
                    if (dxSpots.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshingDx,
                            onRefresh = { viewModel.refreshDxSpots() },
                            state = dxPullToRefreshState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                            ) {
                                for (spot in dxSpots) {
                                    DxSpotItem(spot, onClick = { selectedDxSpot = spot })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManageLogsDialog) {
        Dialog(
            onDismissRequest = { showManageLogsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Logbook", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showManageLogsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    TextButton(onClick = { showAddLogDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOG NEW QSO")
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    
                    if (logs.isEmpty()) {
                        EmptySectionCard("Your logbook is empty.")
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            for (entry in logs) {
                                LogEntryItem(
                                    entry = entry, 
                                    onClick = { selectedLogEntry = entry },
                                    onDelete = { logToDelete = entry }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManageShackDialog) {
        Dialog(
            onDismissRequest = { showManageShackDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("The Shack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showManageShackDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    TextButton(onClick = { showAddEquipmentDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD GEAR")
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    
                    if (equipment.isEmpty()) {
                        EmptySectionCard("No gear in the shack.")
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            for (item in equipment) {
                                ShackItemCard(
                                    item = item, 
                                    onClick = { selectedShackItem = item },
                                    onDelete = { itemToDelete = item },
                                    onImageClick = { selectedImage = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADDING DIALOGS ---
    if (showAddLogDialog) {
        LogDialog(
            onDismiss = { showAddLogDialog = false },
            onConfirm = { entry ->
                viewModel.addLog(
                    callsign = entry.callsign,
                    name = entry.name,
                    qth = entry.qth,
                    frequency = entry.frequency,
                    band = entry.band,
                    mode = entry.mode,
                    rstSent = entry.rstSent,
                    rstReceived = entry.rstReceived,
                    power = entry.power,
                    timestamp = entry.timestamp,
                    notes = entry.notes,
                    sotaRef = entry.sotaRef,
                    potaRef = entry.potaRef,
                    wwffRef = entry.wwffRef,
                    hemaRef = entry.hemaRef,
                    siotaRef = entry.siotaRef,
                    vkShireRef = entry.vkShireRef
                )
                showAddLogDialog = false
            }
        )
    }

    selectedLogEntry?.let { entry ->
        LogDialog(
            existingEntry = entry,
            onDismiss = { selectedLogEntry = null },
            onConfirm = { updatedEntry ->
                viewModel.updateLog(updatedEntry)
                selectedLogEntry = null
            }
        )
    }

    if (showAddEquipmentDialog) {
        AddEquipmentDialog(
            onDismiss = { showAddEquipmentDialog = false },
            onConfirm = { category, man, model, nick, serial, notes, path ->
                viewModel.addEquipment(category, man, model, nick, serial, notes, path)
                showAddEquipmentDialog = false
            }
        )
    }

    selectedShackItem?.let { item ->
        ShackDetailDialog(
            item = item,
            onDismiss = { selectedShackItem = null },
            onImageClick = { selectedImage = it }
        )
    }

    selectedDxSpot?.let { spot ->
        DxSpotDetailDialog(
            spot = spot,
            onDismiss = { selectedDxSpot = null }
        )
    }

    if (selectedImage != null) {
        Dialog(
            onDismissRequest = { selectedImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = selectedImage,
                        contentDescription = "Full Size View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { selectedImage = null },
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
    
    // --- CONFIRMATION DIALOGS ---
    logToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Log Entry?") },
            text = { Text("Are you sure you want to delete the QSO with ${entry.callsign}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLog(entry)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Remove from Shack?") },
            text = { Text("Are you sure you want to remove your ${item.manufacturer} ${item.model}? This will delete all associated details and photos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEquipment(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HeaderBadge(
    text: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ImportantMetric(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 9.sp
        )
    }
}

@Composable
fun PremiumActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class ControlAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DxSpotItem(spot: DxSpot, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                Surface(
                    color = when(spot.source) {
                        SpotSource.POTA -> Color(0xFF4CAF50)
                        SpotSource.SOTA -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = spot.source.name.take(1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when(spot.source) {
                            SpotSource.POTA -> Color(0xFF4CAF50)
                            SpotSource.SOTA -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Text(text = spot.timeZulu, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "(${spot.timeLocal})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = spot.callsign, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${spot.frequency} MHz", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (spot.location.isNotEmpty()) {
                    Text(text = spot.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                if (spot.comment.isNotEmpty()) {
                    Text(text = spot.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = spot.mode, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(text = "de ${spot.spotter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptySectionCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(Spacing.Large), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShackItemCard(
    item: ShackEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            if (item.imagePath.isNotEmpty()) {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = item.model,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onImageClick(item.imagePath) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.category == "Radio") Icons.Default.Radio else Icons.Default.Hardware,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.Medium))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.manufacturer.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = item.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (item.nickname.isNotEmpty()) {
                    Text(text = item.nickname, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShackDetailDialog(
    item: ShackEntity,
    onDismiss: () -> Unit,
    onImageClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = item.manufacturer.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.model,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                if (item.imagePath.isNotEmpty()) {
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = item.model,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onImageClick(item.imagePath) },
                        contentScale = ContentScale.Crop
                    )
                }

                if (item.nickname.isNotEmpty()) {
                    Column {
                        Text(
                            text = "NICKNAME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.nickname,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (item.notes.isNotEmpty()) {
                    Column {
                        Text(
                            text = "NOTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun DxSpotDetailDialog(
    spot: DxSpot,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = spot.source.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (spot.source) {
                        SpotSource.POTA -> Color(0xFF4CAF50)
                        SpotSource.SOTA -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = spot.callsign,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    DetailInfoItem(label = "FREQUENCY", value = "${spot.frequency} MHz", modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "MODE", value = spot.mode.ifEmpty { "N/A" }, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    DetailInfoItem(label = "BAND", value = spot.band, modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "SPOTTER", value = spot.spotter, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    DetailInfoItem(label = "ZULU TIME", value = spot.timeZulu, modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "LOCAL TIME", value = spot.timeLocal, modifier = Modifier.weight(1f))
                }

                if (spot.location.isNotEmpty()) {
                    DetailInfoItem(label = "LOCATION", value = spot.location)
                }

                if (spot.comment.isNotEmpty()) {
                    DetailInfoItem(label = "COMMENT", value = spot.comment)
                }

                Spacer(modifier = Modifier.height(Spacing.Small))
                
                Button(
                    onClick = { uriHandler.openUri("https://www.qrz.com/db/${spot.callsign}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW QRZ PROFILE")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun DetailInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LogEntryItem(
    entry: LogEntryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val zuluFormat = remember { SimpleDateFormat("HH:mm'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val localFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = entry.callsign.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        IconButton(
                            onClick = { uriHandler.openUri("https://www.qrz.com/db/${entry.callsign}") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = "QRZ", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = zuluFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "(${localFormat.format(Date(entry.timestamp))})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val info = mutableListOf<String>()
                    info.add("${entry.band} • ${entry.mode}")
                    if (entry.frequency.isNotEmpty()) info.add("${entry.frequency} kHz")
                    if (entry.power.isNotEmpty()) info.add("${entry.power}W")
                    
                    Text(
                        text = info.joinToString(" • "), 
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (entry.rstSent.isNotEmpty() || entry.rstReceived.isNotEmpty()) {
                        Text(
                            text = "S:${entry.rstSent} R:${entry.rstReceived}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                if (entry.name.isNotEmpty() || entry.qth.isNotEmpty()) {
                    Text(
                        text = listOf(entry.name, entry.qth).filter { it.isNotEmpty() }.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String) -> Unit
) {
    var category by remember { mutableStateOf(EquipmentCategory.RADIO) }
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { imagePath = it.toString() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Gear to Shack", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        EquipmentCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
                RobertTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = "Manufacturer")
                RobertTextField(value = model, onValueChange = { model = it }, label = "Model")
                RobertTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname / Role")
                RobertTextField(value = notes, onValueChange = { notes = it }, label = "Notes")
                
                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imagePath.isEmpty()) "Add Photo" else "Change Photo")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(category.displayName, manufacturer, model, nickname, serialNumber, notes, imagePath) }, enabled = manufacturer.isNotBlank() && model.isNotBlank()) {
                Text("Add to Shack")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
