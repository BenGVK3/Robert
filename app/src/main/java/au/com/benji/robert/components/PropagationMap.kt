package au.com.benji.robert.components

import android.view.ViewGroup
import android.webkit.WebChromeClient
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
    userLat: Double? = null,
    userLon: Double? = null,
    showGreyLine: Boolean = true,
    modifier: Modifier = Modifier,
    onSpotSelected: (PropagationSpot) -> Unit = {}
) {
    val currentSpots = rememberUpdatedState(spots)
    val currentOnSpotSelected = rememberUpdatedState(onSpotSelected)

    // Using a Leaflet-based map to avoid crashes due to missing Google Maps API keys
    val html = remember(spots, showGreyLine, userLat, userLon) {
        val spotsJson = spots.take(200).map { spot ->
            val slat = if (spot.senderLat.isNaN()) 0.0 else spot.senderLat
            val slon = if (spot.senderLon.isNaN()) 0.0 else spot.senderLon
            val rlat = if (spot.receiverLat.isNaN()) 0.0 else spot.receiverLat
            val rlon = if (spot.receiverLon.isNaN()) 0.0 else spot.receiverLon
            """
            {
                "slat": $slat, "slon": $slon,
                "rlat": $rlat, "rlon": $rlon,
                "dist": ${if (spot.distance.isNaN()) 0.0 else spot.distance}, 
                "scall": "${spot.senderCallsign}",
                "rcall": "${spot.receiverCallsign}", "id": "${spot.id}"
            }
            """.trimIndent()
        }.joinToString(",")

        val lat = if (userLat == null || userLat.isNaN()) 20.0 else userLat
        val lon = if (userLon == null || userLon.isNaN()) 0.0 else userLon

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet-terminator"></script>
            <style>
                html, body { margin: 0; padding: 0; background: #000; height: 100%; width: 100%; }
                #map { height: 100%; width: 100%; background: #000; }
                .leaflet-container { background: #000 !important; }
                .user-location-icon {
                    background-color: #2196F3;
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 10px rgba(33, 150, 243, 0.5);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { 
                    zoomControl: false,
                    attributionControl: false 
                }).setView([$lat, $lon], 3);
                
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    detectRetina: true,
                    attribution: '&copy; CARTO'
                }).addTo(map);

                if (${userLat != null && userLon != null}) {
                    L.marker([$lat, $lon], {
                        icon: L.divIcon({
                            className: 'user-location-icon',
                            iconSize: [12, 12]
                        }),
                        interactive: false
                    }).addTo(map);
                }

                if ($showGreyLine) {
                    try {
                        if (typeof L.terminator === 'function') {
                            L.terminator({
                                fillColor: '#000',
                                fillOpacity: 0.5,
                                color: '#fff',
                                weight: 1
                            }).addTo(map);
                        }
                    } catch(e) { console.error('Terminator error:', e); }
                }

                var spots = [$spotsJson];
                spots.forEach(function(s) {
                    var color = s.dist < 1000 ? '#4CAF50' : (s.dist < 5000 ? '#FFEB3B' : (s.dist < 10000 ? '#FF9800' : '#F44336'));
                    
                    // Draw path
                    L.polyline([[s.slat, s.slon], [s.rlat, s.rlon]], {
                        color: color, weight: 1, opacity: 0.4
                    }).addTo(map);
                    
                    // Sender marker
                    L.circleMarker([s.slat, s.slon], { 
                        radius: 3, 
                        color: color, 
                        fillColor: color,
                        fillOpacity: 0.8 
                    }).addTo(map).on('click', function() { Android.onSpotClick(s.id); });
                    
                    // Receiver marker
                    L.circleMarker([s.rlat, s.rlon], { 
                        radius: 2, 
                        color: '#fff', 
                        fillColor: color,
                        fillOpacity: 0.5 
                    }).addTo(map).on('click', function() { Android.onSpotClick(s.id); });
                });
                
                setTimeout(function() {
                    map.invalidateSize();
                }, 500);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    databaseEnabled = true
                }
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                
                // Fix for nested scrolling
                setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    false
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onSpotClick(id: String) {
                        currentSpots.value.find { it.id == id }?.let { 
                            currentOnSpotSelected.value(it) 
                        }
                    }
                }, "Android")
            }
        },
        update = { webView ->
            val lastHtml = webView.tag as? String
            if (lastHtml != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("https://appassets.androidview.com/", html, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
fun GreyLineOverlay() {
    // Logic handled within the main map implementation for now
}
