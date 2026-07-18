package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.*
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookEntryScreen(
    onBack: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
    viewModel: LogbookViewModel = viewModel()
) {
    val qso by viewModel.currentQso.collectAsStateWithLifecycle()
    val lookupResult by viewModel.lookupResult.collectAsStateWithLifecycle()
    val lookupStatus by viewModel.lookupStatus.collectAsStateWithLifecycle()
    val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
    val isDuplicate by viewModel.isDuplicate.collectAsStateWithLifecycle()
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    var showAdvanced by remember { mutableStateOf(false) }
    var freqUnit by remember { mutableStateOf("MHz") }
    val modes = listOf("SSB", "CW", "FM", "AM", "DIGI", "FT8", "FT4", "RTTY")
    var modeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeAct != null) "SESSION LOG" else "LOG QSO", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { focusManager.clearFocus() }) { Icon(Icons.Default.KeyboardHide, "Hide Keyboard") }
                    IconButton(onClick = { viewModel.resetCurrentQso() }) { Icon(Icons.Default.Refresh, "Clear") }
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
            // 1. Primary Entry Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    // Large Callsign Input with Status
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = qso.callWorked,
                                onValueChange = { viewModel.onCallsignChanged(it) },
                                label = { Text("WORKED CALLSIGN", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, color = RobertColors.Primary),
                                placeholder = { Text("EG: VK3ESE", modifier = Modifier.alpha(0.3f)) },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                isError = isDuplicate
                            )
                            
                            if (lookupStatus != LookupStatus.IDLE) {
                                Spacer(Modifier.width(Spacing.Small))
                                LookupStatusBadge(lookupStatus)
                            }
                        }
                        
                        if (isDuplicate) {
                            Text("POSSIBLE DUPLICATE (24H)", color = RobertColors.StatusOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    // 1a. Station Intelligence Strip (Flag & DXCC)
                    lookupResult?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(it.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("${it.country} • CQ:${it.cqZone} • ${it.source}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onNavigateToMap(qso.gridsquare) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Map, null, tint = RobertColors.Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (it.name.isNotEmpty()) {
                            Text(it.name, fontWeight = FontWeight.Bold, color = RobertColors.Primary)
                        }
                    }

                    // 1b. History Summary Card (Instant)
                    callHistory?.let {
                        HistorySummaryCard(it)
                    }
                }
            }

            // 2. Technical Specs (Grid)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                // Frequency with Unit Dropdown
                OutlinedTextField(
                    value = qso.frequency.toString(),
                    onValueChange = { 
                        val f = it.toDoubleOrNull() ?: qso.frequency
                        viewModel.updateCurrentQso { q -> q.copy(frequency = f) }
                    },
                    label = { Text("FREQ", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1.3f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    trailingIcon = {
                        var unitExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { unitExpanded = true }) {
                                Text(freqUnit, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                listOf("MHz", "KHz", "Hz").forEach { unit ->
                                    DropdownMenuItem(text = { Text(unit) }, onClick = { freqUnit = unit; unitExpanded = false })
                                }
                            }
                        }
                    }
                )
                
                // Mode Dropdown
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                    modifier = Modifier.weight(0.7f)
                ) {
                    OutlinedTextField(
                        value = qso.mode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("MODE", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }
                    ) {
                        modes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    viewModel.updateCurrentQso { q -> q.copy(mode = mode) }
                                    modeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. RST Selectors
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                RstSelectorField(label = "SENT", value = qso.rstSent, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(rstSent = it) } }, modifier = Modifier.weight(1f))
                RstSelectorField(label = "RCVD", value = qso.rstReceived, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(rstReceived = it) } }, modifier = Modifier.weight(1f))
                
                CompactEntryField(label = "POWER (W)", value = qso.power.toInt().toString(), onValueChange = { 
                    val p = it.toDoubleOrNull() ?: qso.power
                    viewModel.updateCurrentQso { q -> q.copy(power = p) }
                }, modifier = Modifier.weight(0.8f), type = KeyboardType.Number)
            }

            // 4. Advanced Toggle & Section
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                Spacer(Modifier.width(8.dp))
                Text(if (showAdvanced) "HIDE ADVANCED FIELDS" else "SHOW ADVANCED FIELDS")
            }

            if (showAdvanced) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
                    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text("LOCATION & NOTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            OutlinedTextField(value = qso.qth, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(qth = it) } }, label = { Text("QTH/CITY") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = qso.gridsquare, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(gridsquare = it.uppercase()) } }, label = { Text("GRID") }, modifier = Modifier.weight(0.6f), singleLine = true)
                        }
                        
                        Text("PROGRAMS & ACTIVATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            OutlinedTextField(value = qso.potaRef, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(potaRef = it.uppercase()) } }, label = { Text("POTA") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = qso.sotaRef, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(sotaRef = it.uppercase()) } }, label = { Text("SOTA") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            OutlinedTextField(value = qso.myPotaRef, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(myPotaRef = it.uppercase()) } }, label = { Text("MY POTA") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = qso.mySotaRef, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(mySotaRef = it.uppercase()) } }, label = { Text("MY SOTA") }, modifier = Modifier.weight(1f), singleLine = true)
                        }

                        Text("OTHER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                        OutlinedTextField(value = qso.contestId, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(contestId = it) } }, label = { Text("CONTEST ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = qso.notes, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(notes = it) } }, label = { Text("GENERAL NOTES") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    }
                }
            }

            // 5. Primary Save Button
            Button(
                onClick = { 
                    viewModel.saveQso()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary),
                enabled = qso.callWorked.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(12.dp))
                Text("SAVE CONTACT", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            
            Spacer(Modifier.height(Spacing.ExtraLarge))
        }
    }
}

@Composable
fun RstSelectorField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("59", "57", "55", "51", "599", "579", "559")
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            suggestions.forEach { rst ->
                DropdownMenuItem(text = { Text(rst) }, onClick = { onValueChange(rst); expanded = false })
            }
        }
    }
}

@Composable
fun LookupStatusBadge(status: LookupStatus) {
    val color = when(status) {
        LookupStatus.MATCHED -> RobertColors.StatusGreen
        LookupStatus.SEARCHING -> RobertColors.Primary
        LookupStatus.NO_MATCH -> RobertColors.TextSecondary
        else -> RobertColors.StatusOrange
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (status == LookupStatus.SEARCHING) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp, color = color)
                Spacer(Modifier.width(6.dp))
            }
            Text(status.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun HistorySummaryCard(history: CallsignHistorySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RobertColors.Primary.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp), tint = RobertColors.Primary)
            Spacer(Modifier.width(8.dp))
            Text(
                "PREVIOUS: ${history.totalQsos} QSOs • Last ${history.lastBand}/${history.lastMode}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RobertColors.Primary
            )
        }
    }
}

@Composable
fun CompactEntryField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    modifier: Modifier = Modifier,
    type: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RobertColors.Primary,
            unfocusedBorderColor = RobertColors.Surface
        )
    )
}
