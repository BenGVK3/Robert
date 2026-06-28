package au.com.benji.robert.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import au.com.benji.robert.theme.Spacing

@Composable
fun ToolsScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {

        Text(
            text = "Tools",
            style = MaterialTheme.typography.headlineMedium
        )

        ToolItem(
            icon = Icons.Default.Calculate,
            title = "Maidenhead Locator",
            description = "Calculate grid square from coordinates"
        )

        ToolItem(
            icon = Icons.Default.Map,
            title = "Prefix Map",
            description = "View country prefixes on a map"
        )

        ToolItem(
            icon = Icons.Default.Search,
            title = "Callsign Lookup",
            description = "Search for operator details"
        )
    }
}

@Composable
fun ToolItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* TODO: Navigate to specific tool */ }
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.Medium))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}