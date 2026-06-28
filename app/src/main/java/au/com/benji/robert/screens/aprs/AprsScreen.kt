package au.com.benji.robert.screens.aprs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.theme.Spacing

@Composable
fun AprsScreen() {
    val packets = remember {
        listOf(
            AprsPacket("VK3ABC-9", "144.800", "En route to Ballarat", "5 mins ago"),
            AprsPacket("VK2XYZ-7", "144.800", "Fixed Station - Sydney", "12 mins ago"),
            AprsPacket("ZL1QB-1", "144.800", "Walking near Auckland", "1 min ago"),
            AprsPacket("VK5DEF-12", "144.800", "Weather Station QTH", "30 mins ago")
        )
    }
    
    var isMapFullscreen by remember { mutableStateOf(false) }
    val aprsUrl = "https://aprs.fi/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        Text(text = "APRS Tactical Map", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                RobertMap(url = aprsUrl, modifier = Modifier.fillMaxSize())
            }
            
            IconButton(
                onClick = { isMapFullscreen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
            }
        }

        Text(text = "Recent Packets Heard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(packets) { packet ->
                PacketItem(packet)
            }
        }
    }

    if (isMapFullscreen) {
        Dialog(
            onDismissRequest = { isMapFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    RobertMap(url = aprsUrl, modifier = Modifier.fillMaxSize())
                    
                    IconButton(
                        onClick = { isMapFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

data class AprsPacket(
    val callsign: String,
    val frequency: String,
    val comment: String,
    val time: String
)

@Composable
fun PacketItem(packet: AprsPacket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = packet.callsign, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = packet.comment, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = packet.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
