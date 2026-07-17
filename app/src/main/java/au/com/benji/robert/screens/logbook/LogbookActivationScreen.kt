package au.com.benji.robert.screens.logbook

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.ActiveActivation
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookActivationScreen(
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val activeAct by viewModel.activeActivation.collectAsStateWithLifecycle()
    
    var type by remember { mutableStateOf(activeAct?.type ?: "POTA") }
    var reference by remember { mutableStateOf(activeAct?.reference ?: "") }
    var location by remember { mutableStateOf(activeAct?.locationName ?: "") }
    
    val programs = listOf("POTA", "SOTA", "WWFF", "HEMA", "SIOTA", "VK Shires")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ACTIVATION", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            if (activeAct == null) {
                // 1. Setup New Activation
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
                    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        Text("START NEW SESSION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = RobertColors.Primary)
                        
                        Text("PROGRAM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            programs.forEach { p ->
                                FilterChip(
                                    selected = type == p,
                                    onClick = { type = p },
                                    label = { Text(p) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it.uppercase() },
                            label = { Text("REFERENCE CODE") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("EG: VK-0001", modifier = Modifier.alpha(0.3f)) },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("LOCATION NAME") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { 
                                viewModel.startActivation(type, reference, location)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusGreen),
                            enabled = reference.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("START ACTIVATION", fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                // 2. Active Session Mission Control
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
                    Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ACTIVE SESSION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = RobertColors.StatusGreen)
                            Spacer(Modifier.weight(1f))
                            Surface(color = RobertColors.StatusGreen.copy(alpha = 0.1f), shape = CircleShape) {
                                Text("LIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = RobertColors.StatusGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("${activeAct!!.type}: ${activeAct!!.reference}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        if (activeAct!!.locationName.isNotEmpty()) {
                            Text(activeAct!!.locationName, style = MaterialTheme.typography.bodyMedium, color = RobertColors.TextSecondary)
                        }

                        HorizontalDivider(color = RobertColors.Surface, thickness = 1.dp)

                        Button(
                            onClick = { viewModel.finishActivation(); onBack() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RobertColors.StatusRed),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("FINISH ACTIVATION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
