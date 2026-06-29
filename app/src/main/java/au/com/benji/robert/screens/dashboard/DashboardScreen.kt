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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val dxSpots by viewModel.dxSpots.collectAsStateWithLifecycle()
    val isRefreshingDx by viewModel.isRefreshingDx.collectAsStateWithLifecycle()
    val recommendation by viewModel.recommendation.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showAddLogDialog by remember { mutableStateOf(false) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var showManageLogsDialog by remember { mutableStateOf(false) }
    var showManageShackDialog by remember { mutableStateOf(false) }
    var showManageDxDialog by remember { mutableStateOf(false) }
    
    var selectedShackItem by remember { mutableStateOf<ShackEntity?>(null) }
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
                // --- HERO BANNER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = "https://i.imgur.com/HumKlMe.png",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_menu_gallery),
                        placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = Spacing.Medium)
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RobertTopBar()
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    // --- PRO OPERATOR CARD ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.Large),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "OPERATOR'S INSIGHT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = recommendation,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // --- LIVE SPACE WEATHER ---
                    var solarExpanded by remember { mutableStateOf(true) }
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { solarExpanded = !solarExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DashboardSectionTitle("Space Weather")
                                Icon(
                                    if (solarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "HamQSL Real-time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        AnimatedVisibility(visible = solarExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Solar Flux",
                                        value = solarData.solarFlux.toString(),
                                        icon = Icons.Default.WbSunny
                                    )
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Sunspots",
                                        value = solarData.sunspots.toString(),
                                        icon = Icons.Default.BrightnessLow
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "K-Index",
                                        value = solarData.kIndex.toString(),
                                        icon = Icons.Default.Public
                                    )
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "A-Index",
                                        value = solarData.aIndex.toString(),
                                        icon = Icons.AutoMirrored.Filled.TrendingUp
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "MUF",
                                        value = solarData.muf.replace(" MHz", ""),
                                        unit = "MHz",
                                        icon = Icons.Default.Wifi
                                    )
                                    MetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = "X-Ray",
                                        value = solarData.xRay,
                                        icon = Icons.Default.Thunderstorm
                                    )
                                }
                            }
                        }
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
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        DashboardSectionTitle("Command Center")
                        
                        val controlActions = listOf(
                            ControlAction("Propagation", Icons.Default.SignalCellularAlt, { navController.navigate(Screen.Propagation.route) }),
                            ControlAction("DX Spots", Icons.Default.Radar, { showManageDxDialog = true }),
                            ControlAction("Logbook", Icons.Default.EditNote, { showManageLogsDialog = true }),
                            ControlAction("The Shack", Icons.Default.HomeWork, { showManageShackDialog = true }),
                            ControlAction("SDR Control", Icons.Default.Radio, { navController.navigate(Screen.Sdr.route) }),
                            ControlAction("APRS Map", Icons.Default.LocationOn, { navController.navigate(Screen.Aprs.route) }),
                            ControlAction("Satellites", Icons.Default.Explore, { navController.navigate(Screen.Satellites.route) }),
                            ControlAction("Radio Tools", Icons.Default.Construction, { navController.navigate(Screen.Tools.route) }),
                            ControlAction("Settings", Icons.Default.Settings, { navController.navigate(Screen.Settings.route) })
                        )

                        for (rowActions in controlActions.chunked(3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                            ) {
                                for (action in rowActions) {
                                    QuickActionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = action.icon,
                                        title = action.title,
                                        onClick = action.onClick
                                    )
                                }
                                repeat(3 - rowActions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
                                    DxSpotItem(spot)
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
                                LogEntryItem(entry = entry, onDelete = { logToDelete = entry })
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
        AddLogDialog(
            onDismiss = { showAddLogDialog = false },
            onConfirm = { callsign, freq, band, mode, notes ->
                viewModel.addLog(callsign, freq, band, mode, notes)
                showAddLogDialog = false
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

data class ControlAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DxSpotItem(spot: DxSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
fun LogEntryItem(
    entry: LogEntryEntity,
    onDelete: () -> Unit
) {
    val zuluFormat = remember { SimpleDateFormat("HH:mm'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val localFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(text = entry.callsign.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
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
                    Text(
                        text = "${entry.band} • ${entry.mode} • ${entry.frequency} MHz", 
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var callsign by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var band by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New QSO", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                RobertTextField(value = callsign, onValueChange = { callsign = it }, label = "Callsign")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { RobertTextField(value = frequency, onValueChange = { frequency = it }, label = "Freq (MHz)") }
                    Box(modifier = Modifier.weight(1f)) { RobertTextField(value = band, onValueChange = { band = it }, label = "Band") }
                }
                RobertTextField(value = mode, onValueChange = { mode = it }, label = "Mode (FT8, SSB...)")
                RobertTextField(value = notes, onValueChange = { notes = it }, label = "Notes")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(callsign, frequency, band, mode, notes) }, enabled = callsign.isNotBlank()) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
