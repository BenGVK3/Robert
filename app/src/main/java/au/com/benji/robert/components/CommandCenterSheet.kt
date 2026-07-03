package au.com.benji.robert.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterSheet(
    onNavigate: (String) -> Unit,
    onAction: (CommandActionType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium)
                .padding(bottom = Spacing.ExtraLarge)
        ) {
            Text(
                text = "Command Center",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Medium)
            )

            val actions = listOf(
                CommandAction("Propagation", au.com.benji.robert.R.drawable.propagation1, CommandActionType.Navigate(Screen.Propagation.route)),
                CommandAction("DX Spots", au.com.benji.robert.R.drawable.dxspots1, CommandActionType.Dialog(DialogType.DX_SPOTS)),
                CommandAction("The Shack", au.com.benji.robert.R.drawable.theshack1, CommandActionType.Dialog(DialogType.SHACK)),
                CommandAction("KiwiSDR", au.com.benji.robert.R.drawable.kiwisdr1, CommandActionType.Navigate(Screen.Sdr.route)),
                CommandAction("APRS Map", au.com.benji.robert.R.drawable.aprs1, CommandActionType.Navigate(Screen.Aprs.route)),
                CommandAction("Satellites", au.com.benji.robert.R.drawable.satellites1, CommandActionType.Navigate(Screen.Satellites.route)),
                CommandAction("Moon (EME)", au.com.benji.robert.R.drawable.moon1, CommandActionType.Navigate(Screen.Moon.route)),
                CommandAction("Repeaters", au.com.benji.robert.R.drawable.repeaters1, CommandActionType.Navigate(Screen.RepeaterList.route))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(actions) { action ->
                    PremiumActionCard(
                        icon = action.icon,
                        title = action.title,
                        onClick = {
                            when (val type = action.type) {
                                is CommandActionType.Navigate -> {
                                    onNavigate(type.route)
                                    onDismiss()
                                }
                                is CommandActionType.Dialog -> {
                                    onAction(type)
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

data class CommandAction(
    val title: String,
    val icon: Any,
    val type: CommandActionType
)

sealed class CommandActionType {
    data class Navigate(val route: String) : CommandActionType()
    data class Dialog(val type: DialogType) : CommandActionType()
}

enum class DialogType {
    DX_SPOTS, SHACK, LOGBOOK
}
