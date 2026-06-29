package au.com.benji.robert.screens.aprs

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.models.AprsPacket
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import au.com.benji.robert.components.RobertMap
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AprsScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val packets by viewModel.aprsPackets.collectAsStateWithLifecycle()
    val location by viewModel.locationFlow.collectAsStateWithLifecycle()
    
    var isMapFullscreen by remember { mutableStateOf(false) }
    
    val aprsUrl = remember(location) {
        if (location != null) {
            "https://aprs.fi/#!lat=${location!!.first}&lng=${location!!.second}&z=11"
        } else {
            "https://aprs.fi/"
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Medium)
                .verticalScroll(rememberScrollState()),
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
                        .padding(top = 64.dp, end = 12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                }
            }

            Text(text = "Recent Packets Heard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            when {
                packets == null -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Large), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                packets!!.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.Large).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(Spacing.Small))
                            Text(
                                "No recent APRS packets heard in this area.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    for (packet in packets!!) {
                        PacketItem(packet)
                    }
                }
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

@Composable
fun PacketItem(packet: AprsPacket) {
    val timeAgo = remember(packet.timestamp) {
        val seconds = (System.currentTimeMillis() / 1000) - packet.timestamp
        when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = packet.callsign, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    packet.comment?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(text = timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.height(Spacing.Small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PacketDetail("Dist", "%.1f km".format(packet.distance))
                PacketDetail("Bear", "%d°".format(packet.bearing.toInt()))
                packet.speed?.let { PacketDetail("Speed", "%.0f km/h".format(it)) }
                packet.course?.let { PacketDetail("Crs", "%d°".format(it)) }
            }
        }
    }
}

@Composable
fun PacketDetail(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
