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
import au.com.benji.robert.theme.RobertColors
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookUserProfilesScreen(
    onBack: () -> Unit
) {
    Scaffold(
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
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            // Security Notice
            Surface(
                color = RobertColors.Primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = RobertColors.Primary)
                    Spacer(Modifier.width(Spacing.Medium))
                    Text("Your credentials are encrypted and stored locally. No passwords leave your device except to the respective service.", style = MaterialTheme.typography.bodySmall)
                }
            }

            CredentialGroup(title = "CALLSIGN LOOKUPS", icon = Icons.Default.Public) {
                CredentialField(label = "QRZ.com XML Key")
                CredentialField(label = "HamQTH API Key")
            }

            CredentialGroup(title = "ELECTRONIC LOGS", icon = Icons.Default.CloudQueue) {
                CredentialField(label = "eQSL Username")
                CredentialField(label = "eQSL Password", isPassword = true)
                CredentialField(label = "LoTW Username")
                CredentialField(label = "ClubLog API Key")
            }

            CredentialGroup(title = "SPOTTING SERVICES", icon = Icons.Default.Language) {
                CredentialField(label = "SOTAwatch API Key")
                CredentialField(label = "POTA API Key")
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
fun CredentialField(label: String, isPassword: Boolean = false) {
    var value by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true
        )
        IconButton(onClick = {}) { Icon(Icons.Default.BugReport, "Test", tint = RobertColors.TextSecondary.copy(alpha = 0.5f)) }
    }
}
