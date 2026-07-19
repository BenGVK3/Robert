package au.com.benji.robert.screens.shack

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.R
import au.com.benji.robert.components.*
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.models.*
import au.com.benji.robert.screens.dashboard.DashboardViewModel
import au.com.benji.robert.theme.Spacing
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShackScreen(
    onBack: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: DashboardViewModel = viewModel()
) {
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    
    var selectedShackItem by remember { mutableStateOf<ShackEntity?>(null) }
    var showAddEquipmentDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShackEntity?>(null) }
    var selectedImage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.Medium)
                    .padding(top = Spacing.Medium)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.theshack1),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    text = "The Shack",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Inventory and equipment tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF03DAC6)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.Medium))

                Button(
                    onClick = { showAddEquipmentDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD NEW EQUIPMENT", fontWeight = FontWeight.Black)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(top = Spacing.Medium))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            if (equipment.isEmpty()) {
                EmptySectionCard("No gear in the shack yet. Add your first radio or antenna above.")
            } else {
                for (item in equipment) {
                    ShackItemCard(
                        item = item, 
                        onClick = { selectedShackItem = item },
                        onDelete = { itemToDelete = item },
                        onImageClick = { selectedImage = it }
                    )
                }
            }
        }
    }

    // Dialogs
    selectedShackItem?.let { item ->
        ShackDetailDialog(item = item, onDismiss = { selectedShackItem = null }, onImageClick = { selectedImage = it })
    }

    if (showAddEquipmentDialog) {
        AddEquipmentDialog(
            onDismiss = { showAddEquipmentDialog = false },
            onConfirm = { category, man, model, nick, serial, notes, path ->
                viewModel.addEquipment(category, man, model, nick, serial, notes, path)
                showAddEquipmentDialog = false
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Remove from Shack?") },
            text = { Text("Are you sure you want to remove your ${item.manufacturer} ${item.model}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEquipment(item); itemToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (selectedImage != null) {
        Dialog(
            onDismissRequest = { selectedImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = selectedImage,
                        contentDescription = "Full Size View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { selectedImage = null },
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
