package au.com.benji.robert.screens.shack

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import au.com.benji.robert.components.RobertTextField
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.models.EquipmentCategory
import au.com.benji.robert.repository.ShackRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ShackRepository(DatabaseModule.shackDao(context)) }
    val viewModel: AddEquipmentViewModel = viewModel(
        factory = RobertViewModelFactory { AddEquipmentViewModel(repository) }
    )

    var state by remember { mutableStateOf(AddEquipmentState()) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            state = state.copy(imagePath = it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        Text(text = "Add Shack Gear", style = MaterialTheme.typography.headlineMedium)

        ExposedDropdownMenuBox(
            expanded = showCategoryMenu,
            onExpandedChange = { showCategoryMenu = !showCategoryMenu }
        ) {
            OutlinedTextField(
                value = state.category.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { showCategoryMenu = false }
            ) {
                EquipmentCategory.values().forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.displayName) },
                        onClick = {
                            state = state.copy(category = category)
                            showCategoryMenu = false
                        }
                    )
                }
            }
        }

        RobertTextField(
            value = state.manufacturer,
            label = "Manufacturer (e.g. ICOM, Yaesu)",
            onValueChange = { state = state.copy(manufacturer = it) }
        )

        RobertTextField(
            value = state.model,
            label = "Model (e.g. IC-207, FT-920)",
            onValueChange = { state = state.copy(model = it) }
        )

        RobertTextField(
            value = state.nickname,
            label = "Nickname (optional)",
            onValueChange = { state = state.copy(nickname = it) }
        )

        RobertTextField(
            value = state.serialNumber,
            label = "Serial Number",
            onValueChange = { state = state.copy(serialNumber = it) }
        )

        RobertTextField(
            value = state.notes,
            label = "Notes",
            onValueChange = { state = state.copy(notes = it) }
        )

        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(if (state.imagePath.isEmpty()) "Add Photo" else "Change Photo")
        }

        if (state.imagePath.isNotEmpty()) {
            Text("Photo Attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    viewModel.saveEquipment(
                        category = state.category.displayName,
                        manufacturer = state.manufacturer,
                        model = state.model,
                        nickname = state.nickname,
                        serialNumber = state.serialNumber,
                        notes = state.notes,
                        imagePath = state.imagePath
                    )
                    navController.popBackStack()
                }
            },
            enabled = state.manufacturer.isNotBlank() && state.model.isNotBlank()
        ) {
            Text("Save to Shack")
        }
    }
}
