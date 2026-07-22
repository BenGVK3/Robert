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
import androidx.compose.foundation.verticalScroll
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
import au.com.benji.robert.models.SpotSource
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
    val activeSource by viewModel.dxSourceFilter.collectAsStateWithLifecycle()
    val activeTime by viewModel.dxTimeFilter.collectAsStateWithLifecycle()
    
    var selectedDxSpot by remember { mutableStateOf<DxSpot?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium)
                    .padding(top = paddingValues.calculateTopPadding())
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

                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        val isFiltered = activeBand != null || activeMode != null || activeContinent != null || activeSource != null
                        BadgedBox(badge = { if (isFiltered) Badge() }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dxSpots.size} spots matching criteria",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (activeBand != null || activeMode != null || activeContinent != null || activeSource != null) {
                        Text(
                            text = "Reset Filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.setDxBandFilter(null)
                                viewModel.setDxModeFilter(null)
                                viewModel.setDxContinentFilter(null)
                                viewModel.setDxSourceFilter(null)
                            }
                        )
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
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + Spacing.Medium,
                    bottom = paddingValues.calculateBottomPadding() + Spacing.Medium
                )
            ) {
                items(dxSpots) { spot ->
                    DxSpotItem(spot, onClick = { selectedDxSpot = spot })
                }
            }
        }
    }

    if (showFilterDialog) {
        DxFilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }

    selectedDxSpot?.let { spot ->
        DxSpotDetailDialog(spot = spot, onDismiss = { selectedDxSpot = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DxFilterDialog(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val activeBand by viewModel.dxBandFilter.collectAsStateWithLifecycle()
    val activeMode by viewModel.dxModeFilter.collectAsStateWithLifecycle()
    val activeContinent by viewModel.dxContinentFilter.collectAsStateWithLifecycle()
    val activeSource by viewModel.dxSourceFilter.collectAsStateWithLifecycle()
    val activeTime by viewModel.dxTimeFilter.collectAsStateWithLifecycle()

    val bands = listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m", "2m", "70cm")
    val modes = listOf("CW", "SSB", "FT8", "FT4", "FM", "RTTY")
    val continents = listOf("OC", "AS", "EU", "NA", "SA", "AF")
    val times = listOf(1 to "1h", 4 to "4h", 12 to "12h", 24 to "24h")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter DX Spots", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text("Timeframe", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { (hours, label) ->
                        FilterChip(
                            selected = activeTime == hours,
                            onClick = { viewModel.setDxTimeFilter(hours) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeSource == null,
                        onClick = { viewModel.setDxSourceFilter(null) },
                        label = { Text("All Sources") }
                    )
                    SpotSource.entries.forEach { source ->
                        FilterChip(
                            selected = activeSource == source,
                            onClick = { viewModel.setDxSourceFilter(source) },
                            label = { Text(if (source == SpotSource.PARKSNPEAKS) "PNP" else source.name) }
                        )
                    }
                }

                Text("Band", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeBand == null,
                        onClick = { viewModel.setDxBandFilter(null) },
                        label = { Text("All Bands") }
                    )
                    bands.forEach { band ->
                        FilterChip(
                            selected = activeBand == band,
                            onClick = { viewModel.setDxBandFilter(band) },
                            label = { Text(band) }
                        )
                    }
                }

                Text("Mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeMode == null,
                        onClick = { viewModel.setDxModeFilter(null) },
                        label = { Text("All Modes") }
                    )
                    modes.forEach { mode ->
                        FilterChip(
                            selected = activeMode == mode,
                            onClick = { viewModel.setDxModeFilter(mode) },
                            label = { Text(mode) }
                        )
                    }
                }

                Text("Region", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeContinent == null,
                        onClick = { viewModel.setDxContinentFilter(null) },
                        label = { Text("All Continents") }
                    )
                    continents.forEach { cont ->
                        FilterChip(
                            selected = activeContinent == cont,
                            onClick = { viewModel.setDxContinentFilter(cont) },
                            label = { Text(cont) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}
