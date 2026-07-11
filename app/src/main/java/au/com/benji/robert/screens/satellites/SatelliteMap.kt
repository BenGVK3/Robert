package au.com.benji.robert.screens.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.network.SatellitePosition

@Composable
fun SatelliteMap(
    position: SatellitePosition?,
    selectedId: String,
    userLat: Double,
    userLon: Double,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapUrl = remember(selectedId, userLat, userLon) {
        // Updated URL to request a large size and ensuring parameters are passed correctly
        "https://www.n2yo.com/widgets/widget-tracker.php?s=$selectedId&size=large&all=1&lat=$userLat&lon=$userLon"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        // Using the project's stable WebView-based map
        RobertMap(
            url = mapUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay status badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (position?.isVisible == true) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                )
                Text(
                    text = position?.name ?: "Searching...",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Map FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}
