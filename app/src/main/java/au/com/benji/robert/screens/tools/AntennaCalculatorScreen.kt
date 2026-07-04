package au.com.benji.robert.screens.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.benji.robert.theme.Spacing
import java.util.Locale

enum class AntennaType(val displayName: String) {
    DIPOLE("1/2 Wave Dipole"),
    EFHW("End-Fed Half Wave (EFHW)"),
    VERTICAL("1/4 Wave Vertical")
}

enum class FreqUnit(val factor: Double) {
    Hz(1.0),
    kHz(1000.0),
    MHz(1000000.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntennaCalculatorScreen() {
    var frequencyInput by remember { mutableStateOf("14.200") }
    var selectedUnit by remember { mutableStateOf(FreqUnit.MHz) }
    var selectedAntenna by remember { mutableStateOf(AntennaType.DIPOLE) }
    
    val frequencyMhz = try {
        (frequencyInput.toDoubleOrNull() ?: 0.0) * (selectedUnit.factor / 1_000_000.0)
    } catch (e: Exception) { 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.Medium)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        // Input Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            OutlinedTextField(
                value = frequencyInput,
                onValueChange = { frequencyInput = it },
                label = { Text("Frequency", fontSize = 12.sp) },
                modifier = Modifier.weight(1.5f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            var unitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedUnit.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit", fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    FreqUnit.entries.forEach { unit ->
                        DropdownMenuItem(text = { Text(unit.name) }, onClick = { selectedUnit = unit; unitExpanded = false })
                    }
                }
            }
        }

        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedAntenna.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Antenna Type", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                AntennaType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.displayName) }, onClick = { selectedAntenna = type; typeExpanded = false })
                }
            }
        }

        if (frequencyMhz > 0) {
            CalculationResults(frequencyMhz, selectedAntenna)
            AntennaDiagram(selectedAntenna)
        }
    }
}

@Composable
fun CalculationResults(mhz: Double, type: AntennaType) {
    // Constants for calculation (Metric)
    val velocityFactor = 0.95 // Common for wire antennas
    val speedOfLight = 299.79 // Mm/s
    
    // 1/2 Wave Wavelength in meters = 142.5 / f(MHz) for wire
    // Or more precisely: (Speed of Light / Frequency) * Velocity Factor
    val fullWavelength = speedOfLight / mhz
    val halfWaveTotal = 143.0 / mhz // Standard formula for feet is 468/f, metric equivalent is ~143/f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ResultRow("Full Wavelength (λ)", String.format(Locale.getDefault(), "%.3f m", fullWavelength), "Formula: 300 / f")
            
            val totalLength = when(type) {
                AntennaType.DIPOLE -> halfWaveTotal
                AntennaType.EFHW -> halfWaveTotal
                AntennaType.VERTICAL -> halfWaveTotal / 2.0
            }
            
            ResultRow("Total Wire Length", String.format(Locale.getDefault(), "%.2f m", totalLength), 
                if (type == AntennaType.VERTICAL) "Formula: 71.5 / f (1/4 λ)" else "Formula: 143 / f (1/2 λ)")

            if (type == AntennaType.DIPOLE) {
                ResultRow("Length Each Leg", String.format(Locale.getDefault(), "%.2f m", totalLength / 2.0), "Total / 2")
            }

            if (type == AntennaType.EFHW) {
                Text(
                    text = "Requires 49:1 or 64:1 Unun. Place common-mode choke after ~2-3m of coax for counterpoise effect, or add 0.05λ wire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (type == AntennaType.DIPOLE) {
                Text(
                    text = "Place a 1:1 Balun/Choke at the center feed point to prevent common-mode current on coax.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Requires at least 3-4 radials (counterpoise) of same length as vertical element (~${String.format(Locale.getDefault(), "%.2f", totalLength)}m each).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, formula: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(formula, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun AntennaDiagram(type: AntennaType) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = Spacing.Small)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            val w = size.width
            val h = size.height
            
            when (type) {
                AntennaType.DIPOLE -> {
                    // Center point
                    drawCircle(Color.Gray, 5f, center = Offset(w/2, h/2))
                    // Left leg
                    drawLine(primary, Offset(w/2 - 10f, h/2), Offset(w*0.1f, h*0.4f), strokeWidth = 5f, cap = StrokeCap.Round)
                    // Right leg
                    drawLine(primary, Offset(w/2 + 10f, h/2), Offset(w*0.9f, h*0.4f), strokeWidth = 5f, cap = StrokeCap.Round)
                    // Coax
                    drawLine(secondary, Offset(w/2, h/2 + 5f), Offset(w/2, h), strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))
                    // Labels
                    // No easy way to draw text in Canvas directly without native canvas, so we'll skip text labels inside or use helper.
                }
                AntennaType.EFHW -> {
                    // Unun box
                    drawRect(Color.Gray, Offset(w*0.1f, h*0.7f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                    // Wire
                    drawLine(primary, Offset(w*0.1f + 15f, h*0.7f), Offset(w*0.9f, h*0.2f), strokeWidth = 5f, cap = StrokeCap.Round)
                    // Coax
                    drawLine(secondary, Offset(w*0.1f + 15f, h*0.7f + 30f), Offset(w*0.1f + 15f, h), strokeWidth = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))
                }
                AntennaType.VERTICAL -> {
                    // Ground line
                    drawLine(Color.Gray, Offset(w*0.2f, h*0.8f), Offset(w*0.8f, h*0.8f), strokeWidth = 2f)
                    // Radiator
                    drawLine(primary, Offset(w/2, h*0.8f), Offset(w/2, h*0.1f), strokeWidth = 5f, cap = StrokeCap.Round)
                    // Radials
                    drawLine(primary, Offset(w/2, h*0.8f), Offset(w*0.3f, h*0.9f), strokeWidth = 3f)
                    drawLine(primary, Offset(w/2, h*0.8f), Offset(w*0.7f, h*0.9f), strokeWidth = 3f)
                }
            }
        }
        
        // Diagram Legend overlay
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopStart) {
            Text(
                text = when(type) {
                    AntennaType.DIPOLE -> "Center-fed Dipole (1/2 λ)"
                    AntennaType.EFHW -> "End-Fed Half Wave (1/2 λ)"
                    AntennaType.VERTICAL -> "1/4 λ Vertical with Radials"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
