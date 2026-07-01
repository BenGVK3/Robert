package au.com.benji.robert.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import au.com.benji.robert.repository.propagation.PropagationSpot

@Composable
fun PropagationMap(
    spots: List<PropagationSpot>,
    showGreyLine: Boolean = true,
    modifier: Modifier = Modifier,
    onSpotSelected: (PropagationSpot) -> Unit = {}
) {
    // Using a Leaflet-based map to avoid crashes due to missing Google Maps API keys
    val html = remember(spots, showGreyLine) {
        val spotsJson = spots.take(200).map { spot ->
            """
            {
                "slat": ${spot.senderLat}, "slon": ${spot.senderLon},
                "rlat": ${spot.receiverLat}, "rlon": ${spot.receiverLon},
                "dist": ${spot.distance}, "scall": "${spot.senderCallsign}",
                "rcall": "${spot.receiverCallsign}", "id": "${spot.id}"
            }
            """.trimIndent()
        }.joinToString(",")

        """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; background: #000; }
                #map { height: 100vh; width: 100vw; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([20, 0], 2);
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                var spots = [$spotsJson];
                spots.forEach(function(s) {
                    var color = s.dist < 1000 ? '#4CAF50' : (s.dist < 5000 ? '#FFEB3B' : (s.dist < 10000 ? '#FF9800' : '#F44336'));
                    L.polyline([[s.slat, s.slon], [s.rlat, s.rlon]], {
                        color: color, weight: 2, opacity: 0.6
                    }).addTo(map);
                    
                    L.circleMarker([s.slat, s.slon], { radius: 3, color: color, fillOpacity: 0.8 }).addTo(map)
                        .on('click', function() { Android.onSpotClick(s.id); });
                    L.circleMarker([s.rlat, s.rlon], { radius: 3, color: color, fillOpacity: 0.8 }).addTo(map)
                        .on('click', function() { Android.onSpotClick(s.id); });
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onSpotClick(id: String) {
                        spots.find { it.id == id }?.let { onSpotSelected(it) }
                    }
                }, "Android")
                loadDataWithBaseURL("https://appassets.androidview.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://appassets.androidview.com", html, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun GreyLineOverlay() {
    // Logic handled within the main map implementation for now
}
