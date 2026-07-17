package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    val info by viewModel.callsignInfo.collectAsStateWithLifecycle()
    val isDuplicate by viewModel.isDuplicate.collectAsStateWithLifecycle()
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()
    
    var showAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeAct != null) "SESSION LOG" else "LOG QSO", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
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
            // 1. Primary Entry Area (Always Visible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    // Large Callsign Input
                    OutlinedTextField(
                        value = qso.callWorked,
                        onValueChange = { viewModel.onCallsignChanged(it) },
                        label = { Text("WORKED CALLSIGN", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, color = RobertColors.Primary),
                        placeholder = { Text("EG: VK3ESE", modifier = Modifier.alpha(0.3f)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = isDuplicate,
                        supportingText = if (isDuplicate) {
                            { Text("POSSIBLE DUPLICATE (24H)", color = RobertColors.StatusOrange, fontWeight = FontWeight.Bold) }
                        } else null
                    )
                    
                    // Callsign Intelligence Strip
                    info?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(it.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("${it.country} • CQ:${it.cqZone}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onNavigateToMap(qso.gridsquare) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Map, null, tint = RobertColors.Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 2. Technical Specs (Grid)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                CompactEntryField(label = "FREQ (MHz)", value = qso.frequency.toString(), onValueChange = { 
                    val f = it.toDoubleOrNull() ?: qso.frequency
                    viewModel.updateCurrentQso { q -> q.copy(frequency = f) }
                }, modifier = Modifier.weight(1.2f), type = KeyboardType.Decimal)
                
                CompactEntryField(label = "MODE", value = qso.mode, onValueChange = { 
                    viewModel.updateCurrentQso { q -> q.copy(mode = it.uppercase()) }
                }, modifier = Modifier.weight(0.8f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                CompactEntryField(label = "RST SENT", value = qso.rstSent, onValueChange = { 
                    viewModel.updateCurrentQso { q -> q.copy(rstSent = it) }
                }, modifier = Modifier.weight(1f))
                
                CompactEntryField(label = "RST RCVD", value = qso.rstReceived, onValueChange = { 
                    viewModel.updateCurrentQso { q -> q.copy(rstReceived = it) }
                }, modifier = Modifier.weight(1f))
                
                CompactEntryField(label = "POWER (W)", value = qso.power.toInt().toString(), onValueChange = { 
                    val p = it.toDoubleOrNull() ?: qso.power
                    viewModel.updateCurrentQso { q -> q.copy(power = p) }
                }, modifier = Modifier.weight(1f), type = KeyboardType.Number)
            }

            // 3. Location / Context
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
                Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text("LOCATION & NOTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                    OutlinedTextField(
                        value = qso.qth,
                        onValueChange = { viewModel.updateCurrentQso { q -> q.copy(qth = it) } },
                        label = { Text("QTH / NAME") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = qso.notes,
                        onValueChange = { viewModel.updateCurrentQso { q -> q.copy(notes = it) } },
                        label = { Text("NOTES") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            // 4. Activation Block (Auto-hidden if session active, otherwise shown if data exists)
            if (activeAct == null) {
                // Manual activation ref fields if no session is live
            }

            // 5. Advanced Toggle
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
                    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        Text("METADATA & QSL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                        OutlinedTextField(value = qso.gridsquare, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(gridsquare = it.uppercase()) } }, label = { Text("GRID SQUARE") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = qso.operatorCallsign, onValueChange = { viewModel.updateCurrentQso { q -> q.copy(operatorCallsign = it.uppercase()) } }, label = { Text("OPERATOR CALL") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 6. Primary Save Button
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
