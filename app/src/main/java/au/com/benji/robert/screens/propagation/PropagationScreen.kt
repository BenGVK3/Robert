package au.com.benji.robert.screens.propagation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@Composable
fun PropagationScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val solarData by viewModel.solarData.collectAsStateWithLifecycle()
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        item {
            Text(
                text = "Propagation Center",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            solarData?.let { data ->
                SolarDataCard(data)
            }
        }

        propagationData?.ducting?.let { ducting ->
            item {
                Text(
                    text = "Atmospheric Ducting",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ducting.isActive) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = if (ducting.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(
                                text = if (ducting.isActive) "ENHANCED PROPAGATION" else "NORMAL CONDITIONS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text(text = ducting.description, style = MaterialTheme.typography.bodyLarge)
                        if (ducting.isActive) {
                            Spacer(modifier = Modifier.height(Spacing.Small))
                            Text(
                                text = "Intensity: ${ducting.intensity}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Band Conditions",
                style = MaterialTheme.typography.titleLarge
            )
        }

        propagationData?.let { data ->
            items(data.bands) { band ->
                BandConditionRow(band)
            }
        } ?: item { 
            Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text("Loading local band conditions...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = "Data incorporates real-time solar indices and local tropospheric models.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun SolarDataCard(data: SolarData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("SFI", data.solarFlux.toString())
                MetricItem("K-Index", data.kIndex.toString())
                MetricItem("A-Index", data.aIndex.toString())
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "Estimated MUF: ${data.muf}", 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BandConditionRow(band: BandCondition) {
    val color = when (band.rating) {
        "Excellent" -> Color(0xFF4CAF50)
        "Good" -> Color(0xFF8BC34A)
        "Fair" -> Color(0xFFFFC107)
        "Poor" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = band.band, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = band.trend, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Text(
                    text = band.rating.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
