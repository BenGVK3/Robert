package au.com.benji.robert.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.*
import au.com.benji.robert.repository.BandPlanRepository
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.BandPlanViewModel
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import androidx.compose.ui.platform.LocalContext

@Composable
fun BandPlanScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val bandPlanRepository = remember { BandPlanRepository(context) }
    val viewModel: BandPlanViewModel = viewModel(
        factory = RobertViewModelFactory { BandPlanViewModel(bandPlanRepository, settingsRepository) }
    )

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bands by viewModel.filteredBands.collectAsStateWithLifecycle()
    val userCountry by viewModel.userCountry.collectAsStateWithLifecycle()
    val userLicenceClass by viewModel.userLicenceClass.collectAsStateWithLifecycle()
    
    var showFrequencyLookup by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search Bands (e.g. 40m, 7.1)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showFrequencyLookup = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Frequency Lookup")
                }
            }
        )

        Row(
            modifier = Modifier.padding(vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$userCountry - $userLicenceClass",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            contentPadding = PaddingValues(bottom = Spacing.Large)
        ) {
            items(bands) { band ->
                BandCard(band, userLicenceClass)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = band.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${band.frequencyRange.start} - ${band.frequencyRange.end} MHz",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            VisualBandChart(band, licenceId)
            
            Spacer(modifier = Modifier.height(Spacing.Medium))

            band.allocations.forEach { allocation ->
                AllocationItem(allocation, licenceId)
            }
            
            if (!band.notes.isNullOrBlank()) {
                Text(
                    text = band.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.Small),
                    color = MaterialTheme.colorScheme.outline
                )
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RestrictionIndicator(restriction)
        Spacer(modifier = Modifier.width(Spacing.Small))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = allocation.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${allocation.frequencyRange.start} - ${allocation.frequencyRange.end} MHz | ${allocation.modes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (powerLimit != null) {
            Text(text = powerLimit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RestrictionIndicator(restriction: RestrictionType) {
    val color = when (restriction) {
        RestrictionType.ALLOWED -> Color.Green
        RestrictionType.RESTRICTED -> Color.Yellow
        RestrictionType.NOT_PERMITTED -> Color.Red
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
    )
}

@Composable
fun VisualBandChart(band: Band, licenceId: String) {
    // Horizontal chart showing allocations
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val totalRange = band.frequencyRange.end - band.frequencyRange.start
        
        band.allocations.forEach { allocation ->
            val startOffset = (allocation.frequencyRange.start - band.frequencyRange.start) / totalRange
            val widthFactor = (allocation.frequencyRange.end - allocation.frequencyRange.start) / totalRange
            
            val restriction = allocation.licenceRestrictions[licenceId] ?: RestrictionType.NOT_PERMITTED
            val color = when (restriction) {
                RestrictionType.ALLOWED -> MaterialTheme.colorScheme.primary
                RestrictionType.RESTRICTED -> MaterialTheme.colorScheme.secondary
                RestrictionType.NOT_PERMITTED -> Color.Gray.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthFactor.toFloat())
                    .offset(x = (maxWidth * startOffset.toFloat()))
                    .background(color)
            )
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
        title = { Text("Frequency Lookup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                OutlinedTextField(
                    value = frequencyText,
                    onValueChange = { frequencyText = it },
                    label = { Text("Frequency (MHz)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (lookupResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(Spacing.Medium)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RestrictionIndicator(lookupResult.restriction)
                                Spacer(modifier = Modifier.width(Spacing.Small))
                                Text(text = lookupResult.bandName, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Allocation: ${lookupResult.allocationName}")
                            Text(text = "Modes: ${lookupResult.modes.joinToString(", ")}")
                            if (lookupResult.powerLimit != null) {
                                Text(text = "Power Limit: ${lookupResult.powerLimit}")
                            }
                        }
                    }
                } else if (frequencyText.isNotBlank()) {
                    Text("No band found for this frequency.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
