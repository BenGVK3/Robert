package au.com.benji.robert.screens.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.NetEntity
import au.com.benji.robert.repository.NetRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubNetsScreen() {
    val context = LocalContext.current
    val netRepository = remember { NetRepository(DatabaseModule.netDao(context)) }
    val viewModel: NetViewModel = viewModel(
        factory = RobertViewModelFactory { NetViewModel(netRepository) }
    )

    val nets by viewModel.allNets.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Nets & Meetings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("ADD")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        if (nets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No nets added yet. Add your local club nets!", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            var expandedNetId by remember { mutableStateOf<Int?>(null) }
            var editingNet by remember { mutableStateOf<NetEntity?>(null) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nets, key = { it.id }) { net ->
                    NetItemCard(
                        net = net,
                        isExpanded = expandedNetId == net.id,
                        onToggleExpand = {
                            expandedNetId = if (expandedNetId == net.id) null else net.id
                        },
                        onDelete = { viewModel.deleteNet(net) },
                        onEdit = { editingNet = net }
                    )
                }
            }

            if (editingNet != null) {
                EditNetDialog(
                    net = editingNet!!,
                    onDismiss = { editingNet = null },
                    onConfirm = { updatedNet ->
                        viewModel.updateNet(updatedNet)
                        editingNet = null
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddNetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, freq, day, time, type, notes ->
                viewModel.addNet(NetEntity(name = name, frequency = freq, dayOfWeek = day, time = time, type = type, notes = notes))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun NetItemCard(
    net: NetEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onToggleExpand
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = net.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (net.type == "Net") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = net.type.uppercase(),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text("${net.frequency} • ${getDayString(net.dayOfWeek)} @ ${net.time}", style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = Spacing.Medium)) {
                    if (net.notes.isNotEmpty()) {
                        Text(
                            text = "Notes:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = net.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = Spacing.Small)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELETE")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EDIT")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNetDialog(net: NetEntity, onDismiss: () -> Unit, onConfirm: (NetEntity) -> Unit) {
    var name by remember { mutableStateOf(net.name) }
    var freq by remember { mutableStateOf(net.frequency) }
    var selectedDay by remember { mutableStateOf<Int?>(net.dayOfWeek) }
    var time by remember { mutableStateOf(net.time) }
    var type by remember { mutableStateOf(net.type) }
    var notes by remember { mutableStateOf(net.notes) }
    var isRecurring by remember { mutableStateOf(net.dayOfWeek != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Net/Meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency") }, modifier = Modifier.fillMaxWidth())
                
                Text("Type", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "Net", onClick = { type = "Net" }, label = { Text("Net") })
                    FilterChip(selected = type == "Meeting", onClick = { type = "Meeting" }, label = { Text("Meeting") })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Weekly Recurring", style = MaterialTheme.typography.bodyMedium)
                }

                if (isRecurring) {
                    Text("Day of Week", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..7).forEach { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(getDayString(day)) }
                            )
                        }
                    }
                }

                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(net.copy(
                        name = name, 
                        frequency = freq, 
                        dayOfWeek = if (isRecurring) selectedDay else null, 
                        time = time, 
                        type = type, 
                        notes = notes
                    )) 
                }, 
                enabled = name.isNotBlank()
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNetDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int?, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf<Int?>(2) } // Monday
    var time by remember { mutableStateOf("20:00") }
    var type by remember { mutableStateOf("Net") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Net/Meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency") }, modifier = Modifier.fillMaxWidth())
                
                Text("Type", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "Net", onClick = { type = "Net" }, label = { Text("Net") })
                    FilterChip(selected = type == "Meeting", onClick = { type = "Meeting" }, label = { Text("Meeting") })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Weekly Recurring", style = MaterialTheme.typography.bodyMedium)
                }

                if (isRecurring) {
                    Text("Day of Week", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..7).forEach { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(getDayString(day)) }
                            )
                        }
                    }
                }

                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(name, freq, if (isRecurring) selectedDay else null, time, type, notes) 
                }, 
                enabled = name.isNotBlank()
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

fun getDayString(day: Int?): String = when (day) {
    1 -> "Sun"
    2 -> "Mon"
    3 -> "Tue"
    4 -> "Wed"
    5 -> "Thu"
    6 -> "Fri"
    7 -> "Sat"
    else -> "Once"
}

class NetViewModel(private val repository: NetRepository) : ViewModel() {
    val allNets = repository.getAllNets()

    fun addNet(net: NetEntity) {
        viewModelScope.launch { repository.addNet(net) }
    }

    fun deleteNet(net: NetEntity) {
        viewModelScope.launch { repository.deleteNet(net) }
    }

    fun updateNet(net: NetEntity) {
        viewModelScope.launch { repository.updateNet(net) }
    }
}
