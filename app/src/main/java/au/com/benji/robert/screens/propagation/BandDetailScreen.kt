package au.com.benji.robert.screens.propagation

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.database.PropagationHistoryEntity
import au.com.benji.robert.repository.propagation.PropagationPoint
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.utils.PropagationSmoother
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandDetailScreen(
    bandName: String,
    viewModel: DashboardViewModel = viewModel(),
    onBack: () -> Unit
) {
    val propagationData by viewModel.propagationData.collectAsStateWithLifecycle()
    val bandCondition = propagationData?.bands?.find { it.band == bandName }
    
    var timeframeHours by remember { mutableIntStateOf(24) }
    val historyEntities by viewModel.bandConditions.getHistoryFlow(bandName, timeframeHours).collectAsStateWithLifecycle(initialValue = emptyList())

    val smoothedHistory = remember(historyEntities) {
        val rawPoints = historyEntities.map { PropagationPoint(it.timestamp, it.score) }
        PropagationSmoother.smoothHistory(bandName, rawPoints)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$bandName Propagation History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF0A0E14)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11171F)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C242F))
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT SCORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = "${bandCondition?.score ?: "--"}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val color = try { Color(android.graphics.Color.parseColor(bandCondition?.color ?: "#808080")) } catch(e: Exception) { Color.Gray }
                            Surface(
                                color = color.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = bandCondition?.rating?.uppercase() ?: "UNKNOWN",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = color
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Last updated: Just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    Text(
                        text = "ANALYSIS & ALGORITHM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This forecast is calculated using a real-time blend of Solar Flux (SFI), Sunspot activity, and current K-Index trends. For the $bandName band, we also integrate live PSK Reporter signal-to-noise ratios and regional ionospheric MUF data to determine the current path probability score (0-100).",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
            }

            // Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 6, 24, 72, 168).forEach { hours ->
                    val label = when(hours) {
                        1 -> "1H"
                        6 -> "6H"
                        24 -> "24H"
                        72 -> "3D"
                        168 -> "7D"
                        else -> "${hours}H"
                    }
                    FilterChip(
                        selected = timeframeHours == hours,
                        onClick = { timeframeHours = hours },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Large Graph
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1219))
                    .padding(vertical = 24.dp, horizontal = 8.dp)
            ) {
                DetailedPropagationGraph(
                    history = smoothedHistory,
                    forecast = bandCondition?.forecastData ?: emptyList(),
                    color = try { Color(android.graphics.Color.parseColor(bandCondition?.color ?: "#00B2FF")) } catch(e: Exception) { Color(0xFF00B2FF) },
                    timeframeLabel = "Last $timeframeHours Hours"
                )
            }

            // Additional Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                StatCard(Modifier.weight(1f), "Peak Score", "${smoothedHistory.maxOfOrNull { it.score } ?: "--"}", Icons.Default.TrendingUp)
                StatCard(Modifier.weight(1f), "Trend", bandCondition?.trend ?: "Stable", Icons.Default.Timeline)
            }
        }
    }
}

@Composable
fun DetailedPropagationGraph(
    history: List<PropagationPoint>,
    forecast: List<PropagationPoint>,
    color: Color,
    timeframeLabel: String
) {
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(0f) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(timeframeLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Score 0-100", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                        offset += pan.x
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width * zoom
                val height = size.height
                
                // Draw Grid Lines
                val gridLines = 5
                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val allPoints = (history + forecast).sortedBy { it.timestamp }
                if (allPoints.isEmpty()) return@Canvas

                val minTime = if (allPoints.isNotEmpty()) allPoints.minOf { it.timestamp } else 0L
                val maxTime = if (allPoints.isNotEmpty()) allPoints.maxOf { it.timestamp } else 0L
                val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                
                fun getOffset(p: PropagationPoint): Offset {
                    val x = ((p.timestamp - minTime).toFloat() / timeRange.toFloat()) * width + offset
                    val y = height - (p.score.toFloat() / 100f * height)
                    return Offset(x, y)
                }

                // Draw History Path with Cubic Smoothing
                if (history.size >= 2) {
                    val historyPoints = history.map { getOffset(it) }
                    val path = Path().apply {
                        moveTo(historyPoints[0].x, historyPoints[0].y)
                        for (i in 0 until historyPoints.size - 1) {
                            val p1 = historyPoints[i]
                            val p2 = historyPoints[i + 1]
                            val cp1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                            val cp2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                        }
                    }

                    // Gradient Fill
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(historyPoints.last().x, height)
                        lineTo(historyPoints.first().x, height)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Draw Forecast Path with Cubic Smoothing (Dotted)
                if (forecast.isNotEmpty() && history.isNotEmpty()) {
                    val lastHistory = getOffset(history.last())
                    val forecastPoints = listOf(lastHistory) + forecast.map { getOffset(it) }
                    
                    val forecastPath = Path().apply {
                        moveTo(forecastPoints[0].x, forecastPoints[0].y)
                        for (i in 0 until forecastPoints.size - 1) {
                            val p1 = forecastPoints[i]
                            val p2 = forecastPoints[i + 1]
                            val cp1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                            val cp2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                        }
                    }

                    drawPath(
                        path = forecastPath,
                        color = color.copy(alpha = 0.4f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Draw current point marker
                if (history.isNotEmpty()) {
                    val lastPoint = getOffset(history.last())
                    if (lastPoint.x in 0f..size.width) {
                        drawCircle(
                            color = color,
                            radius = 6.dp.toPx(),
                            center = lastPoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = lastPoint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11171F)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C242F))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00B2FF))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
