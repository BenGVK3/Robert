package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookMapScreen(
    onBack: () -> Unit,
    grid: String
) {
    // Constructing a map URL using a public grid locator service
    // This is a common practice for amateur radio apps
    val mapUrl = "https://levinecentral.com/ham/grid_square.php?Grid=$grid"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map: $grid", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            RobertMap(
                url = mapUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
