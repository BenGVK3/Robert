package au.com.benji.robert.screens.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.*
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookUserProfilesScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    viewModel: LogbookViewModel = viewModel()
) {
    val credentials by viewModel.repository.serviceCredentials.collectAsStateWithLifecycle(emptyList<ServiceCredential>())

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("SERVICE CREDENTIALS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
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
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            CredentialGroup(title = "CALLSIGN LOOKUPS", icon = Icons.Default.Public) {
                val qrz = credentials.find { it.serviceName == "QRZ" } ?: ServiceCredential("QRZ")
                CredentialField(
                    label = "QRZ.com Username",
                    value = qrz.username,
                    onValueChange = { viewModel.updateCredential(qrz.copy(username = it)) }
                )
                CredentialField(
                    label = "QRZ.com Password",
                    value = qrz.passwordEncrypted,
                    isPassword = true,
                    onValueChange = { viewModel.updateCredential(qrz.copy(passwordEncrypted = it)) }
                )
                
                val callook = credentials.find { it.serviceName == "Callook" } ?: ServiceCredential("Callook")
                TogglePreference(
                    title = "Enable Callook (US)",
                    checked = callook.isEnabled,
                    onCheckedChange = { viewModel.updateCredential(callook.copy(isEnabled = it)) }
                )
            }

            CredentialGroup(title = "ELECTRONIC LOGS", icon = Icons.Default.CloudQueue) {
                val eqsl = credentials.find { it.serviceName == "eQSL" } ?: ServiceCredential("eQSL")
                CredentialField(
                    label = "eQSL Username",
                    value = eqsl.username,
                    onValueChange = { viewModel.updateCredential(eqsl.copy(username = it)) }
                )
                CredentialField(
                    label = "eQSL Password",
                    value = eqsl.passwordEncrypted,
                    isPassword = true,
                    onValueChange = { viewModel.updateCredential(eqsl.copy(passwordEncrypted = it)) }
                )
            }
            
            Spacer(Modifier.height(Spacing.ExtraLarge))
        }
    }
}

@Composable
fun CredentialGroup(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = RobertColors.Primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RobertColors.TextSecondary)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RobertColors.Surface)) {
            Column(modifier = Modifier.padding(vertical = Spacing.Small)) { content() }
        }
    }
}

@Composable
fun CredentialField(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    var textState by remember { mutableStateOf(value) }
    
    // Update local state when external value changes (e.g. on load)
    // but only if we're not currently editing to avoid cursor jumps
    LaunchedEffect(value) {
        if (textState != value) {
            textState = value
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        OutlinedTextField(
            value = textState,
            onValueChange = { 
                textState = it
                onValueChange(it) 
            },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true
        )
    }
}
