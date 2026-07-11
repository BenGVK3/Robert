package au.com.benji.robert.screens.repeater

import android.Manifest
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.benji.robert.models.Repeater
import au.com.benji.robert.theme.Spacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RepeaterMapScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    paddingValues: PaddingValues,
    viewModel: RepeaterViewModel = viewModel()
) {
    val location by viewModel.userLocation.collectAsStateWithLifecycle()
    val repeaters by viewModel.filteredRepeaters.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("Repeater Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Find Me")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!locationPermissionState.allPermissionsGranted) {
                PermissionRequestView {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
            } else {
                LeafletMapView(
                    lat = location?.first ?: -37.8136,
                    lon = location?.second ?: 144.9631,
                    repeaters = repeaters,
                    onMarkerClick = onNavigateToDetail
                )
            }
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun PermissionRequestView(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text("Location Permission Required", style = MaterialTheme.typography.titleMedium)
        Text("We need your location to show nearby repeaters on the map.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(Spacing.Large))
        Button(onClick = onGrant) {
            Text("Grant Permission")
        }
    }
}
