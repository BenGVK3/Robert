package au.com.benji.robert.screens.sdr

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.benji.robert.theme.Spacing
import kotlin.random.Random

@Composable
fun SdrScreen() {
    var frequency by remember { mutableStateOf(144.800) }
    var gain by remember { mutableStateOf(42f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "SDR Control Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        // Waterfall Simulation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            WaterfallCanvas()
        }

        // Frequency Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "TUNED FREQUENCY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%.3f MHz", frequency),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { frequency -= 0.025 }) { Icon(Icons.Default.Remove, contentDescription = null) }
                    Slider(
                        value = frequency.toFloat(),
                        onValueChange = { frequency = it.toDouble() },
                        valueRange = 144f..146f,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { frequency += 0.025 }) { Icon(Icons.Default.Add, contentDescription = null) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text("Gain Control", style = MaterialTheme.typography.labelMedium)
                    Slider(value = gain, onValueChange = { gain = it }, valueRange = 0f..50f)
                    Text("${gain.toInt()} dB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text("Mode", style = MaterialTheme.typography.labelMedium)
                    var expanded by remember { mutableStateOf(false) }
                    var selectedMode by remember { mutableStateOf("USB") }
                    
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(selectedMode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("USB", "LSB", "AM", "NFM", "CW").forEach { mode ->
                                DropdownMenuItem(text = { Text(mode) }, onClick = { selectedMode = mode; expanded = false })
                            }
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = { /* Connect SDR */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SettingsInputComponent, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CONNECT HARDWARE")
        }
    }
}

@Composable
fun WaterfallCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "waterfall")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Background noise
        for (i in 0..10) {
            val x = Random.nextFloat() * width
            drawCircle(
                color = Color.Blue.copy(alpha = 0.2f),
                radius = 2f,
                center = Offset(x, (phase % height))
            )
        }

        // Simulated signal peaks
        val peak1X = width * 0.3f
        val peak2X = width * 0.7f
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Cyan, Color.Blue, Color.Transparent),
                startY = 0f,
                endY = height
            ),
            topLeft = Offset(peak1X - 20f, 0f),
            size = androidx.compose.ui.geometry.Size(40f, height)
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Yellow, Color.Red, Color.Transparent),
                startY = 0f,
                endY = height
            ),
            topLeft = Offset(peak2X - 10f, 0f),
            size = androidx.compose.ui.geometry.Size(20f, height)
        )
    }
}
