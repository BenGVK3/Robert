package au.com.benji.robert.screens.satellites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.network.SatellitePosition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@Composable
fun SatellitesScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val positions by viewModel.satellitePositions.collectAsStateWithLifecycle()
    val timer by viewModel.nextPassTimer.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Orbital Tracking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                RobertMap(url = "https://www.n2yo.com/widgets/widget-tracker.php?s=25544&size=small&all=1")
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SatelliteAlt, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Column {
                        Text(text = "UPCOMING PRIORITY PASS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        Text(text = timer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (positions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text("Acquiring telemetry...")
                    }
                }
            }
        }

        items(positions) { satellite ->
            SatelliteDetailCard(satellite)
        }
        
        item { Spacer(modifier = Modifier.height(Spacing.Large)) }
    }
}

@Composable
fun SatelliteDetailCard(satellite: SatellitePosition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = satellite.name, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Public,
                    label = "Lat",
                    value = satellite.latitude.toString().take(6)
                )
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Public,
                    label = "Lon",
                    value = satellite.longitude.toString().take(6)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Height,
                    label = "Alt",
                    value = "${satellite.altitude.toInt()} km"
                )
                SatelliteMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Velocity",
                    value = "${satellite.velocity.toInt()} km/h"
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "VISIBILITY: ${satellite.visibility.uppercase()}", 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SatelliteMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Text(text = label, style = MaterialTheme.typography.labelSmall)
            }
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}
