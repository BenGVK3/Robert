package au.com.benji.robert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.com.benji.robert.navigation.Screen
import au.com.benji.robert.theme.Spacing

@Composable
fun CommandCenterSheet(
    onNavigate: (String) -> Unit,
    onAction: (CommandActionType) -> Unit,
    onDismiss: () -> Unit
) {
    // Layout for the Command Center content
    // We expect this to be wrapped in AnimatedVisibility for the slide effect
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Backdrop scrim (part of the component so it animates together or stays behind)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium)
                .padding(bottom = 16.dp), // Gap above the bottom bar
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.extraLarge
                            )
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))

                Text(
                    text = "Command Center",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.Medium)
                )

                val actions = listOf(
                    CommandAction("Propagation", au.com.benji.robert.R.drawable.propagation1, CommandActionType.Navigate(Screen.Propagation.route)),
                    CommandAction("DX Spots", au.com.benji.robert.R.drawable.dxspots1, CommandActionType.Navigate(Screen.DxSpots.route)),
                    CommandAction("The Shack", au.com.benji.robert.R.drawable.theshack1, CommandActionType.Navigate(Screen.Shack.route)),
                    CommandAction("KiwiSDR", au.com.benji.robert.R.drawable.kiwisdr1, CommandActionType.Navigate(Screen.Sdr.route)),
                    CommandAction("APRS Map", au.com.benji.robert.R.drawable.aprs1, CommandActionType.Navigate(Screen.Aprs.route)),
                    CommandAction("DXLook", au.com.benji.robert.R.drawable.dxlook1, CommandActionType.Navigate(Screen.DxLook.route)),
                    CommandAction("Morse", au.com.benji.robert.R.drawable.morse1, CommandActionType.Navigate(Screen.Morse.route)),
                    CommandAction("Satellites", au.com.benji.robert.R.drawable.satellites1, CommandActionType.Navigate(Screen.Satellites.route)),
                    CommandAction("Moon", au.com.benji.robert.R.drawable.moon1, CommandActionType.Navigate(Screen.Moon.route)),
                    CommandAction("Repeaters", au.com.benji.robert.R.drawable.repeaters1, CommandActionType.Navigate(Screen.RepeaterList.route))
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = Spacing.Small)
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
