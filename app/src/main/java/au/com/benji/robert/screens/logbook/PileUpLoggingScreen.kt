package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.*
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PileUpLoggingScreen(
    onBack: () -> Unit,
    onNavigateToLogging: () -> Unit = {},
    viewModel: LogbookViewModel = viewModel()
) {
    val qso by viewModel.currentQso.collectAsStateWithLifecycle()
    val isDuplicate by viewModel.isDuplicate.collectAsStateWithLifecycle()
    val pileUpQueue by viewModel.pileUpQueue.collectAsStateWithLifecycle()
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PILE-UP QUEUE", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 1. Queue Entry Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium), horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = qso.callWorked,
                                onValueChange = { viewModel.onCallsignChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black, 
                                    color = if (isDuplicate) RobertColors.StatusRed else RobertColors.Primary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                placeholder = { Text("CALLSIGN", modifier = Modifier.fillMaxWidth().alpha(0.2f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = isDuplicate
                            )

                            // Modifier buttons moved below
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.padding(top = 8.dp)) {
                                TextButton(
                                    onClick = { viewModel.onCallsignChanged("${qso.callWorked}/P") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.textButtonColors(containerColor = RobertColors.Primary.copy(alpha = 0.05f))
                                ) { 
                                    Text("/P", fontSize = 12.sp, fontWeight = FontWeight.Black) 
                                }
                                TextButton(
                                    onClick = { viewModel.onCallsignChanged("${qso.callWorked}/M") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.textButtonColors(containerColor = RobertColors.Primary.copy(alpha = 0.05f))
                                ) { 
                                    Text("/M", fontSize = 12.sp, fontWeight = FontWeight.Black) 
                                }
                            }
                    
                    Spacer(Modifier.height(Spacing.Small))
                    
                    Button(
                        onClick = { viewModel.addToPileUp(qso.callWorked) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = qso.callWorked.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ADD TO QUEUE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // 2. Queue List
            Text("WAITING IN QUEUE (${pileUpQueue.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(pileUpQueue) { call ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                            Text(call, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, color = RobertColors.Primary)
                            Spacer(Modifier.weight(1f))
                            
                            Button(
                                onClick = { 
                                    viewModel.usePileUpCall(call)
                                    onNavigateToLogging()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("USE", fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.width(Spacing.Small))
                            
                            IconButton(onClick = { /* Need long press handle */ }) {
                                // For simplicity using a clickable on the row or custom interaction
                            }
                            
                            // Single-tap Clear button
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.clearPileUpCall(call) },
                                color = RobertColors.StatusRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RobertColors.StatusRed.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "CLEAR", 
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RobertColors.StatusRed,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextBadge(text: String) {
    Surface(
        color = RobertColors.Surface,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, RobertColors.Primary.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = RobertColors.Primary
        )
    }
}
