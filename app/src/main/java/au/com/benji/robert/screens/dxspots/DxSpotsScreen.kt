package au.com.benji.robert.screens.dxspots

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.R
import au.com.benji.robert.components.DxSpotItem
import au.com.benji.robert.components.DxSpotDetailDialog
import au.com.benji.robert.models.DxSpot
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DxSpotsScreen(
    onBack: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    val dxSpots by viewModel.dxSpots.collectAsStateWithLifecycle()
    val isRefreshingDx by viewModel.isRefreshingDx.collectAsStateWithLifecycle()
    val dxPullToRefreshState = rememberPullToRefreshState()

    val activeBand by viewModel.dxBandFilter.collectAsStateWithLifecycle()
    val activeMode by viewModel.dxModeFilter.collectAsStateWithLifecycle()
    val activeContinent by viewModel.dxContinentFilter.collectAsStateWithLifecycle()
    val activeTime by viewModel.dxTimeFilter.collectAsStateWithLifecycle()
    
    var selectedDxSpot by remember { mutableStateOf<DxSpot?>(null) }
    var showBandMenu by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showContinentMenu by remember { mutableStateOf(false) }
    var showTimeMenu by remember { mutableStateOf(false) }
    
    val bands = listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm")
    val modes = listOf("CW", "SSB", "FT8", "FT4", "FM", "RTTY")
    val continents = listOf("OC", "AS", "EU", "NA", "SA", "AF")
    val times = listOf(1 to "1 Hour", 4 to "4 Hours", 12 to "12 Hours", 24 to "24 Hours")

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

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium)
                    .padding(top = Spacing.Medium)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dxspots1),
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
                }
                
                Spacer(modifier = Modifier.height(Spacing.Medium))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(top = Spacing.Small))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshingDx,
            onRefresh = { viewModel.refreshDxSpots() },
            state = dxPullToRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                contentPadding = PaddingValues(Spacing.Medium)
            ) {
                items(dxSpots) { spot ->
                    DxSpotItem(spot, onClick = { selectedDxSpot = spot })
                }
            }
        }
    }

    selectedDxSpot?.let { spot ->
        DxSpotDetailDialog(spot = spot, onDismiss = { selectedDxSpot = null })
    }
}
