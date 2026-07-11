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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.utils.MufCalculator
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.repository.propagation.AuroraReport
import au.com.benji.robert.repository.propagation.ESkipReport
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
    val location by dashboardViewModel.locationFlow.collectAsStateWithLifecycle()

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
                        painter = painterResource(id = au.com.benji.robert.R.drawable.propagation1),
                        contentDescription = null,
                        modifier = Modifier
                            .size(62.dp),
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
                    SolarDataCard(data, mufResult, bandConditions?.aurora, bandConditions?.eSkip)
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
fun SolarDataCard(data: SolarData, mufResult: MufCalculator.MufResult, aurora: AuroraReport? = null, eSkip: ESkipReport? = null) {
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", mufResult.foF2),
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
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text("VHF CONDITIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val statusColor = remember(aurora?.color) {
                    try {
                        if (aurora?.color != null) Color(android.graphics.Color.parseColor(aurora.color)) else null
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val skipColor = remember(eSkip?.color) {
                    try {
                        if (eSkip?.color != null) Color(android.graphics.Color.parseColor(eSkip.color)) else null
                    } catch (e: Exception) {
                        null
                    }
                }
                
                Text(
                    text = "VHF Aurora: ${aurora?.status ?: data.vhfAurora}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (aurora != null) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "E-Skip: ${eSkip?.status ?: data.eSkip}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = skipColor ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (eSkip != null) FontWeight.Bold else FontWeight.Normal
                )
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
    val color = try {
        Color(android.graphics.Color.parseColor(band.color))
    } catch (e: Exception) {
        when (band.rating) {
            "Excellent" -> Color(0xFF4CAF50)
            "Very Good" -> Color(0xFF4CAF50)
            "Good" -> Color(0xFF8BC34A)
            "Fair" -> Color(0xFFFFC107)
            "Poor" -> Color(0xFFF44336)
            else -> MaterialTheme.colorScheme.primary
        }
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = band.band, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = trendColor
                )
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                if (band.history.isNotEmpty()) {
                    SparklineGraph(
                        history = band.history,
                        color = color,
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                    )
                } else {
                    Text("---", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Surface(
                color = color.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = "${band.score} - ${band.rating.uppercase()}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = color,
                    fontSize = 10.sp
                )
            }

            if (band.summaries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(4.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    band.summaries.forEach { summary ->
                        OperatingSummaryRow(summary)
                    }
                }
            }
        }
    }
}

@Composable
fun OperatingSummaryRow(summary: au.com.benji.robert.repository.propagation.OperatingSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 9.sp,
            maxLines = 1
        )
        
        val ratingColor = when (summary.rating) {
            "Excellent" -> Color(0xFF4CAF50)
            "Very Good" -> Color(0xFF4CAF50)
            "Good" -> Color(0xFF8BC34A)
            "Fair" -> Color(0xFFFFC107)
            "Poor" -> Color(0xFFF44336)
            else -> MaterialTheme.colorScheme.outline
        }

        Text(
            text = summary.rating,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ratingColor,
            fontSize = 9.sp
        )
    }
}

@Composable
fun SparklineGraph(history: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val maxScore = 100f
        val stepX = width / (history.size - 1)
        
        val path = Path().apply {
            history.forEachIndexed { index, score ->
                val x = index * stepX
                val y = height - (score.toFloat() / maxScore * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
