package au.com.benji.robert.screens.repeater

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterListScreen(
    onNavigateToDetail: (String, String) -> Unit,
    paddingValues: PaddingValues,
    viewModel: RepeaterViewModel = viewModel()
) {
    val repeaters by viewModel.filteredRepeaters.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is RepeaterUiState.Error) {
            snackbarHostState.showSnackbar((uiState as RepeaterUiState.Error).message)
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Repeaters") },
                actions = {
                    IconButton(onClick = { viewModel.refreshFromWia() }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import WIA CSV")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        val isFiltered = filters.band != "All" || filters.mode != "All" || filters.onlyFavorites
                        BadgedBox(badge = { if (isFiltered) Badge() }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refreshLocation() },
            state = pullToRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium),
                    placeholder = { Text("Search by callsign, town, freq...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                if (repeaters.isEmpty() && !isLoading) {
                    EmptyState(
                        isFiltered = filters.band != "All" || filters.mode != "All" || filters.onlyFavorites || searchQuery.isNotEmpty(),
                        onClearFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.updateFilters(RepeaterFilters())
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(repeaters, key = { it.callsign + it.frequency }) { repeater ->
                            RepeaterCard(
                                repeater = repeater,
                                onClick = { onNavigateToDetail(repeater.callsign, repeater.frequency) },
                                onFavoriteClick = { viewModel.toggleFavorite(repeater) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        RepeaterFilterDialog(
            currentFilters = filters,
            onDismiss = { showFilterDialog = false },
            onApply = { 
                viewModel.updateFilters(it)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun RepeaterCard(
    repeater: Repeater,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = repeater.callsign,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    if (repeater.status?.contains("On Air", ignoreCase = true) == true || 
                        repeater.status?.contains("Open", ignoreCase = true) == true) {
                        StatusBadge("OPEN", Color(0xFF4CAF50))
                    }
                }
                Text(
                    text = "${repeater.frequency} MHz",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${repeater.town ?: repeater.location ?: "Unknown Location"}, ${repeater.state ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoLabel(Icons.Default.Navigation, "${String.format("%.1f", repeater.distance)} km ${repeater.direction}")
                    if (!repeater.mode.isNullOrBlank()) {
                        InfoLabel(Icons.Default.SettingsInputAntenna, repeater.mode)
                    }
                }
            }
            
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (repeater.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (repeater.isFavorite) Color.Red else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun EmptyState(isFiltered: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isFiltered) Icons.Default.SearchOff else Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            if (isFiltered) "No repeaters match your filters" else "No repeaters found",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            if (isFiltered) "Try adjusting your search or filter settings." else "Import the WIA directory to get started.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
        if (isFiltered) {
            TextButton(onClick = onClearFilters) {
                Text("Clear all filters")
            }
        }
    }
}

@Composable
fun RepeaterFilterDialog(
    currentFilters: RepeaterFilters,
    onDismiss: () -> Unit,
    onApply: (RepeaterFilters) -> Unit
) {
    var band by remember { mutableStateOf(currentFilters.band) }
    var mode by remember { mutableStateOf(currentFilters.mode) }
    var onlyFavorites by remember { mutableStateOf(currentFilters.onlyFavorites) }
    var maxDistance by remember { mutableFloatStateOf(currentFilters.maxDistance.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Repeaters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text("Band", style = MaterialTheme.typography.labelLarge)
                FilterChipGroup(
                    options = listOf("All", "2m", "70cm", "6m", "10m", "23cm"),
                    selectedOption = band,
                    onSelected = { band = it }
                )

                Text("Mode", style = MaterialTheme.typography.labelLarge)
                FilterChipGroup(
                    options = listOf("All", "FM", "DMR", "D-Star", "Fusion", "P25"),
                    selectedOption = mode,
                    onSelected = { mode = it }
                )

                Text("Max Distance: ${maxDistance.toInt()} km", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = maxDistance,
                    onValueChange = { maxDistance = it },
                    valueRange = 10f..500f,
                    steps = 49
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyFavorites, onCheckedChange = { onlyFavorites = it })
                    Text("Favourites only")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(RepeaterFilters(band, mode, onlyFavorites, maxDistance.toInt())) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipGroup(options: List<String>, selectedOption: String, onSelected: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedOption == option,
                onClick = { onSelected(option) },
                label = { Text(option) }
            )
        }
    }
}
