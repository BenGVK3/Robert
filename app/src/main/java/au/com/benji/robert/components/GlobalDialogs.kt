package au.com.benji.robert.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.models.*
import au.com.benji.robert.screens.dashboard.*
import au.com.benji.robert.theme.Spacing
import coil.compose.AsyncImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDialogs(
    viewModel: DashboardViewModel,
    showDxSpots: Boolean,
    onDismissDxSpots: () -> Unit,
    showShack: Boolean,
    onDismissShack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val dxSpots by viewModel.dxSpots.collectAsStateWithLifecycle()
    val isRefreshingDx by viewModel.isRefreshingDx.collectAsStateWithLifecycle()
    val dxPullToRefreshState = rememberPullToRefreshState()

    val activeBand by viewModel.dxBandFilter.collectAsStateWithLifecycle()
    val activeMode by viewModel.dxModeFilter.collectAsStateWithLifecycle()
    val activeContinent by viewModel.dxContinentFilter.collectAsStateWithLifecycle()
    val activeTime by viewModel.dxTimeFilter.collectAsStateWithLifecycle()
    
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    
    var selectedDxSpot by remember { mutableStateOf<DxSpot?>(null) }
    var selectedShackItem by remember { mutableStateOf<ShackEntity?>(null) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShackEntity?>(null) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    
    var showBandMenu by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showContinentMenu by remember { mutableStateOf(false) }
    var showTimeMenu by remember { mutableStateOf(false) }
    
    val bands = listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm")
    val modes = listOf("CW", "SSB", "FT8", "FT4", "FM", "RTTY")
    val continents = listOf("OC", "AS", "EU", "NA", "SA", "AF")
    val times = listOf(1 to "1 Hour", 4 to "4 Hours", 12 to "12 Hours", 24 to "24 Hours")

    // Dynamic clock for the top
    var currentZuluTime by remember { mutableStateOf("") }
    var currentLocalTime by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while(true) {
            val now = Instant.now()
            currentZuluTime = ZonedDateTime.ofInstant(now, ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "z"
            currentLocalTime = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            kotlinx.coroutines.delay(1000)
        }
    }

    if (showDxSpots) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Image(
                                painter = painterResource(id = au.com.benji.robert.R.drawable.dxspots1),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    text = "Live DX Spots",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Global cluster synchronization",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF03DAC6)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        
                        // Digital Clocks
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ZULU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(currentZuluTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LOCAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Text(currentLocalTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismissDxSpots,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.Medium))

                // Quick Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Filter
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showTimeMenu = true },
                            label = { Text("Last ${activeTime}h") },
                            leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = showTimeMenu, onDismissRequest = { showTimeMenu = false }) {
                            times.forEach { (hours, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) }, 
                                    onClick = { viewModel.setDxTimeFilter(hours); showTimeMenu = false }
                                )
                            }
                        }
                    }

                    // Band Filter Button
                    Box {
                        FilterChip(
                            selected = activeBand != null,
                            onClick = { showBandMenu = true },
                            label = { Text(activeBand ?: "All Bands") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = showBandMenu, onDismissRequest = { showBandMenu = false }) {
                            DropdownMenuItem(text = { Text("All Bands") }, onClick = { viewModel.setDxBandFilter(null); showBandMenu = false })
                            bands.forEach { band ->
                                DropdownMenuItem(text = { Text(band) }, onClick = { viewModel.setDxBandFilter(band); showBandMenu = false })
                            }
                        }
                    }
                    
                    Box {
                        FilterChip(
                            selected = activeMode != null,
                            onClick = { showModeMenu = true },
                            label = { Text(activeMode ?: "All Modes") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = showModeMenu, onDismissRequest = { showModeMenu = false }) {
                            DropdownMenuItem(text = { Text("All Modes") }, onClick = { viewModel.setDxModeFilter(null); showModeMenu = false })
                            modes.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode) }, onClick = { viewModel.setDxModeFilter(mode); showModeMenu = false })
                            }
                        }
                    }

                    Box {
                        FilterChip(
                            selected = activeContinent != null,
                            onClick = { showContinentMenu = true },
                            label = { Text(activeContinent ?: "All Regions") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = showContinentMenu, onDismissRequest = { showContinentMenu = false }) {
                            DropdownMenuItem(text = { Text("All Continents") }, onClick = { viewModel.setDxContinentFilter(null); showContinentMenu = false })
                            continents.forEach { cont ->
                                DropdownMenuItem(text = { Text(cont) }, onClick = { viewModel.setDxContinentFilter(cont); showContinentMenu = false })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dxSpots.size} spots matching criteria",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (activeBand != null || activeMode != null || activeContinent != null) {
                        Text(
                            text = "Reset Filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.setDxBandFilter(null)
                                viewModel.setDxModeFilter(null)
                                viewModel.setDxContinentFilter(null)
                            }
                        )
                    }
                }

                if (dxSpots.isEmpty() && !isRefreshingDx) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptySectionCard("No spots found for selected timeframe.")
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshingDx,
                        onRefresh = { viewModel.refreshDxSpots() },
                        state = dxPullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                            contentPadding = PaddingValues(bottom = Spacing.Large)
                        ) {
                            items(dxSpots) { spot ->
                                DxSpotItem(spot, onClick = { selectedDxSpot = spot })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShack) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Image(
                                painter = painterResource(id = au.com.benji.robert.R.drawable.theshack1),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "The Shack",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Your Inventory & Equipment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    IconButton(
                        onClick = onDismissShack,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
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

    // Detail Dialogs and Sub-dialogs
    selectedDxSpot?.let { spot ->
        DxSpotDetailDialog(spot = spot, onDismiss = { selectedDxSpot = null })
    }

    selectedShackItem?.let { item ->
        ShackDetailDialog(item = item, onDismiss = { selectedShackItem = null }, onImageClick = { selectedImage = it })
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

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Remove from Shack?") },
            text = { Text("Are you sure you want to remove your ${item.manufacturer} ${item.model}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEquipment(item); itemToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
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
                
                // Smart Fields based on Category
                when (category) {
                    EquipmentCategory.RADIO, EquipmentCategory.AMPLIFIER, EquipmentCategory.SDR, EquipmentCategory.ANALYSER, EquipmentCategory.COMPUTER -> {
                        RobertTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = "Serial Number")
                        RobertTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname (e.g. Primary Rig)")
                    }
                    EquipmentCategory.ANTENNA -> {
                        RobertTextField(value = nickname, onValueChange = { nickname = it }, label = "Location (e.g. Roof, Backyard)")
                    }
                    EquipmentCategory.POWER_SUPPLY -> {
                        RobertTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = "Serial Number (Optional)")
                        RobertTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname / Role")
                    }
                    else -> {
                        RobertTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname / Role")
                    }
                }
                
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
                Text(text = item.manufacturer.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = item.model, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
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
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).clickable { onImageClick(item.imagePath) },
                        contentScale = ContentScale.Crop
                    )
                }
                if (item.nickname.isNotEmpty()) {
                    val nicknameLabel = when(item.category) {
                        "Antenna" -> "LOCATION"
                        else -> "NICKNAME"
                    }
                    DetailInfoItem(label = nicknameLabel, value = item.nickname)
                }
                DetailInfoItem(label = "CATEGORY", value = item.category)
                if (item.serialNumber.isNotEmpty()) {
                    DetailInfoItem(label = "SERIAL NUMBER", value = item.serialNumber)
                }
                if (item.notes.isNotEmpty()) {
                    DetailInfoItem(label = "NOTES", value = item.notes)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
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
                Text(text = spot.source.name, style = MaterialTheme.typography.labelSmall, color = when (spot.source) { SpotSource.POTA -> Color(0xFF4CAF50); SpotSource.SOTA -> Color(0xFFFF9800); else -> MaterialTheme.colorScheme.primary }, fontWeight = FontWeight.Bold)
                Text(text = spot.callsign, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    DetailInfoItem(label = "FREQUENCY", value = "${spot.frequency} MHz", modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "MODE", value = spot.mode.ifEmpty { "N/A" }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                    DetailInfoItem(label = "BAND", value = spot.band, modifier = Modifier.weight(1f))
                    DetailInfoItem(label = "SPOTTER", value = spot.spotter, modifier = Modifier.weight(1f))
                }
                if (spot.location.isNotEmpty()) DetailInfoItem(label = "LOCATION", value = spot.location)
                if (spot.comment.isNotEmpty()) DetailInfoItem(label = "COMMENT", value = spot.comment)
                Button(onClick = { uriHandler.openUri("https://www.qrz.com/db/${spot.callsign}") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW QRZ PROFILE")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
fun WeatherForecastDialog(
    weather: DetailedWeather,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "7-Day Forecast",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = weather.locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    weather.forecast.forEach { day ->
                        ForecastDayItem(day, weather.unit)
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastDayItem(day: ForecastDay, unit: String) {
    val dateText = remember(day.date) {
        try {
            val date = LocalDate.parse(day.date)
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val monthName = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            "$dayName, ${date.dayOfMonth} $monthName"
        } catch (e: Exception) {
            day.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.Medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = day.condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            val icon = remember(day.weatherCode) {
                when {
                    day.weatherCode == 0 -> Icons.Default.WbSunny
                    day.weatherCode in 1..3 -> Icons.Default.CloudQueue
                    day.weatherCode in 45..48 -> Icons.Default.Cloud // Foggy often not in default
                    day.weatherCode in 51..65 -> Icons.Default.WaterDrop
                    day.weatherCode in 80..82 -> Icons.Default.Thunderstorm
                    else -> Icons.Default.Cloud
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${day.maxTemp.toInt()}$unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${day.minTemp.toInt()}$unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun DetailInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
