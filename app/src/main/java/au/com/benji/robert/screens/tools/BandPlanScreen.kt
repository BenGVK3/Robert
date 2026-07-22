package au.com.benji.robert.screens.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.models.*
import au.com.benji.robert.repository.BandPlanRepository
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.BandPlanViewModel
import au.com.benji.robert.viewmodel.RobertViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandPlanScreen(
    onBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(DatabaseModule.cacheDao(context)) }
    val bandPlanRepository = remember { BandPlanRepository(context) }
    val viewModel: BandPlanViewModel = viewModel(
        factory = RobertViewModelFactory { BandPlanViewModel(bandPlanRepository, settingsRepository) }
    )

    val onlyUsable by viewModel.showOnlyUsableBands.collectAsStateWithLifecycle()
    val bands by viewModel.filteredBands.collectAsStateWithLifecycle()
    val userLicenceClass by viewModel.userLicenceClass.collectAsStateWithLifecycle()
    
    var showFrequencyLookup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        // Custom Header (replaces Scaffold topBar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "OFFICIAL BAND PLAN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "WIA AUSTRALIA 2026",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            IconButton(onClick = { showFrequencyLookup = true }) {
                Icon(Icons.Default.MyLocation, contentDescription = "Frequency Lookup", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Medium)
        ) {
            // Filter Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.Small),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = userLicenceClass.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Show Only Usable",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = onlyUsable,
                            onCheckedChange = { viewModel.toggleOnlyUsableBands(it) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }

            if (bands.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No usable bands found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    contentPadding = PaddingValues(bottom = Spacing.Medium)
                ) {
                    items(bands) { band ->
                        BandCard(band, userLicenceClass)
                    }
                }
            }
        }
    }

    if (showFrequencyLookup) {
        FrequencyLookupDialog(viewModel) { showFrequencyLookup = false }
    }
}

@Composable
fun BandCard(band: Band, licenceId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            // Band Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = band.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Amateur Secondary Service",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${band.frequencyRange.start} - ${band.frequencyRange.end} MHz",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            // The Graph
            VisualBandChart(band, licenceId)
            
            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Allocations
            Text(
                text = "SUB-ALLOCATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(Spacing.Small))

            band.allocations.forEach { allocation ->
                AllocationItem(allocation, licenceId)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
            
            if (!band.notes.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.padding(top = Spacing.Small),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.Small),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = band.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllocationItem(allocation: BandAllocation, licenceId: String) {
    val restriction = allocation.licenceRestrictions[licenceId] ?: RestrictionType.NOT_PERMITTED
    val powerLimit = allocation.powerLimits?.get(licenceId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RestrictionIndicator(restriction)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = allocation.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (powerLimit != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = powerLimit,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Text(
                text = "${allocation.frequencyRange.start} - ${allocation.frequencyRange.end} MHz",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            
            if (!allocation.description.isNullOrEmpty()) {
                Text(
                    text = allocation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // Mode Badges
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            allocation.modes.take(2).forEach { mode ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = mode,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun RestrictionIndicator(restriction: RestrictionType) {
    val color = when (restriction) {
        RestrictionType.ALLOWED -> Color(0xFF4CAF50)
        RestrictionType.RESTRICTED -> Color(0xFFFFC107)
        RestrictionType.NOT_PERMITTED -> Color(0xFFF44336)
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
fun VisualBandChart(band: Band, licenceId: String) {
    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            val totalRange = band.frequencyRange.end - band.frequencyRange.start
            
            band.allocations.forEach { allocation ->
                val startOffset = (allocation.frequencyRange.start - band.frequencyRange.start) / totalRange
                val widthFactor = (allocation.frequencyRange.end - allocation.frequencyRange.start) / totalRange
                
                val restriction = allocation.licenceRestrictions[licenceId] ?: RestrictionType.NOT_PERMITTED
                
                val baseColor = when {
                    allocation.name.contains("CW", true) -> Color(0xFF2196F3)
                    allocation.name.contains("Digital", true) || allocation.name.contains("Data", true) -> Color(0xFF9C27B0)
                    allocation.name.contains("SSB", true) || allocation.name.contains("All Modes", true) -> Color(0xFF4CAF50)
                    allocation.name.contains("FM", true) -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }

                val finalColor = if (restriction == RestrictionType.NOT_PERMITTED) {
                    Color.Gray.copy(alpha = 0.3f)
                } else baseColor

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(widthFactor.toFloat())
                        .offset(x = (maxWidth * startOffset.toFloat()))
                        .background(
                            Brush.verticalGradient(
                                listOf(finalColor.copy(alpha = 0.8f), finalColor)
                            )
                        )
                        .border(0.5.dp, Color.Black.copy(alpha = 0.1f))
                ) {
                    if (widthFactor > 0.1) {
                        Text(
                            text = allocation.modes.firstOrNull() ?: "",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            color = if (restriction == RestrictionType.NOT_PERMITTED) Color.Gray else Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        
        // Frequency Markers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(band.frequencyRange.start.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
            Text(band.frequencyRange.end.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 9.sp)
        }
    }
}

@Composable
fun FrequencyLookupDialog(viewModel: BandPlanViewModel, onDismiss: () -> Unit) {
    var frequencyText by remember { mutableStateOf("") }
    val lookupResult = remember(frequencyText) {
        frequencyText.toDoubleOrNull()?.let { viewModel.lookupFrequency(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Frequency Checker") 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(
                    "Enter a frequency in MHz to check allocation and license permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = frequencyText,
                    onValueChange = { frequencyText = it },
                    label = { Text("Frequency (MHz)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                if (lookupResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(Spacing.Medium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RestrictionIndicator(lookupResult.restriction)
                                Spacer(modifier = Modifier.width(Spacing.Small))
                                Text(
                                    text = lookupResult.bandName, 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = "Allocation: ${lookupResult.allocationName}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Permitted: ${lookupResult.modes.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            
                            if (lookupResult.powerLimit != null) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Limit: ${lookupResult.powerLimit}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else if (frequencyText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("No allocation found.", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE", fontWeight = FontWeight.Black) }
        }
    )
}
