package au.com.benji.robert.screens.moon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.models.MoonData
import au.com.benji.robert.components.StatusChip
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonScreen(
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    val moonData by viewModel.moonData.collectAsStateWithLifecycle()
    var showLocator by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.size(56.dp).align(Alignment.Center),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = au.com.benji.robert.R.drawable.moon1),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { showLocator = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Explore, "Locate Moon", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(Spacing.Small))
                Text(
                    text = "Moon Center",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "EME and Moonbounce tracking",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF03DAC6),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = Spacing.Small)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(Spacing.Small))
            }
            item {
                CurrentMoonCard(moonData)
            }

            item {
                MoonPositionCard(moonData)
            }

            item {
                EmeInformationCard(moonData)
            }

            item {
                EmeOperatingConditionsCard(moonData)
            }

            item {
                LunarCalendarCard(moonData)
            }

            item {
                QuickFactsCard(moonData)
            }
        }
    }

    if (showLocator) {
        MoonLocatorDialog(
            azimuth = moonData.azimuth,
            altitude = moonData.altitude,
            onDismiss = { showLocator = false }
        )
    }
}

@Composable
fun MoonLocatorDialog(
    azimuth: Double,
    altitude: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rotationMatrix = remember { FloatArray(9) }
    var orientationAngles = remember { FloatArray(3) }
    var currentAzimuth by remember { mutableFloatStateOf(0f) }
    var currentPitch by remember { mutableFloatStateOf(0f) }
    var currentRoll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                if (event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                    android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    android.hardware.SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    
                    // azimuth (0 to 360)
                    currentAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (currentAzimuth < 0) currentAzimuth += 360
                    
                    // pitch (-90 to 90)
                    currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    
                    // roll (-180 to 180)
                    currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.9f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MOON LOCATOR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(Spacing.Large))

                    // 3D-ish target
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Circle (Compass)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                            
                            // North marker
                            val nAngle = Math.toRadians((currentAzimuth).toDouble())
                            // ... simple logic for now, showing relative moon position
                        }

                        // Target Moon Icon
                        val deltaAz = (azimuth - currentAzimuth).let { if (it < -180) it + 360 else if (it > 180) it - 360 else it }
                        // deltaAlt: phone level is usually 0 pitch? or 90?
                        // Let's assume phone held vertically: pitch 0 is vertical? No, pitch is usually around X axis.
                        // For simplicity, let's use a 2D projection for now that "feels" like locating it.
                        
                        val xOffset = (deltaAz / 45.0 * 140.dp.value).coerceIn(-140.0, 140.0)
                        val yOffset = ((altitude - currentPitch) / 45.0 * 140.dp.value).coerceIn(-140.0, 140.0)

                        Icon(
                            imageVector = Icons.Default.Brightness2,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .offset(x = xOffset.dp, y = (-yOffset).dp),
                            tint = if (Math.abs(deltaAz) < 5 && Math.abs(altitude - currentPitch) < 5) Color(0xFF03DAC6) else Color.White
                        )
                        
                        // Crosshair
                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
                        Divider(modifier = Modifier.width(60.dp), color = Color.White.copy(alpha = 0.5f))
                        Divider(modifier = Modifier.height(60.dp).width(1.dp), color = Color.White.copy(alpha = 0.5f))
                    }

                    Spacer(Modifier.height(Spacing.Large))
                    
                    Text(
                        text = "Target: Az ${azimuth.toInt()}° Alt ${altitude.toInt()}°",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device: Az ${currentAzimuth.toInt()}° Alt ${currentPitch.toInt()}°",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(Modifier.height(Spacing.ExtraLarge))
                    
                    Button(onClick = onDismiss) {
                        Text("DONE")
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentMoonCard(data: MoonData) {
    val context = LocalContext.current
    var currentImageIndex by remember { mutableIntStateOf(2) }
    
    val targetIndex = remember(data.age) {
        val phaseNum = (data.age / 29.53).coerceIn(0.0, 1.0)
        val index = when {
            phaseNum < 0.02 || phaseNum > 0.98 -> 2 // New Moon (02)
            phaseNum < 0.24 -> 3 + ((phaseNum - 0.02) / 0.22 * 4).toInt().coerceIn(0, 4) // Waxing Crescent (03-07)
            phaseNum < 0.26 -> 8 // First Quarter (08)
            phaseNum < 0.49 -> 9 + ((phaseNum - 0.26) / 0.23 * 6).toInt().coerceIn(0, 6) // Waxing Gibbous (09-15)
            phaseNum < 0.51 -> 16 // Full Moon (16)
            phaseNum < 0.74 -> 17 + ((phaseNum - 0.51) / 0.23 * 5).toInt().coerceIn(0, 5) // Waning Gibbous (17-22)
            phaseNum < 0.76 -> 23 // Third Quarter (23)
            else -> 24 + ((phaseNum - 0.76) / 0.24 * 6).toInt().coerceIn(0, 6) // Waning Crescent (24-30)
        }
        index
    }

    val isDataLoaded = remember(data.phaseName) { data.phaseName != "---" }

    LaunchedEffect(isDataLoaded) {
        if (isDataLoaded) {
            // Animation: One full rotation + rotation to target
            for (i in 2..30) {
                currentImageIndex = i
                delay(40)
            }
            for (i in 2..targetIndex) {
                currentImageIndex = i
                delay(40)
            }
        }
    }

    val moonResId = remember(currentImageIndex) {
        context.resources.getIdentifier(
            "moon${String.format("%02d", currentImageIndex)}",
            "drawable",
            context.packageName
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isDataLoaded && moonResId != 0) {
                    Image(
                        painter = painterResource(id = moonResId),
                        contentDescription = "Current Moon Phase",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Placeholder stays black while loading or if images are missing
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Text(
                text = data.phaseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${data.illumination}% Illuminated",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            if (data.lastUpdated > 0) {
                Text(
                    text = "Last updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(data.lastUpdated))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MoonMetric("Age", String.format("%.1f d", data.age))
                MoonMetric("Distance", String.format("%,.0f km", data.distanceKm))
                MoonMetric("Size", String.format("%.2f°", data.angularSize))
            }
        }
    }
}

@Composable
fun MoonPositionCard(data: MoonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text("Moon Position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    PositionItem("Altitude", String.format("%.2f°", data.altitude))
                    PositionItem("Azimuth", String.format("%.2f°", data.azimuth))
                }
                Column {
                    PositionItem("Rise", data.riseTime)
                    PositionItem("Set", data.setTime)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = Spacing.Small))
            
            Text(
                text = if (data.isVisible) "Moon is currently VISIBLE" else "Moon is below horizon",
                style = MaterialTheme.typography.labelSmall,
                color = if (data.isVisible) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmeInformationCard(data: MoonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text("EME Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            GridInfoItem("Declination", String.format("%.2f°", data.declination))
            GridInfoItem("Radial Velocity", String.format("%.2f m/s", data.radialVelocity))
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text("Estimated Doppler Shift", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DopplerItem("144M", data.doppler144)
                DopplerItem("432M", data.doppler432)
                DopplerItem("1296M", data.doppler1296)
            }
            
            Divider(modifier = Modifier.padding(vertical = Spacing.Small))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    GridInfoItem("Path Loss (432M)", String.format("%.1f dB", data.pathLoss))
                    GridInfoItem("Sky Temp", "--- K")
                }
                Column {
                    GridInfoItem("One-way Delay", String.format("%.2f s", data.oneWayDelay))
                    GridInfoItem("Round-trip Delay", String.format("%.2f s", data.roundTripDelay))
                }
            }
        }
    }
}

@Composable
fun EmeOperatingConditionsCard(data: MoonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text("EME Operating Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Current Status", style = MaterialTheme.typography.labelSmall)
                    val ratingColor = when (data.emeRating) {
                        "Good" -> Color(0xFF4CAF50)
                        "Fair" -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                    Text(data.emeRating.uppercase(), color = ratingColor, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                
                Surface(
                    color = if (data.isVisible) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (data.isVisible) "MOON UP" else "MOON DOWN",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (data.isVisible) Color(0xFF4CAF50) else Color.Red,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LunarCalendarCard(data: MoonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text("Lunar Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.Small))
            CalendarItem("Next New Moon", data.nextNewMoon)
            CalendarItem("Next Full Moon", data.nextFullMoon)
        }
    }
}

@Composable
fun QuickFactsCard(data: MoonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text("Quick Facts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Spacing.Small))
            FactItem("Average Distance", "384,400 km")
            FactItem("Orbital Period", "27.32 days")
            FactItem("Synodic Month", "29.53 days")
        }
    }
}

@Composable
fun MoonMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PositionItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GridInfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DopplerItem(band: String, value: Double) {
    Column {
        Text(band, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(String.format("%+.0f Hz", value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CalendarItem(label: String, date: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FactItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
