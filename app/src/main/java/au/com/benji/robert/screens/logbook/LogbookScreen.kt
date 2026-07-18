package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.R
import au.com.benji.robert.components.RobertHeader
import au.com.benji.robert.models.*
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    paddingValues: PaddingValues,
    onNavigateToLogging: () -> Unit = {},
    onNavigateToActivation: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPileUp: () -> Unit = {},
    onNavigateToAllLogs: () -> Unit = {},
    viewModel: LogbookViewModel = viewModel()
) {
    val qsos by viewModel.qsos.collectAsStateWithLifecycle()
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()
    val sessionQsoCount by viewModel.sessionQsos.collectAsStateWithLifecycle()
    val elapsedTime by viewModel.elapsedTime.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val pileUpQueue by viewModel.pileUpQueue.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium)
                    .padding(top = Spacing.Small)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    RobertHeader(
                        title = "Logbook",
                        description = "Track your contacts and activations",
                        iconRes = R.drawable.logbook1,
                        isCentered = true,
                        isHorizontal = true,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { showExportSheet = true }) { Icon(Icons.Default.FileUpload, "Export") }
                    IconButton(onClick = onNavigateToStats) { Icon(Icons.Default.BarChart, "Stats") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
                HorizontalDivider(color = RobertColors.TextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(top = Spacing.Small))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 1. Statistics Strip
            item {
                StatsStrip(stats)
            }

            // 2. Active Activation Banner (Dynamic)
            if (activeAct != null) {
                item {
                    ActivationBanner(
                        activeAct!!, 
                        sessionQsoCount.size, 
                        elapsedTime,
                        onPause = { viewModel.pauseActivation() },
                        onFinish = { viewModel.finishActivation() },
                        onPileUp = onNavigateToPileUp,
                        pileUpCount = pileUpQueue.size,
                        onClick = onNavigateToActivation
                    )
                }
            }

            // 3. Primary Actions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    LargeActionButton(
                        title = "Log QSO",
                        icon = Icons.Default.Add,
                        color = RobertColors.Primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLogging
                    )
                    
                    if (activeAct == null) {
                        LargeActionButton(
                            title = "Activate",
                            icon = Icons.Default.Terrain,
                            color = RobertColors.StatusGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToActivation
                        )
                    }
                }
            }

            // 4. Recent Contacts
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("RECENT CONTACTS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onNavigateToAllLogs) { 
                        Text("VIEW ALL")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (qsos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))) {
                        Box(modifier = Modifier.padding(Spacing.ExtraLarge).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No contacts logged yet.", color = RobertColors.TextSecondary)
                        }
                    }
                }
            } else {
                items(qsos.take(15), key = { it.id }) { qso ->
                    QsoSwipeItem(
                        qso, 
                        onDuplicate = { viewModel.duplicateQso(qso) }, 
                        onDelete = { viewModel.deleteQso(qso) },
                        onClick = {
                            viewModel.editQso(qso)
                            onNavigateToLogging()
                        }
                    )
                }
            }
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            ExportBottomSheetContent(
                onExportAdif = {
                    val adif = viewModel.exportLogs()
                    exportDestinationSelection(context, "logbook_${System.currentTimeMillis()}.adi", adif)
                    showExportSheet = false
                },
                onExportCabrillo = {
                    val cab = viewModel.exportCabrillo()
                    exportDestinationSelection(context, "contest_${System.currentTimeMillis()}.log", cab)
                    showExportSheet = false
                },
                onExportCsv = {
                    val csv = viewModel.exportCsv()
                    exportDestinationSelection(context, "logbook_${System.currentTimeMillis()}.csv", csv)
                    showExportSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QsoSwipeItem(qso: Qso, onDuplicate: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDuplicate()
                    false 
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> RobertColors.StatusOrange
                SwipeToDismissBoxValue.EndToStart -> RobertColors.StatusRed
                else -> Color.Transparent
            }
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.CopyAll else Icons.Default.Delete
            
            Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = alignment) {
                Icon(icon, null, tint = Color.White)
            }
        }
    ) {
        Box(modifier = Modifier.clickable { onClick() }) {
            QsoRowItem(qso)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllLogsScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredQsos by viewModel.filteredQsos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search logs...") },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(filteredQsos, key = { it.id }) { qso ->
                QsoSwipeItem(
                    qso, 
                    onDuplicate = { viewModel.duplicateQso(qso) }, 
                    onDelete = { viewModel.deleteQso(qso) },
                    onClick = {
                        viewModel.editQso(qso)
                        onBack() // Or navigate to logging - but edit typically goes to the entry screen
                    }
                )
            }
        }
    }
}

@Composable
fun StatsStrip(stats: LogbookStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Today", value = stats.today.toString())
            StatItem(label = "DXCC", value = stats.dxcc.toString())
            StatItem(label = "Top Band", value = stats.topBand)
            StatItem(label = "Total", value = stats.total.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = RobertColors.Primary)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary, fontSize = 9.sp)
    }
}

@Composable
fun LargeActionButton(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = if (color == RobertColors.Accent) Color.Black else Color.White),
        contentPadding = PaddingValues(Spacing.Small)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(2.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun ActivationBanner(
    active: ActiveActivation, 
    count: Int, 
    timeMillis: Long,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onPileUp: () -> Unit,
    pileUpCount: Int,
    onClick: () -> Unit
) {
    val duration = String.format(Locale.US, "%02d:%02d:%02d", (timeMillis/3600000), (timeMillis/60000)%60, (timeMillis/1000)%60)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = RobertColors.Primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terrain, null, tint = RobertColors.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("${active.type}: ${active.reference}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(duration, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(Spacing.Small))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                Text("${count} QSOs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (active.currentBand.isNotEmpty()) Text(active.currentBand, style = MaterialTheme.typography.bodyMedium)
                if (active.currentMode.isNotEmpty()) Text(active.currentMode, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(Modifier.height(Spacing.Medium))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(
                    onClick = onPause, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), contentColor = RobertColors.TextPrimary),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(if (active.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(if (active.isPaused) "RESUME" else "PAUSE", fontSize = 10.sp, maxLines = 1)
                }
                Button(
                    onClick = onPileUp, 
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Accent, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PILE-UP ($pileUpCount)", fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                Button(
                    onClick = onFinish, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusRed),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("FINISH", fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun QsoRowItem(qso: Qso) {
    val time = SimpleDateFormat("HH:mm'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(qso.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)
    ) {
        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(qso.callWorked, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RobertColors.Primary)
                Text("${qso.band} • ${qso.mode} • S:${qso.rstSent} R:${qso.rstReceived}", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                val ref = qso.potaRef.ifEmpty { qso.sotaRef.ifEmpty { qso.wwffRef } }
                if (ref.isNotEmpty()) {
                    Surface(color = RobertColors.Primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(ref, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = RobertColors.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
