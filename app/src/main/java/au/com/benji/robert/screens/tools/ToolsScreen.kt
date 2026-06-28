package au.com.benji.robert.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.components.RobertTextField
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    var activeTool by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            if (activeTool != null) {
                TopAppBar(
                    title = { Text(activeTool!!, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { activeTool = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Radio Tools", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (activeTool == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.Medium)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    ToolCard(
                        title = "Maidenhead Locator",
                        description = "Current GPS Grid Square",
                        icon = Icons.Default.MyLocation,
                        onClick = { activeTool = "Maidenhead Locator" }
                    )

                    ToolCard(
                        title = "Callsign Lookup",
                        description = "Search operator details",
                        icon = Icons.Default.Search,
                        onClick = { activeTool = "Callsign Lookup" }
                    )

                    ToolCard(
                        title = "Prefix Map",
                        description = "Global prefix identifier",
                        icon = Icons.Default.Public,
                        onClick = { activeTool = "Prefix Map" }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(Spacing.Medium)) {
                    when (activeTool) {
                        "Maidenhead Locator" -> MaidenheadTool(viewModel)
                        "Callsign Lookup" -> CallsignLookupTool()
                        "Prefix Map" -> PrefixMapTool()
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(Spacing.Large))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun MaidenheadTool(viewModel: DashboardViewModel) {
    val location by viewModel.locationFlow.collectAsStateWithLifecycle()
    
    val lat = location?.first ?: -37.8136
    val lon = location?.second ?: 144.9631
    val grid = calculateMaidenhead(lat, lon)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(Spacing.ExtraLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YOUR GRID SQUARE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                Text(text = grid, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                Text("GPS POSITION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Latitude: $lat", style = MaterialTheme.typography.bodyMedium)
                Text("Longitude: $lon", style = MaterialTheme.typography.bodyMedium)
                if (location == null) {
                    Text("Note: Using default location (Melbourne)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))
        Button(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("UPDATE GPS LOCATION")
        }
    }
}

@Composable
fun CallsignLookupTool() {
    var callsign by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<LookupResult?>(null) }
    var searching by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = callsign,
            onValueChange = { callsign = it },
            label = { Text("Enter Callsign") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { 
                    searching = true
                    result = LookupResult(
                        name = "Benji",
                        qth = "Melbourne, Australia",
                        grid = "QF22og",
                        contacts = 42,
                        awards = listOf("Worked All VK", "Grid Squared", "DX Century Club", "VHF Enthusiast")
                    )
                    searching = false
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        if (result != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(text = callsign.uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Name: ${result!!.name}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Location: ${result!!.qth}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Grid: ${result!!.grid}", style = MaterialTheme.typography.bodyMedium)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(text = "QRZ STATS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Total Contacts: ${result!!.contacts}")
                    
                    Text(text = "AWARDS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        result!!.awards.forEach { award ->
                            AssistChip(onClick = {}, label = { Text(award) })
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Button(
                        onClick = { /* Open QRZ Profile */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("VIEW FULL QRZ PROFILE")
                    }
                }
            }
        }
    }
}

@Composable
fun PrefixMapTool() {
    var query by remember { mutableStateOf("") }
    val mapUrl = "https://www.dxcluster.info/telnet/prefix.php" 
    
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        RobertTextField(value = query, onValueChange = { query = it }, label = "Search Prefix (e.g. VK3, JA, W6)")
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            RobertMap(url = if (query.isEmpty()) mapUrl else "$mapUrl?q=$query", modifier = Modifier.fillMaxSize())
        }
    }
}

data class LookupResult(
    val name: String,
    val qth: String,
    val grid: String,
    val contacts: Int,
    val awards: List<String>
)

fun calculateMaidenhead(lat: Double, lon: Double): String {
    val longitude = lon + 180
    val latitude = lat + 90

    val lonField = (longitude / 20).toInt()
    val latField = (latitude / 10).toInt()

    val lonSquare = ((longitude % 20) / 2).toInt()
    val latSquare = (latitude % 10).toInt()

    val lonSub = (((longitude % 2) / (2.0 / 24.0))).toInt()
    val latSub = ((latitude % 1) / (1.0 / 24.0)).toInt()

    return "${('A' + lonField)}${('A' + latField)}$lonSquare$latSquare${('a' + lonSub)}${('a' + latSub)}"
}
