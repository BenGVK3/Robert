package au.com.benji.robert.screens.dashboard

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import au.com.benji.robert.components.*
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.models.*
import au.com.benji.robert.database.LogEntryEntity
import au.com.benji.robert.database.NetEntity
import au.com.benji.robert.repository.propagation.PropagationData
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.network.SatellitePass
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.utils.MufCalculator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    onShowDxSpots: () -> Unit = {},
    onShowShack: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val callsign by viewModel.callsign.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val locationData by viewModel.locationFlow.collectAsStateWithLifecycle()
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()
    val moonData by viewModel.moonData.collectAsStateWithLifecycle()
    val mufResult by viewModel.mufResult.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    val dxSpots by viewModel.dxSpots.collectAsStateWithLifecycle()
    val favoriteRepeater by viewModel.favoriteRepeater.collectAsStateWithLifecycle()
    val allUpcomingPasses by viewModel.allUpcomingPasses.collectAsStateWithLifecycle()
    val shackSummary by viewModel.shackSummary.collectAsStateWithLifecycle()
    val latestLog by viewModel.latestLog.collectAsStateWithLifecycle()
    val nextNet by viewModel.nextNet.collectAsStateWithLifecycle()
    val aprsPackets by viewModel.aprsPackets.collectAsStateWithLifecycle()
    val nextPassTimer by viewModel.nextPassTimer.collectAsStateWithLifecycle()

    val context = LocalContext.current
    
    // Moon Image logic
    val isMoonDataLoaded = remember(moonData.phaseName) { moonData.phaseName != "---" }
    var currentMoonImageIndex by remember { mutableIntStateOf(2) }
    val targetMoonIndex = remember(moonData.age) {
        val phaseNum = (moonData.age / 29.53).coerceIn(0.0, 1.0)
        when {
            phaseNum < 0.02 || phaseNum > 0.98 -> 2
            phaseNum < 0.24 -> 3 + ((phaseNum - 0.02) / 0.22 * 4).toInt().coerceIn(0, 4)
            phaseNum < 0.26 -> 8
            phaseNum < 0.49 -> 9 + ((phaseNum - 0.26) / 0.23 * 6).toInt().coerceIn(0, 6)
            phaseNum < 0.51 -> 16
            phaseNum < 0.74 -> 17 + ((phaseNum - 0.51) / 0.23 * 5).toInt().coerceIn(0, 5)
            phaseNum < 0.76 -> 23
            else -> 24 + ((phaseNum - 0.76) / 0.24 * 6).toInt().coerceIn(0, 6)
        }
    }

    LaunchedEffect(isMoonDataLoaded) {
        if (isMoonDataLoaded) {
            for (i in 2..30) {
                currentMoonImageIndex = i
                delay(40)
            }
            for (i in 2..targetMoonIndex) {
                currentMoonImageIndex = i
                delay(40)
            }
        }
    }

    val moonResId = remember(currentMoonImageIndex) {
        context.resources.getIdentifier(
            "moon${String.format("%02d", currentMoonImageIndex)}",
            "drawable",
            context.packageName
        )
    }

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

    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14))
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
                    .padding(paddingValues)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- HEADER SECTION WITH IMAGE ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = au.com.benji.robert.R.drawable.robertheader),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    HeaderSection(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                        callsign = callsign,
                        gridSquare = locationData?.fourth ?: "---"
                    )
                }

                // --- GREETING & WEATHER ---
                GreetingSection(name, weatherData)

                // --- MAIN GRID ---
                Row(modifier = Modifier.weight(0.9f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NextNetCard(
                        modifier = Modifier.weight(1f),
                        net = nextNet,
                        navController = navController
                    )
                    SatellitePassCard(
                        modifier = Modifier.weight(1f),
                        nextPassTimer = nextPassTimer,
                        allUpcomingPasses = allUpcomingPasses,
                        navController = navController
                    )
                }

                Row(modifier = Modifier.weight(1.3f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperatingConditionsCard(
                        modifier = Modifier.weight(1.1f).clickable { navController.navigate(Screen.Propagation.route) },
                        solarData = solarData,
                        muf = mufResult.value
                    )
                    MoonStatusCard(
                        modifier = Modifier.weight(0.9f),
                        moonData = moonData,
                        moonResId = moonResId,
                        navController = navController
                    )
                }

                Row(modifier = Modifier.weight(0.9f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FavoriteRepeaterCard(
                        modifier = Modifier.weight(1f),
                        repeater = favoriteRepeater,
                        navController = navController
                    )
                    LatestLogCard(
                        modifier = Modifier.weight(1f),
                        log = latestLog,
                        navController = navController
                    )
                }

                BandConditionsCard(
                    modifier = Modifier.weight(0.8f),
                    propagationData = propagationData, 
                    navController = navController
                )

                RecentDxSpotsCard(
                    modifier = Modifier.weight(0.8f).fillMaxWidth(),
                    spots = dxSpots.take(3),
                    onClick = onShowDxSpots
                )

                // --- BOTTOM DASHBOARD CARDS ---
                DashboardBottomRow(
                    modifier = Modifier.weight(0.7f),
                    shackSummary = shackSummary,
                    onShowShack = onShowShack,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun HeaderSection(modifier: Modifier = Modifier, callsign: String, gridSquare: String) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "R.O.B.E.R.T.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Radio Operator's Band Exploration\n& Resource Tool",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 8.sp,
                lineHeight = 9.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Color(0xFF00B2FF))
                        Text(callsign, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = Color(0xFF00B2FF))
                        Text(gridSquare, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingSection(name: String, weather: DetailedWeather?) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    
    val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(calendar.time)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (name.isNotEmpty()) "$greeting, $name" else greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        if (weather != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(24.dp), tint = Color.White)
                    Column {
                        Text("${weather.temperature}°C", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(weather.condition, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.WaterDrop, null, modifier = Modifier.size(12.dp), tint = Color(0xFF00B2FF))
                    Text("${weather.humidity}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.Air, null, modifier = Modifier.size(12.dp), tint = Color(0xFF00B2FF))
                    Text("${weather.windSpeed.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OperatingConditionsCard(modifier: Modifier, solarData: SolarData, muf: Double) {
    DashboardCard(
        modifier = modifier,
        title = "OPERATING CONDITIONS",
        icon = Icons.Default.WbSunny
    ) {
        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ConditionMetric("SFI", solarData.solarFlux.toString(), "Good")
                ConditionMetric("K-IDX", solarData.kIndex.toString(), "Good")
                ConditionMetric("A-IDX", solarData.aIndex.toString(), "Good")
                ConditionMetric("MUF", String.format("%.1f", muf), "MHz")
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallMetric("Spots", solarData.sunspots.toString())
                SmallMetric("X-Ray", solarData.xRay)
                SmallMetric("Wind", solarData.solarWind.take(4))
                SmallMetric("Bz", solarData.magneticField.split("/").lastOrNull() ?: "---")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge("OVERALL: GOOD", Color(0xFF4CAF50))
                Text(
                    text = "3m ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
fun MoonStatusCard(modifier: Modifier, moonData: MoonData, moonResId: Int, navController: NavHostController) {
    DashboardCard(
        modifier = modifier.clickable { navController.navigate(Screen.Moon.route) },
        title = "MOON STATUS",
        icon = Icons.Default.Brightness2
    ) {
        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (moonResId != 0) {
                    Image(
                        painter = painterResource(id = moonResId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }
                
                Column {
                    Text(moonData.phaseName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${moonData.illumination}% Illum", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00FFCC), fontSize = 8.sp)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Age", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    Text(String.format("%.1fd", moonData.age), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Dist", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    Text(String.format("%.0fk", moonData.distanceKm/1000), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Rise", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    Text(moonData.riseTime, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Set", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    Text(moonData.setTime, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(Color(0xFF4CAF50), CircleShape))
                    Text("EME: GOOD", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                }
                StatusBadge(if (moonData.isVisible) "UP" else "DOWN", if (moonData.isVisible) Color(0xFF4CAF50) else Color.Gray)
            }
        }
    }
}

@Composable
fun NextNetCard(modifier: Modifier, net: NetEntity?, navController: NavHostController) {
    DashboardCard(
        modifier = modifier.clickable { 
            navController.navigate(Screen.Tools.createRoute("Club Nets"))
        },
        title = "NEXT CLUB NET",
        icon = Icons.Default.Groups
    ) {
        if (net == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No nets scheduled",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = net.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = Color(0xFFFFC107).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = au.com.benji.robert.screens.tools.getDayString(net.dayOfWeek),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFC107),
                            fontSize = 8.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CellTower, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                    Text(net.frequency, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(net.time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color(0xFF00B2FF))
                }
            }
        }
    }
}

@Composable
fun SatellitePassCard(modifier: Modifier, nextPassTimer: String, allUpcomingPasses: List<SatellitePass>, navController: NavHostController) {
    val nextPass = allUpcomingPasses.firstOrNull()
    
    DashboardCard(
        modifier = modifier.clickable { navController.navigate(Screen.Satellites.route) },
        title = "NEXT SATELLITE",
        icon = Icons.Default.SatelliteAlt
    ) {
        if (nextPass == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (nextPassTimer.contains("Scanning")) "Scanning sky..." else "No upcoming passes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nextPass.name, 
                            style = MaterialTheme.typography.labelMedium, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Max El: ${nextPass.maxElevation.toInt()}°", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray, 
                            fontSize = 8.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        val timerText = if (nextPassTimer.contains(" • In ")) {
                            nextPassTimer.substringAfter(" • In ").substringBefore("s")
                        } else if (nextPassTimer.contains("m ")) {
                            nextPassTimer.substringBefore("s")
                        } else {
                            "--m"
                        }
                        Text(
                            text = timerText, 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFFA680FF)
                        )
                        Text(
                            text = "TO AOS", 
                            style = MaterialTheme.typography.labelSmall, 
                            fontSize = 6.sp, 
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteRepeaterCard(modifier: Modifier, repeater: Repeater?, navController: NavHostController) {
    DashboardCard(
        modifier = modifier.clickable { navController.navigate(Screen.RepeaterList.route) },
        title = "FAV REPEATER",
        icon = Icons.Default.Podcasts
    ) {
        if (repeater == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No favorites set",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(repeater.callsign, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${repeater.frequency} MHz ${repeater.mode}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                    }
                    StatusBadge("ONLINE", Color(0xFF4CAF50))
                }
                Text(
                    text = "${repeater.town} • ${String.format("%.1fkm", repeater.distance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
fun BandConditionsCard(modifier: Modifier = Modifier, propagationData: PropagationData?, navController: NavHostController) {
    DashboardCard(
        modifier = modifier.fillMaxWidth().clickable { navController.navigate(Screen.Propagation.route) },
        title = "HF BAND CONDITIONS",
        icon = Icons.Default.BarChart
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val bandsToShow = propagationData?.bands ?: emptyList()
                if (bandsToShow.isEmpty()) {
                    Text(
                        "Calculating conditions...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(Spacing.Small)
                    )
                } else {
                    bandsToShow.take(9).forEach { band ->
                        BandMiniGraph(modifier = Modifier.weight(1f), band = band)
                    }
                }
            }
        }
    }
}

@Composable
fun BandMiniGraph(modifier: Modifier, band: BandCondition) {
    val color = try {
        Color(android.graphics.Color.parseColor(band.color))
    } catch (e: Exception) {
        Color.Gray
    }
    
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(band.band, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 7.sp)
        
        Canvas(modifier = Modifier.height(24.dp).fillMaxWidth()) {
            if (band.history.size < 2) {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx()
                )
            } else {
                val stepX = size.width / (band.history.size - 1)
                val points = band.history.mapIndexed { index, score ->
                    Offset(index * stepX, size.height - (score.toFloat() / 100f * size.height))
                }
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[(i - 1).coerceAtLeast(0)]
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
                        
                        val cp1 = p1 + (p2 - p0) / 6f
                        val cp2 = p2 - (p3 - p1) / 6f
                        
                        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        Text(band.rating.take(1), style = MaterialTheme.typography.labelSmall, fontSize = 6.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun RecentDxSpotsCard(modifier: Modifier, spots: List<DxSpot>, onClick: () -> Unit) {
    DashboardCard(
        modifier = modifier.clickable { onClick() },
        title = "RECENT DX SPOTS",
        icon = Icons.Default.Language
    ) {
        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
            if (spots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No spots available", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            } else {
                spots.forEach { spot ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(spot.callsign, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        Text(spot.frequency, style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B2FF), fontWeight = FontWeight.Bold)
                        Text(spot.band, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(spot.normalizedMode, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.weight(0.2f))
                        Text(spot.timeZulu.take(5), style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun LatestLogCard(modifier: Modifier, log: LogEntryEntity?, navController: NavHostController) {
    DashboardCard(
        modifier = modifier.clickable { navController.navigate(Screen.Logbook.route) },
        title = "LATEST LOG",
        icon = Icons.Default.Book
    ) {
        if (log == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No logs yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.callsign, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(log.band, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                }
                Text("${log.mode} • ${log.frequency}k • ${log.power}W", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                Text(if (log.name.isNotEmpty()) log.name else "---", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00B2FF), fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun DashboardBottomRow(
    modifier: Modifier = Modifier,
    shackSummary: Map<String, Int>,
    onShowShack: () -> Unit,
    navController: NavHostController
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val bottomCards = listOf(
            Triple("SHACK", Icons.Default.Home, listOf(
                "Rad" to (shackSummary["Radio"] ?: 0), 
                "Ant" to (shackSummary["Antenna"] ?: 0)
            )),
            Triple("APRS", Icons.Default.LocationOn, listOf("Pkt" to 128, "Near" to "2.1k")),
            Triple("KIWI", Icons.Default.Wifi, listOf("Lsn" to 12, "Lat" to "42ms")),
            Triple("FAV", Icons.Default.Star, listOf("Sat" to 3, "Rpt" to 2)),
            Triple("ALRT", Icons.Default.Notifications, listOf("ISS" to "14m", "Kp" to "Up"))
        )

        bottomCards.forEach { (title, icon, items) ->
            val isEnabled = title != "FAV" && title != "ALRT"
            
            val onClick = when(title) {
                "SHACK" -> onShowShack
                "APRS" -> { { navController.navigate(Screen.Aprs.route) } }
                "KIWI" -> { { navController.navigate(Screen.Sdr.route) } }
                else -> { {} }
            }

            BottomDashboardCard(
                modifier = Modifier.weight(1f),
                title = title,
                icon = icon,
                items = items,
                onClick = onClick,
                enabled = isEnabled
            )
        }
    }
}

@Composable
fun BottomDashboardCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    items: List<Pair<String, Any?>>,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161C24),
            contentColor = if (enabled) Color.Unspecified else Color.Gray
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C242F))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .fillMaxSize()
                .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    icon, 
                    null, 
                    modifier = Modifier.size(10.dp), 
                    tint = if (enabled) Color(0xFF00B2FF) else Color.Gray
                )
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Black, 
                    color = if (enabled) Color(0xFF00B2FF) else Color.Gray
                )
            }
            
            Column(verticalArrangement = Arrangement.Center) {
                items.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray)
                        Text(value.toString(), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11171F)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C242F))
    ) {
        Column(modifier = Modifier.padding(6.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, modifier = Modifier.size(11.dp), tint = Color(0xFF00B2FF))
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF00B2FF), letterSpacing = 0.5.sp, fontSize = 8.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
fun ConditionMetric(label: String, value: String, status: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.sp)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
        Text(status, style = MaterialTheme.typography.labelSmall, color = if (status == "Good") Color(0xFF4CAF50) else Color.Gray, fontSize = 7.sp)
    }
}

@Composable
fun SmallMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 7.sp
        )
    }
}
