package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import au.com.benji.robert.models.*
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookStatsScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val qsos by viewModel.qsos.collectAsStateWithLifecycle()
    val awards by viewModel.awardsProgress.collectAsStateWithLifecycle()
    val dailyActivity by viewModel.dailyActivity.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("ANALYTICS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { showExportSheet = true }) { Icon(Icons.Default.IosShare, "Export") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 1. Career Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                MetricSmallCard("TODAY", qsos.count { isSameDay(it.timestamp) }.toString(), Icons.Default.Today, Modifier.weight(1f))
                MetricSmallCard("TOTAL", qsos.size.toString(), Icons.Default.AllInclusive, Modifier.weight(1f))
                MetricSmallCard("DXCC", (awards["DXCC"] ?: 0).toString(), Icons.Default.Public, Modifier.weight(1f))
            }

            // 2. Activity Chart
            StatsSection(title = "LOGGING HISTORY (7D)") {
                ActivityChart(dailyActivity)
            }

            // 3. Award Progress
            StatsSection(title = "MAJOR AWARDS") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    AwardProgressCard("DXCC 100", (awards["DXCC"] ?: 0), 100, Modifier.weight(1f))
                    AwardProgressCard("WAC (6)", (awards["Continents"] ?: 0), 6, Modifier.weight(1f))
                }
            }

            // 4. Distribution
            StatsSection(title = "TOP BANDS") {
                val bands = qsos.groupBy { it.band }.mapValues { it.value.size }.toList().sortedByDescending { it.second }
                DistributionCard(bands, qsos.size)
            }

            StatsSection(title = "TOP MODES") {
                val modes = qsos.groupBy { it.mode }.mapValues { it.value.size }.toList().sortedByDescending { it.second }
                DistributionCard(modes, qsos.size)
            }
            
            Spacer(Modifier.height(Spacing.ExtraLarge))
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

fun exportDestinationSelection(context: android.content.Context, filename: String, content: String) {
    val options = arrayOf("Save to Downloads", "Send via Email")
    android.app.AlertDialog.Builder(context)
        .setTitle("Export Destination")
        .setItems(options) { _, which ->
            when (which) {
                0 -> saveFileToDownloads(context, filename, content)
                1 -> shareFile(context, filename, content)
            }
        }
        .show()
}

fun saveFileToDownloads(context: android.content.Context, filename: String, content: String) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, filename)
            file.writeText(content)
            android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to save: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun MetricSmallCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
        Column(modifier = Modifier.padding(Spacing.Medium), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = RobertColors.Primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = RobertColors.Primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary, fontSize = 8.sp)
        }
    }
}

@Composable
fun StatsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = RobertColors.TextSecondary)
        content()
    }
}

@Composable
fun ActivityChart(activity: List<Pair<String, Int>>) {
    val maxCount = activity.map { it.second }.maxOrNull() ?: 1
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
        Row(
            modifier = Modifier.padding(Spacing.Medium).height(100.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            activity.forEach { (label, count) ->
                val ratio = count.toFloat() / maxCount.coerceAtLeast(1)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio.coerceIn(0.1f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(RobertColors.Primary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label.take(3), style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary, fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
fun AwardProgressCard(label: String, current: Int, goal: Int, modifier: Modifier) {
    val progress = current.toFloat() / goal.coerceAtLeast(1)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
        Column(modifier = Modifier.padding(Spacing.Medium), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(48.dp), color = RobertColors.Primary, strokeWidth = 4.dp, trackColor = RobertColors.Surface)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DistributionCard(items: List<Pair<String, Int>>, total: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
        Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            items.take(5).forEach { (label, count) ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = RobertColors.Primary)
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = RobertColors.Primary,
                        trackColor = RobertColors.Surface
                    )
                }
            }
        }
    }
}

@Composable
fun ExportBottomSheetContent(onExportAdif: () -> Unit, onExportCabrillo: () -> Unit, onExportCsv: () -> Unit) {
    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text("EXPORT LOGS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(Spacing.Small))
        
        ExportItem(title = "ADIF (.adi)", icon = Icons.Default.Description, onClick = onExportAdif)
        ExportItem(title = "Cabrillo (.log)", icon = Icons.Default.EmojiEvents, onClick = onExportCabrillo)
        ExportItem(title = "CSV (.csv)", icon = Icons.Default.TableChart, onClick = onExportCsv)
        
        Spacer(Modifier.height(Spacing.Large))
    }
}

@Composable
fun ExportItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RobertColors.Primary)
            Spacer(Modifier.width(Spacing.Medium))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

private fun isSameDay(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
}

private fun shareFile(context: android.content.Context, filename: String, content: String) {
    try {
        val file = java.io.File(context.cacheDir, filename)
        file.writeText(content)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Logbook File"))
    } catch (e: Exception) {
        android.util.Log.e("Stats", "Error sharing file", e)
    }
}
