package au.com.benji.robert.screens.propagation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.MufCalculator
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropagationScreen(
    paddingValues: PaddingValues,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val solarData by dashboardViewModel.solarData.collectAsStateWithLifecycle()
    val mufResult by dashboardViewModel.mufResult.collectAsStateWithLifecycle()
    val bandConditions by dashboardViewModel.propagationData.collectAsStateWithLifecycle()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { dashboardViewModel.refresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Image(
                        painter = painterResource(id = au.com.benji.robert.R.drawable.propagation),
                        contentDescription = null,
                        modifier = Modifier
                            .size(62.dp)
                            .offset(x = 5.dp),
                        colorFilter = null,
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Propagation Center",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                solarData?.let { data ->
                    SolarDataCard(data, mufResult)
                }
            }

            item {
                Text(
                    text = "Live Band Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            bandConditions?.let { data ->
                val chunkedBands = data.bands.chunked(2)
                items(chunkedBands) { rowBands ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        rowBands.forEach { band ->
                            BandConditionCard(band, modifier = Modifier.weight(1f))
                        }
                        if (rowBands.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } ?: item { 
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        Text("Calculating band stability...", style = MaterialTheme.typography.bodySmall)
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
                            text = "Data combines real-time NOAA solar indices and traffic density.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(Spacing.Large)) }
        }
    }
}

@Composable
fun SolarDataCard(data: SolarData, mufResult: MufCalculator.MufResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "HAMQSL SOLAR INDICES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("SFI", data.solarFlux.toString())
                MetricItem("SN", data.sunspots.toString())
                MetricItem("A-Idx", data.aIndex.toString())
                MetricItem("K-Idx", data.kIndex.toString())
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("X-Ray", data.xRay)
                    DetailRow("Wind", data.solarWind)
                    DetailRow("Magnetic", data.magneticField)
                }
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("Proton", data.protonFlux)
                    DetailRow("Electron", data.electronFlux)
                    DetailRow("Aurora", data.aurora)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Small)) {
                        Text(
                            text = if (mufResult.isEstimated) "Estimated MUF" else "Reported MUF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mufResult.isEstimated) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", mufResult.value),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = " MHz",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Small)) {
                        Text("foF2", style = MaterialTheme.typography.labelSmall)
                        Text(data.foF2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text("VHF CONDITIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VHF Aurora: ${data.vhfAurora}", style = MaterialTheme.typography.bodySmall)
                Text("E-Skip: ${data.eSkip}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
fun BandConditionCard(band: BandCondition, modifier: Modifier = Modifier) {
    val color = when (band.rating) {
        "Excellent" -> Color(0xFF4CAF50)
        "Good" -> Color(0xFF8BC34A)
        "Fair" -> Color(0xFFFFC107)
        "Poor" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val (trendIcon, trendColor) = when (band.trend) {
        "Improving" -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF4CAF50)
        "Declining" -> Icons.AutoMirrored.Filled.TrendingDown to Color(0xFFF44336)
        else -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier.padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = band.band, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = trendColor
                )
            }
            
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = band.rating.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = color,
                    fontSize = 10.sp
                )
            }
        }
    }
}
