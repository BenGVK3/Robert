package au.com.benji.robert.screens.dashboard

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import au.com.benji.robert.components.DashboardSectionTitle
import au.com.benji.robert.components.MetricCard
import au.com.benji.robert.components.QuickActionCard
import au.com.benji.robert.components.RobertTopBar
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.theme.RobertColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val nextPass by viewModel.nextPassTimer.collectAsStateWithLifecycle()

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            viewModel.refresh()
        } else {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RobertTopBar()
            IconButton(
                onClick = { viewModel.refresh() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
            }
        }

        // --- SPACE WEATHER SECTION ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            DashboardSectionTitle("Live Solar Conditions")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Solar Flux",
                    value = solarData?.solarFlux?.toString() ?: "---",
                    icon = Icons.Default.WbSunny
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "K-Index",
                    value = solarData?.kIndex?.toString() ?: "---",
                    icon = Icons.Default.Public
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "A-Index",
                    value = solarData?.aIndex?.toString() ?: "---",
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "MUF",
                    value = solarData?.muf?.replace(" MHz", "") ?: "---",
                    unit = "MHz",
                    icon = Icons.Default.Wifi
                )
            }
        }

        // --- LOCAL WEATHER SECTION ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            DashboardSectionTitle("Local Weather: ${weatherData?.locationName ?: "Locating..."}")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${weatherData?.temperature ?: "--"}${weatherData?.unit ?: "°C"}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weatherData?.condition ?: "Checking conditions...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    
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

        // --- ORBITAL SECTION ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            DashboardSectionTitle("Orbital Operations")
            MetricCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Next Satellite Pass",
                value = nextPass,
                icon = Icons.Default.SatelliteAlt
            )
        }

        // --- QUICK ACTIONS ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            DashboardSectionTitle("Control Center")
            
            val actions = viewModel.quickActions
            val rows = actions.chunked(2)
            
            rows.forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    rowActions.forEach { action ->
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = getActionIcon(action.title),
                            title = action.title,
                            onClick = { navController.navigate(action.route) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.Large))
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
fun getActionIcon(title: String): ImageVector {
    return when (title) {
        "Propagation" -> Icons.Default.SignalCellularAlt
        "SDR" -> Icons.Default.Radio
        "APRS" -> Icons.Default.LocationOn
        "Satellites" -> Icons.Default.Explore
        "Logbook" -> Icons.Default.EditNote
        "Shack" -> Icons.Default.HomeWork
        "Tools" -> Icons.Default.Construction
        "Settings" -> Icons.Default.Settings
        else -> Icons.Default.Apps
    }
}
