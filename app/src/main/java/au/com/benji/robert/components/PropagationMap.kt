package au.com.benji.robert.components

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import au.com.benji.robert.repository.propagation.PropagationSpot
import org.json.JSONArray
import org.json.JSONObject

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

    val html = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js"></script>
            <style>
                html, body { margin: 0; padding: 0; background: #aad3df; height: 100%; width: 100%; }
                #map { 
                    height: 100%; width: 100%; background: #aad3df; 
                }
                .leaflet-container { background: #aad3df !important; }
                
                .user-location-icon {
                    background-color: #2196F3;
                    width: 12px; height: 12px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 10px rgba(33, 150, 243, 0.9);
                }
                
                .station-marker-div {
                    width: 8px; height: 8px;
                    border-radius: 50%;
                    border: 1px solid rgba(255,255,255,0.4);
                    box-shadow: 0 0 6px currentColor;
                }
                .station-marker-div div {
                    width: 100%; height: 100%;
                    border-radius: 50%;
                }

                .cluster-icon {
                    background: rgba(33, 150, 243, 0.4);
                    border: 1.5px solid rgba(255, 255, 255, 0.6);
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { 
                    zoomControl: false,
                    attributionControl: false,
                    maxBounds: [[-90, -180], [90, 180]],
                    worldCopyJump: true,
                    preferCanvas: true
                }).setView([20, 0], 2);
                
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager_nolabels/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19, detectRetina: true
                }).addTo(map);
                
                var labels = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager_only_labels/{z}/{x}/{y}{r}.png', {
                    pane: 'shadowPane', opacity: 0.6
                }).addTo(map);

                // Create panes to control Z-index
                map.createPane('terminatorPane');
                map.getPane('terminatorPane').style.zIndex = 300; // Above base tiles (200)
                map.getPane('terminatorPane').style.pointerEvents = 'none';
                
                map.createPane('pathPane');
                map.getPane('pathPane').style.zIndex = 400;
                map.getPane('pathPane').style.pointerEvents = 'none';

                var terminatorLayer = null;
                var userMarker = null;
                
                var markers = L.markerClusterGroup({
                    showCoverageOnHover: false,
                    maxClusterRadius: 30,
                    spiderfyOnMaxZoom: true,
                    disableClusteringAtZoom: 7,
                    clusterPane: 'markerPane',
                    iconCreateFunction: function(cluster) {
                        return new L.DivIcon({ 
                            className: 'cluster-icon', 
                            iconSize: [12, 12] 
                        });
                    }
                }).addTo(map);
                
                var pathLayer = L.layerGroup([], { pane: 'pathPane' }).addTo(map);

                function getGreatCircle(start, end) {
                    const points = [];
                    const n = 50;
                    const lat1 = start[0] * Math.PI / 180;
                    const lon1 = start[1] * Math.PI / 180;
                    const lat2 = end[0] * Math.PI / 180;
                    const lon2 = end[1] * Math.PI / 180;

                    const d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2) +
                        Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lon1 - lon2) / 2), 2)));

                    if (d < 0.000001) return [[start[0], start[1]], [end[0], end[1]]];

                    for (let i = 0; i <= n; i++) {
                        const f = i / n;
                        const A = Math.sin((1 - f) * d) / Math.sin(d);
                        const B = Math.sin(f * d) / Math.sin(d);
                        const x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2) * Math.cos(lon2);
                        const y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2) * Math.sin(lon2);
                        const z = A * Math.sin(lat1) + B * Math.sin(lat2);
                        const lat = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
                        let lon = Math.atan2(y, x) * 180 / Math.PI;
                        
                        if (points.length > 0) {
                            let prevLon = points[points.length - 1][1];
                            if (lon - prevLon > 180) lon -= 360;
                            else if (lon - prevLon < -180) lon += 360;
                        }
                        
                        points.push([lat * 180 / Math.PI, lon]);
                    }
                    return points;
                }

                function getSnrColor(snr) {
                    if (snr === null || snr === undefined) return '#888';
                    if (snr >= 5) return '#4CAF50';
                    if (snr >= -5) return '#FFEB3B';
                    if (snr >= -15) return '#FF9800';
                    return '#F44336';
                }

                function updateTerminator() {
                    if (terminatorLayer) map.removeLayer(terminatorLayer);
                    
                    const now = new Date();
                    const julianDay = (now.getTime() / 86400000) + 2440587.5;
                    const D = julianDay - 2451545.0;
                    const g = (357.529 + 0.98560028 * D) % 360;
                    const q = (280.459 + 0.98564736 * D) % 360;
                    const L = (q + 1.915 * Math.sin(g * Math.PI / 180) + 0.020 * Math.sin(2 * g * Math.PI / 180)) % 360;
                    const e = (23.439 - 0.00000036 * D) % 360;
                    const dec = Math.asin(Math.sin(e * Math.PI / 180) * Math.sin(L * Math.PI / 180));
                    const gst = (18.697374558 + 24.06570982441908 * D) % 24;
                    const gha = (gst * 15) % 360;

                    const points = [];
                    // Generate terminator line
                    for (let lon = -180; lon <= 180; lon += 1) {
                        const lonRad = (lon + gha) * Math.PI / 180;
                        const tanDec = Math.tan(dec);
                        let latRad = Math.atan(-Math.cos(lonRad) / (tanDec || 0.00001));
                        points.push([latRad * 180 / Math.PI, lon]);
                    }

                    // Close the polygon around the night side
                    if (dec > 0) {
                        points.push([-90, 180], [-90, -180]);
                    } else {
                        points.push([90, 180], [90, -180]);
                    }

                    terminatorLayer = L.polygon(points, {
                        pane: 'terminatorPane',
                        fillColor: '#002', fillOpacity: 0.35,
                        color: 'rgba(0,0,0,0.2)', weight: 1.5,
                        interactive: false
                    }).addTo(map);
                }

                window.updateData = function(dataBase64) {
                    const dataJson = atob(dataBase64);
                    const data = JSON.parse(dataJson);
                    const spots = data.spots;
                    const showGreyLine = data.showGreyLine;
                    const uLat = data.userLat;
                    const uLon = data.userLon;
                    
                    const centerBefore = map.getCenter();
                    const zoomBefore = map.getZoom();

                    // Only handle User Location for placing the marker.
                    // DO NOT call setView here except on very first load to avoid snapping.
                    if (uLat != null && uLon != null) {
                        if (!userMarker) {
                            userMarker = L.marker([uLat, uLon], {
                                icon: L.divIcon({ className: 'user-location-icon', iconSize: [12, 12] }),
                                interactive: false
                            }).addTo(map);
                            
                            if (!window.hasCentered) {
                                map.setView([uLat, uLon], 4);
                                window.hasCentered = true;
                                Android.logMapState(0, 0, 0, 0, uLat, uLon, 0, 4);
                            }
                        } else {
                            userMarker.setLatLng([uLat, uLon]);
                        }
                    }

                    if (showGreyLine) {
                        updateTerminator();
                    } else if (terminatorLayer) {
                        map.removeLayer(terminatorLayer);
                        terminatorLayer = null;
                    }

                    markers.clearLayers();
                    pathLayer.clearLayers();
                    
                    const now = Date.now();
                    let drawnPaths = 0;
                    spots.forEach(s => {
                        if (isNaN(s.slat) || isNaN(s.slon) || isNaN(s.rlat) || isNaN(s.rlon)) return;
                        
                        const color = getSnrColor(s.snr);
                        const ageMin = (now - s.time) / 60000;
                        const ageFactor = Math.max(0.1, 1 - (ageMin / 60));
                        
                        const gcPoints = getGreatCircle([s.slat, s.slon], [s.rlat, s.rlon]);
                        L.polyline(gcPoints, {
                            pane: 'pathPane',
                            color: color, weight: 1.2, opacity: 0.4 * ageFactor, interactive: false
                        }).addTo(pathLayer);
                        drawnPaths++;

                        const m = L.marker([s.slat, s.slon], {
                            icon: L.divIcon({
                                className: 'station-marker-div',
                                html: '<div style="color:'+color+';background:'+color+'"></div>',
                                iconSize: [8, 8]
                            })
                        }).on('click', () => Android.onSpotClick(s.id));
                        markers.addLayer(m);
                    });
                };

                setInterval(() => { if (terminatorLayer) updateTerminator(); }, 60000);
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
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    textZoom = 100
                }
                setInitialScale(0)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Check if we already have data to update the map
                        if (spots.isNotEmpty() || userLat != null) {
                            updateMap(view, spots, showGreyLine, userLat, userLon)
                        }
                    }
                }
                webChromeClient = WebChromeClient()
                
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
                    
                    @android.webkit.JavascriptInterface
                    fun logMapState(paths: Int, markers: Int, latBefore: Double, lonBefore: Double, latAfter: Double, lonAfter: Double, zoomBefore: Int, zoomAfter: Int) {
                        Log.d("PropagationMap", "JS State Update: Paths=$paths, Markers=$markers, Center=($latAfter, $lonAfter) z$zoomAfter")
                    }
                }, "Android")

                loadDataWithBaseURL("https://appassets.androidview.com/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Use a stable identifier to avoid unnecessary updates if data hasn't changed.
            // But usually the spots list itself is what changes.
            if (webView.tag == "loaded") {
                updateMap(webView, spots, showGreyLine, userLat, userLon)
            }
        }
    )
}

private fun updateMap(webView: WebView?, spots: List<PropagationSpot>, showGreyLine: Boolean, userLat: Double?, userLon: Double?) {
    val json = JSONObject().apply {
        put("userLat", userLat ?: JSONObject.NULL)
        put("userLon", userLon ?: JSONObject.NULL)
        put("showGreyLine", showGreyLine)
        put("spots", JSONArray(spots.map { spot ->
            JSONObject().apply {
                put("id", spot.id)
                put("slat", spot.senderLat)
                put("slon", spot.senderLon)
                put("rlat", spot.receiverLat)
                put("rlon", spot.receiverLon)
                put("snr", spot.snr ?: JSONObject.NULL)
                put("time", spot.timestamp)
            }
        }))
    }
    
    val base64 = android.util.Base64.encodeToString(json.toString().toByteArray(), android.util.Base64.NO_WRAP)
    Log.d("PropagationMap", "Pushing ${spots.size} spots to WebView. showGreyLine=$showGreyLine")
    webView?.evaluateJavascript("updateData('$base64')", null)
    webView?.tag = "loaded"
}
