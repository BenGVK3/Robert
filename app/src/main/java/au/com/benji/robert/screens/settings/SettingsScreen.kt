package au.com.benji.robert.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.repository.BandPlanRepository
import au.com.benji.robert.repository.SettingsRepository
import au.com.benji.robert.theme.Spacing
import au.com.benji.robert.viewmodel.RobertViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val bandPlanRepository = remember { BandPlanRepository(context) }
    val viewModel: SettingsViewModel = viewModel(
        factory = RobertViewModelFactory { SettingsViewModel(repository, bandPlanRepository) }
    )

    val savedCallsign by viewModel.callsign.collectAsStateWithLifecycle()
    val savedName by viewModel.name.collectAsStateWithLifecycle()
    val savedGridSquare by viewModel.gridSquare.collectAsStateWithLifecycle()
    val savedCountry by viewModel.country.collectAsStateWithLifecycle()
    val savedLicenceClass by viewModel.licenceClass.collectAsStateWithLifecycle()
    val savedThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var callsign by remember(savedCallsign) { mutableStateOf(savedCallsign) }
    var name by remember(savedName) { mutableStateOf(savedName) }
    var gridSquare by remember(savedGridSquare) { mutableStateOf(savedGridSquare) }
    var country by remember(savedCountry) { mutableStateOf(savedCountry) }
    var licenceClass by remember(savedLicenceClass) { mutableStateOf(savedLicenceClass) }

    var countryExpanded by remember { mutableStateOf(false) }
    var licenceExpanded by remember { mutableStateOf(false) }

    val countries = viewModel.getCountries()
    val licenceClasses = viewModel.getLicenceClasses(country)

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    TextButton(
                        onClick = { 
                            viewModel.saveSettings(callsign, name, gridSquare, country, licenceClass)
                        }
                    ) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.Medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Text(text = "Station Information", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = callsign,
                onValueChange = { callsign = it },
                label = { Text("Home Callsign") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Operator Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = gridSquare,
                onValueChange = { gridSquare = it },
                label = { Text("Home Grid Square") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(text = "Licensing & Region", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = !countryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = country,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country / Region") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = countryExpanded,
                    onDismissRequest = { countryExpanded = false }
                ) {
                    countries.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                country = selectionOption
                                countryExpanded = false
                                // Reset licence class to first available for new country
                                val newClasses = viewModel.getLicenceClasses(selectionOption)
                                if (newClasses.isNotEmpty()) {
                                    licenceClass = newClasses.first().id
                                }
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = licenceExpanded,
                onExpandedChange = { licenceExpanded = !licenceExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentLicenceName = licenceClasses.find { it.id == licenceClass }?.name ?: licenceClass
                OutlinedTextField(
                    value = currentLicenceName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Licence Class") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = licenceExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = licenceExpanded,
                    onDismissRequest = { licenceExpanded = false }
                ) {
                    licenceClasses.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption.name) },
                            onClick = {
                                licenceClass = selectionOption.id
                                licenceExpanded = false
                            }
                        )
                    }
                }
            }

            Text(text = "Appearance", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                listOf("System", "Light", "Dark").forEach { mode ->
                    FilterChip(
                        selected = savedThemeMode == mode,
                        onClick = { viewModel.saveThemeMode(mode) },
                        label = { Text(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

            // App Information Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(
                    text = "Created by Ben (VK3ESE)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                SelectionContainer {
                    Text(
                        text = "VK3ESE@Gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "This app is entirely free, open source, and ad-free.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                TextButton(
                    onClick = { /* Future donation page link */ },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Support development: Buy me a coffee",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Version 1.0.0 (Beta)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = Spacing.Small)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
        }
    }
}
