package au.com.benji.robert.screens.shack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import au.com.benji.robert.database.DatabaseModule
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.repository.ShackRepository
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory


@Composable
fun ShackScreen(
    navController: NavHostController
) {

    val context = LocalContext.current

    val repository = remember {
        ShackRepository(
            DatabaseModule.shackDao(context)
        )
    }

    val viewModel: ShackViewModel = viewModel(
        factory = RobertViewModelFactory {
            ShackViewModel(repository)
        }
    )

    val equipment by viewModel.equipment.collectAsStateWithLifecycle()

    Scaffold(

        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddEquipment.route)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Equipment"
                )
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {

            item {

                Text(
                    text = "Shack",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            items(equipment) { item ->

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RobertColors.CardBackground
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(Spacing.Medium)
                    ) {

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}