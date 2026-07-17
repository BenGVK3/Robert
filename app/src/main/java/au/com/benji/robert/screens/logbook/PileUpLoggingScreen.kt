package au.com.benji.robert.screens.logbook

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PileUpLoggingScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val qso by viewModel.currentQso.collectAsStateWithLifecycle()
    val isDuplicate by viewModel.isDuplicate.collectAsStateWithLifecycle()
    val sessionQsos by viewModel.sessionQsos.collectAsStateWithLifecycle()
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PILE-UP MODE", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetCurrentQso() }) { Icon(Icons.Default.DeleteSweep, "Clear") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            // 1. Massive Entry Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RobertColors.Surface),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = qso.callWorked,
                        onValueChange = { viewModel.onCallsignChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black, 
                            color = if (isDuplicate) RobertColors.StatusOrange else RobertColors.Primary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        placeholder = { Text("CALLSIGN", modifier = Modifier.fillMaxWidth().alpha(0.2f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = isDuplicate
                    )
                    
                    if (isDuplicate) {
                        Text("DUPE WARNING!", color = RobertColors.StatusOrange, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // 2. Big Log Button
            Button(
                onClick = { viewModel.saveQso() },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RobertColors.Primary),
                enabled = qso.callWorked.isNotEmpty()
            ) {
                Text("LOG CONTACT", fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            // 3. Quick Stats & Context
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ContextBadge(activeAct?.currentBand ?: "---")
                ContextBadge(activeAct?.currentMode ?: "---")
                ContextBadge("${sessionQsos.size} QSOs")
            }

            // 4. Last 5 Contacts (Live list)
            Text("RECENTLY LOGGED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                items(sessionQsos.take(5)) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface.copy(alpha = 0.5f))) {
                        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.callWorked, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = RobertColors.Primary)
                            Spacer(Modifier.width(Spacing.Small))
                            Text("${item.band} • ${item.mode}", style = MaterialTheme.typography.bodySmall, color = RobertColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            Text("S:${item.rstSent} R:${item.rstReceived}", style = MaterialTheme.typography.labelSmall)
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
