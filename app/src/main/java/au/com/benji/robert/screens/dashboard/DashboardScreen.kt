package au.com.benji.robert.screens.dashboard

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import au.com.benji.robert.components.*
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.models.MoonData
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.utils.MufCalculator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val callsign by viewModel.callsign.collectAsStateWithLifecycle()
    val locationData by viewModel.locationFlow.collectAsStateWithLifecycle()
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()
    val moonData by viewModel.moonData.collectAsStateWithLifecycle()
    val mufResult by viewModel.mufResult.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()

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
                    .padding(paddingValues)
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
                                        Color.Black.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
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
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large)
                ) {
                    // Status Badges below Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderBadge(text = callsign, icon = Icons.Default.Person)
                        HeaderBadge(text = locationData?.fourth ?: "---", icon = Icons.Default.LocationOn)
                    }

                    // --- COMPACT LOCAL WEATHER ---
                    weatherData?.let { weather ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = weather.locationName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${weather.temperature}${weather.unit}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.Small))
                                        Text(
                                            text = "• ${weather.condition}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                    WeatherCompactDetail(Icons.Default.WaterDrop, "${weather.humidity}%")
                                    WeatherCompactDetail(Icons.Default.Air, "${weather.windSpeed}")
                                }
                            }
                        }
                    }

                    // --- COMPACT SPACE WEATHER ---
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                Image(
                                    painter = painterResource(id = au.com.benji.robert.R.drawable.sun),
                                    contentDescription = "The Sun",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                DashboardSectionTitle("Space Weather")
                            }
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                // Important Values Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(1f),
                                        title = "SFI",
                                        value = solarData.solarFlux.toString(),
                                        icon = Icons.Default.WbSunny
                                    )
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(0.8f),
                                        title = "K-Idx",
                                        value = solarData.kIndex.toString(),
                                        icon = Icons.Default.Public
                                    )
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(1.2f),
                                        title = if (mufResult.isEstimated) "Est. MUF" else "MUF",
                                        value = String.format("%.1f", mufResult.value),
                                        unit = "MHz",
                                        icon = Icons.Default.WifiTethering
                                    )
                                }
                                
                                // Secondary Values Grid
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactMetric(modifier = Modifier.weight(1f), label = "Spots", value = solarData.sunspots.toString())
                                    CompactMetric(modifier = Modifier.weight(1f), label = "A-Idx", value = solarData.aIndex.toString())
                                    CompactMetric(modifier = Modifier.weight(1f), label = "X-Ray", value = solarData.xRay)
                                }
                            }
                        }
                        
                        // Propagation Summary
                        val summary = remember(propagationData) {
                            val goodBands = propagationData?.bands?.filter { it.rating == "Excellent" || it.rating == "Good" } ?: emptyList()
                            if (goodBands.isEmpty()) "Conditions stable across most bands"
                            else "${goodBands.take(2).joinToString(" and ") { it.band }} active"
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = Spacing.Small)
                        )
                    }

                    // --- COMPACT MOON CENTER ---
                    Column(
                        modifier = Modifier.clickable { 
                            if (!navController.popBackStack(Screen.Moon.route, inclusive = false)) {
                                navController.navigate(Screen.Moon.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        this.saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = moonData.phaseIcon, fontSize = 18.sp)
                                }
                                DashboardSectionTitle("Moon Center")
                            }
                            
                            // EME Status Indicator
                            Surface(
                                color = if (moonData.isVisible) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (moonData.isVisible) Color(0xFF4CAF50) else Color.Red)
                                    )
                                    Text(
                                        text = if (moonData.isVisible) "EME Possible Now" else "Moon Below Horizon",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (moonData.isVisible) Color(0xFF4CAF50) else Color.Red
                                    )
                                }
                            }
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                // Important Values Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(1f),
                                        title = "Altitude",
                                        value = String.format("%.1f°", moonData.altitude),
                                        icon = Icons.Default.VerticalAlignTop
                                    )
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(1f),
                                        title = "Azimuth",
                                        value = String.format("%.1f°", moonData.azimuth),
                                        icon = Icons.Default.Explore
                                    )
                                    ImportantMetricSmall(
                                        modifier = Modifier.weight(1f),
                                        title = "Distance",
                                        value = String.format("%,.0f", moonData.distanceKm / 1000),
                                        unit = "k km",
                                        icon = Icons.Default.Straighten
                                    )
                                }
                                
                                // Secondary Values Grid
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactMetric(modifier = Modifier.weight(1f), label = "Phase", value = moonData.phaseName)
                                    CompactMetric(modifier = Modifier.weight(1f), label = "Illum", value = "${moonData.illumination}%")
                                    CompactMetric(modifier = Modifier.weight(1f), label = "Doppler", value = String.format("%+.0fHz", moonData.doppler432))
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
            }
        }
    }
}
