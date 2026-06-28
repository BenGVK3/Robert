package au.com.benji.robert.screens.propagation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.benji.robert.models.SolarData
import au.com.benji.robert.repository.SolarDataRepository
import au.com.benji.robert.theme.Spacing

@Composable
fun PropagationScreen() {
    val repository = remember { SolarDataRepository() }
    val solarData by repository.getSolarData().collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {

        Text(
            text = "Propagation",
            style = MaterialTheme.typography.headlineMedium
        )

        solarData?.let { data ->
            SolarDataCard(data)
            
            Text(
                text = "Band Conditions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Spacing.Small)
            )
            
            val bands = listOf("80m", "40m", "20m", "15m", "10m")
            bands.forEach { band ->
                BandConditionRow(band, "Good")
            }
        } ?: CircularProgressIndicator()
    }
}

@Composable
fun SolarDataCard(data: SolarData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("SFI", data.solarFlux.toString())
                MetricItem("K-Index", data.kIndex.toString())
                MetricItem("A-Index", data.aIndex.toString())
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(text = "Maximum Usable Frequency: ${data.muf}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun BandConditionRow(band: String, condition: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = band, style = MaterialTheme.typography.bodyLarge)
        Text(text = condition, color = MaterialTheme.colorScheme.primary)
    }
}