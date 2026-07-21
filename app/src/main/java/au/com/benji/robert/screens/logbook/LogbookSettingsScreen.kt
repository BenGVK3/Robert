package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookSettingsScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onNavigateToOperators: () -> Unit = {},
    onNavigateToUserProfiles: () -> Unit = {},
    onNavigateToRadios: () -> Unit = {},
    onNavigateToAntennas: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    viewModel: LogbookViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Medium, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsGroup(title = "LOGGING") {
                TogglePreference(
                    title = "Auto-save typing",
                    checked = settings.autoSave,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoSave = it)) }
                )
                TogglePreference(
                    title = "Auto-increment time",
                    checked = settings.autoIncrementTime,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoIncrementTime = it)) }
                )
                TogglePreference(
                    title = "Copy Prev Operator",
                    checked = settings.copyPreviousOperator,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(copyPreviousOperator = it)) }
                )
                TogglePreference(
                    title = "Copy Prev QTH",
                    checked = settings.copyPreviousQth,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(copyPreviousQth = it)) }
                )
            }

            SettingsGroup(title = "EQUIPMENT & OPERATORS") {
                ActionPreference(
                    title = "Station Operators",
                    subtitle = "Manage Operator IDs",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToOperators
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ActionPreference(
                        title = "Radios",
                        subtitle = "Inventory",
                        icon = Icons.Default.Radio,
                        onClick = onNavigateToRadios,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPreference(
                        title = "Antennas",
                        subtitle = "Inventory",
                        icon = Icons.Default.SettingsInputAntenna,
                        onClick = onNavigateToAntennas,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsGroup(title = "SERVICES") {
                ActionPreference(
                    title = "Credentials",
                    subtitle = "QRZ, eQSL, API Keys",
                    icon = Icons.Default.VpnKey,
                    onClick = onNavigateToUserProfiles
                )
            }
            
            Spacer(Modifier.height(Spacing.Medium))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = RobertColors.Primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
            Column { content() }
        }
    }
}

@Composable
fun TogglePreference(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun ActionPreference(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RobertColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.Medium))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun ComingSoonPreference(title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.Medium).alpha(0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RobertColors.TextSecondary)
        Spacer(Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = RobertColors.TextSecondary)
        }
        Surface(color = RobertColors.Surface, shape = RoundedCornerShape(4.dp)) {
            Text("SOON", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
        }
    }
}
