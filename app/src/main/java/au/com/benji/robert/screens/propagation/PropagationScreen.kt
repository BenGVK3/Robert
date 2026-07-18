package au.com.benji.robert.screens.propagation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import au.com.benji.robert.R
import au.com.benji.robert.components.PremiumActionCard
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.propagation.BandCondition
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropagationScreen(
    paddingValues: PaddingValues,
    dashboardViewModel: DashboardViewModel = viewModel(),
    navController: NavController
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            contentPadding = PaddingValues(Spacing.Medium),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.propagation1),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.Small))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Propagation Center",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Real-time HF & Solar Analysis",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF03DAC6),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PremiumActionCard(
                        icon = R.drawable.dxlook1,
                        title = "Open DXLook",
                        onClick = { 
                            try {
                                navController.navigate(Screen.DxLook.route) 
                            } catch (e: Exception) {
                                android.util.Log.e("PropagationScreen", "Navigation to DXLook failed", e)
                            }
                        },
                        modifier = Modifier.widthIn(min = 180.dp, max = 220.dp)
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                SectionHeader("Solar Data", Icons.Default.WbSunny)
            }

            item(span = { GridItemSpan(2) }) {
                solarData?.let { data ->
                    OperatingConditionsCard(data, mufResult.value)
                }
            }

            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Live Band Conditions", Icons.Default.Podcasts)
                    
                    bandConditions?.let { data ->
                        val mins = (System.currentTimeMillis() - data.timestamp) / 60000
                        Text(
                            text = if (mins < 1) "LIVE" else "${mins}m ago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            val bands = bandConditions?.bands ?: emptyList()
            if (bands.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing band conditions...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                items(bands) { condition ->
                    DetailedBandCard(condition, onClick = {
                        navController.navigate(Screen.BandDetail.createRoute(condition.band))
                    })
                }
            }

            item(span = { GridItemSpan(2) }) {
                SolarDataDisclaimer()
            }
            
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun DetailedBandCard(condition: BandCondition, onClick: () -> Unit) {
    val scoreColor = try {
        Color(android.graphics.Color.parseColor(condition.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = condition.band,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = when (condition.trend) {
                        "Improving" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "Declining" -> Color.Red.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = when (condition.trend) {
                            "Declining" -> Icons.AutoMirrored.Filled.TrendingDown
                            "Improving" -> Icons.AutoMirrored.Filled.TrendingUp
                            else -> Icons.AutoMirrored.Filled.TrendingFlat
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp).size(14.dp),
                        tint = when (condition.trend) {
                            "Declining" -> Color.Red
                            "Improving" -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scoreColor.copy(alpha = 0.05f))
                    .padding(vertical = 4.dp)
            ) {
                SparklineGraph(
                    data = condition.history,
                    color = scoreColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            Surface(
                color = scoreColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${condition.score} • ${condition.rating.uppercase()}",
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = scoreColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                condition.summaries.forEach { summary ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summary.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        val ratingColor = when (summary.rating) {
                            "Excellent", "Very Good", "Good" -> Color(0xFF4CAF50)
                            "Fair" -> Color(0xFFFFC107)
                            "Poor" -> Color(0xFFFF9800)
                            else -> Color.Red
                        }
                        Text(
                            text = summary.rating,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = ratingColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OperatingConditionsCard(solarData: SolarData, muf: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            // 1. Primary Solar Indices (Top Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryMetricCard(Modifier.weight(1f), "SFI", solarData.solarFlux.toString())
                PrimaryMetricCard(Modifier.weight(1f), "SN", solarData.sunspots.toString())
                PrimaryMetricCard(Modifier.weight(1f), "A-IDX", solarData.aIndex.toString())
                PrimaryMetricCard(Modifier.weight(1f), "K-IDX", solarData.kIndex.toString())
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(Spacing.Medium))

            // 2. Secondary Solar Data (Two Columns)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    SecondaryMetricItem("X-RAY", solarData.xRay)
                    SecondaryMetricItem("SOLAR WIND", solarData.solarWind)
                    SecondaryMetricItem("MAG FIELD (Bz)", solarData.magneticField)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    SecondaryMetricItem("PROTON FLUX", solarData.protonFlux)
                    SecondaryMetricItem("ELECTRON FLUX", solarData.electronFlux)
                    SecondaryMetricItem("AURORA INDEX", solarData.aurora)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            // 3. Highlighted Radio Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                HighlightedMetricCard(
                    Modifier.weight(1f),
                    "ESTIMATED MUF",
                    if (muf > 0) String.format(Locale.US, "%.1f MHz", muf) else solarData.muf,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
                HighlightedMetricCard(
                    Modifier.weight(1f),
                    "foF2",
                    solarData.foF2,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            // 4. VHF Conditions Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = Spacing.Small, horizontal = Spacing.Medium),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VhfStatusItem("VHF AURORA", solarData.vhfAurora)
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    VhfStatusItem("E-SKIP", solarData.eSkip)
                }
            }
        }
    }
}

@Composable
private fun PrimaryMetricCard(modifier: Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF81D4FA),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SecondaryMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF81D4FA),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun HighlightedMetricCard(modifier: Modifier, label: String, value: String, backgroundColor: Color) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF81D4FA).copy(alpha = 0.8f),
                fontWeight = FontWeight.Black,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VhfStatusItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF81D4FA),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
fun SolarDataDisclaimer() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            Text(
                text = "Data combines real-time NOAA solar indices and traffic density analysis.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun SparklineGraph(data: List<Int>, color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val path = androidx.compose.ui.graphics.Path()
        
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { index, value ->
            androidx.compose.ui.geometry.Offset(
                x = index * stepX,
                y = size.height - (value.toFloat() / 100f * size.height)
            )
        }

        path.moveTo(points[0].x, points[0].y)
        
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val controlPoint1 = androidx.compose.ui.geometry.Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
            val controlPoint2 = androidx.compose.ui.geometry.Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
        }
        
        // Shadow/Fill
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.15f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Draw terminal point
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = points.last()
        )
    }
}
