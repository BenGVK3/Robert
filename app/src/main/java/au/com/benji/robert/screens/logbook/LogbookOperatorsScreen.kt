package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.OperatorProfile
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookOperatorsScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val operators by viewModel.operators.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("OPERATORS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Operator", tint = RobertColors.Primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.Medium)) {
            if (operators.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No operators added. Add your callsign to start logging.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    items(operators) { op ->
                        val isDefault = settings.defaultOperatorId == op.id
                        OperatorCard(
                            op, 
                            isDefault,
                            onSetDefault = { viewModel.updateSettings(settings.copy(defaultOperatorId = op.id)) },
                            onDelete = { viewModel.deleteOperator(op) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOperatorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { call, name, portable ->
                viewModel.addOperator(OperatorProfile(callsign = call, name = name, portableCallsign = portable))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun OperatorCard(op: OperatorProfile, isDefault: Boolean, onSetDefault: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) RobertColors.Primary.copy(alpha = 0.1f) else RobertColors.Surface
        ),
        border = if (isDefault) androidx.compose.foundation.BorderStroke(1.dp, RobertColors.Primary) else null
    ) {
        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = RobertColors.Primary, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(op.callsign.take(1), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
            
            Spacer(Modifier.width(Spacing.Medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(op.callsign, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${op.name} • ${op.portableCallsign.ifEmpty { "Fixed" }}", style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
            }

            if (!isDefault) {
                IconButton(onClick = onSetDefault) { Icon(Icons.Default.CheckCircleOutline, "Set Default") }
            } else {
                Icon(Icons.Default.CheckCircle, "Default", tint = RobertColors.Primary, modifier = Modifier.padding(12.dp))
            }

            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = RobertColors.StatusRed.copy(alpha = 0.6f)) }
        }
    }
}

@Composable
fun AddOperatorDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var callsign by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var portable by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADD OPERATOR") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                OutlinedTextField(value = callsign, onValueChange = { callsign = it.uppercase() }, label = { Text("CALLSIGN") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("FULL NAME") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = portable, onValueChange = { portable = it.uppercase() }, label = { Text("PORTABLE CALLSIGN (OPTIONAL)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(callsign, name, portable) }, enabled = callsign.isNotEmpty()) { Text("CREATE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
