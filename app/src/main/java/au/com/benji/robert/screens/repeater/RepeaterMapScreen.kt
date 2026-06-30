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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    lat: Double,
    lon: Double,
    repeaters: List<Repeater>,
    onMarkerClick: (String, String) -> Unit
) {
    val jsonRepeaters = remember(repeaters) { Json.encodeToString(repeaters) }
    
    val html = remember(lat, lon, jsonRepeaters) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Leaflet Map</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; background: #121212; }
                #map { height: 100vh; width: 100vw; }
                .leaflet-popup-content-wrapper { background: #1e1e1e; color: white; border-radius: 8px; }
                .leaflet-popup-tip { background: #1e1e1e; }
                .popup-title { font-weight: bold; font-size: 16px; margin-bottom: 4px; color: #2196F3; }
                .popup-info { font-size: 14px; margin-bottom: 8px; }
                .popup-button { 
                    background: #2196F3; color: white; border: none; 
                    padding: 6px 12px; border-radius: 4px; width: 100%;
                    font-weight: bold; cursor: pointer;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$lat, $lon], 11);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                // User location marker
                L.circle([$lat, $lon], { radius: 1000, color: '#2196F3', fillOpacity: 0.3 }).addTo(map);
                L.marker([$lat, $lon]).addTo(map).bindPopup("You are here");

                var repeaters = $jsonRepeaters;
                repeaters.forEach(function(r) {
                    var marker = L.marker([r.lat, r.lng]).addTo(map);
                    var content = '<div class="popup-title">' + r.callsign + '</div>' +
                                  '<div class="popup-info">' + r.frequency + ' MHz (' + (r.town || r.location || '') + ')</div>' +
                                  '<button class="popup-button" onclick="Android.onMarkerClick(\'' + r.callsign + '\', \'' + r.frequency + '\')">VIEW DETAILS</button>';
                    marker.bindPopup(content);
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onMarkerClick(callsign: String, frequency: String) {
                        onMarkerClick(callsign, frequency)
                    }
                }, "Android")
                loadDataWithBaseURL("https://appassets.androidview.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Note: Reloading every time might be heavy, but it ensures markers are updated
            // For production, we'd use a JS bridge to add/remove markers without reload
            webView.loadDataWithBaseURL("https://appassets.androidview.com", html, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
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
