package au.com.benji.robert.screens.shack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import au.com.benji.robert.components.RobertTextField
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.repository.ShackRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun AddEquipmentScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember {
        ShackRepository(
            DatabaseModule.shackDao(context)
        )
    }

    val viewModel: AddEquipmentViewModel = viewModel(
        factory = RobertViewModelFactory {
            AddEquipmentViewModel(repository)
        }
    )

    var state by remember {
        mutableStateOf(AddEquipmentState())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {

        Text(
            text = "Add Equipment",
            style = MaterialTheme.typography.headlineMedium
        )

        RobertTextField(
            value = state.manufacturer,
            label = "Manufacturer",
            onValueChange = {
                state = state.copy(manufacturer = it)
            }
        )

        RobertTextField(
            value = state.model,
            label = "Model",
            onValueChange = {
                state = state.copy(model = it)
            }
        )

        RobertTextField(
            value = state.nickname,
            label = "Nickname (optional)",
            onValueChange = {
                state = state.copy(nickname = it)
            }
        )

        RobertTextField(
            value = state.serialNumber,
            label = "Serial Number",
            onValueChange = {
                state = state.copy(serialNumber = it)
            }
        )

        RobertTextField(
            value = state.notes,
            label = "Notes",
            onValueChange = {
                state = state.copy(notes = it)
            }
        )

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
                        notes = state.notes
                    )

                    navController.popBackStack()
                }
            }
        ) {
            Text("Save Equipment")
        }
    }
}