package au.com.benji.robert.screens.shack

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.database.ShackEntity
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.ShackRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import coil.compose.AsyncImage

@Composable
fun ShackScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val repository = remember { ShackRepository(DatabaseModule.shackDao(context)) }
    val viewModel: ShackViewModel = viewModel(
        factory = RobertViewModelFactory { ShackViewModel(repository) }
    )

    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    var selectedImage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddEquipment.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Equipment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.Medium)
        ) {
            Text(text = "My Radio Shack", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Manage your gear and link it to live conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(Spacing.Medium))

            if (equipment.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No gear added yet. Tap + to start.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(equipment) { item ->
                        ShackItemCard(
                            item = item,
                            onDelete = { viewModel.deleteEquipment(item) },
                            onImageClick = { selectedImage = it }
                        )
                    }
                }
            }
        }
    }

    if (selectedImage != null) {
        Dialog(onDismissRequest = { selectedImage = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.large
            ) {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = "Full Size View",
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ShackItemCard(
    item: ShackEntity,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            if (item.imagePath.isNotEmpty()) {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = item.model,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onImageClick(item.imagePath) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
            } else {
                Box(
                    modifier = Modifier.size(80.dp).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.category == "Radio") Icons.Default.Radio else Icons.Default.Hardware,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.Medium))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.manufacturer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = item.model, style = MaterialTheme.typography.titleLarge)
                if (item.nickname.isNotEmpty()) {
                    Text(text = "\"${item.nickname}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(text = item.category, style = MaterialTheme.typography.labelMedium)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
