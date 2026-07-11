package au.com.benji.robert.screens.repeater

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import au.com.benji.robert.models.Repeater
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

@Composable
fun LeafletMapView(
    lat: Double,
    lon: Double,
    repeaters: List<Repeater>,
    onMarkerClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    var hasInitializedCenter by remember { mutableStateOf(false) }
    
    // Initialize OSMDroid
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    // Keep track of the current repeaters to avoid unnecessary marker refreshes
    val currentRepeatersRef = remember { mutableStateOf<List<Repeater>>(emptyList()) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            setHasTransientState(true)
            
            // Crucial: Intercept touch events to prevent parent scrolling (LazyColumn/PullToRefresh)
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP -> {
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // Allow MapView to handle the event as well
            }
        }
    }

    val groupedRepeaters = remember(repeaters) {
        repeaters.filter { it.lat != 0.0 && it.lng != 0.0 }
            .groupBy { it.lat to it.lng }
    }

    AndroidView(
        factory = { mapView },
        update = { mv ->
            // Initial centering
            if (!hasInitializedCenter && lat != 0.0 && lon != 0.0) {
                mv.controller.setZoom(9.0)
                mv.controller.setCenter(GeoPoint(lat, lon))
                hasInitializedCenter = true
            }
            
            // Only rebuild overlays if the data actually changed to prevent flickering while panning
            if (currentRepeatersRef.value != repeaters) {
                currentRepeatersRef.value = repeaters
                
                mv.overlays.clear()
                
                // User Location: Blue Dot
                val userMarker = Marker(mv).apply {
                    position = GeoPoint(lat, lon)
                    icon = createUserLocationIcon(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "You are here"
                    infoWindow = null
                }
                mv.overlays.add(userMarker)
                
                // Repeater Markers
                groupedRepeaters.forEach { (coords, list) ->
                    val marker = Marker(mv).apply {
                        position = GeoPoint(coords.first, coords.second)
                        icon = createRepeaterIcon(context, list.size > 1)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        
                        infoWindow = RepeaterInfoWindow(mv, list, onMarkerClick)
                        
                        setOnMarkerClickListener { m, _ ->
                            if (!m.isInfoWindowShown) {
                                m.showInfoWindow()
                                mv.controller.animateTo(m.position)
                            } else {
                                m.closeInfoWindow()
                            }
                            true
                        }
                    }
                    mv.overlays.add(marker)
                }
                mv.invalidate()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    
    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }
}

private fun createUserLocationIcon(context: Context): Drawable {
    val px = (18 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    paint.color = Color.parseColor("#40000000")
    canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
    
    paint.color = Color.WHITE
    canvas.drawCircle(px / 2f, px / 2f, px / 2.2f, paint)
    
    paint.color = Color.parseColor("#2196F3")
    canvas.drawCircle(px / 2f, px / 2f, px / 3.2f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

private fun createRepeaterIcon(context: Context, isGroup: Boolean): Drawable {
    val px = (28 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    val color = if (isGroup) Color.parseColor("#FF9800") else Color.parseColor("#673AB7")
    
    paint.color = Color.parseColor("#40000000")
    canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
    
    paint.color = color
    canvas.drawCircle(px / 2f, px / 2f, px / 2.4f, paint)
    
    paint.style = Paint.Style.STROKE
    paint.color = Color.WHITE
    paint.strokeWidth = 2 * context.resources.displayMetrics.density
    canvas.drawCircle(px / 2f, px / 2f, px / 2.4f, paint)
    
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawCircle(px / 2f, px / 2f, px / 6f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

class RepeaterInfoWindow(
    mapView: MapView,
    private val repeaters: List<Repeater>,
    private val onDetailsClick: (String, String) -> Unit
) : InfoWindow(createLayout(mapView.context), mapView) {

    companion object {
        private fun createLayout(context: Context): View {
            val density = context.resources.displayMetrics.density
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    (280 * density).toInt(), // Limit width
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun onOpen(item: Any?) {
        val context = mView.context
        val layout = mView as LinearLayout
        layout.removeAllViews()
        
        val density = context.resources.displayMetrics.density
        val padding = (12 * density).toInt()
        layout.setPadding(padding, padding, padding, padding)

        val bg = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#EE1A1A1A").toInt())
            cornerRadius = 12 * density
            setStroke((1 * density).toInt(), Color.parseColor("#444444").toInt())
        }
        layout.background = bg

        repeaters.take(5).forEachIndexed { index, repeater ->
            if (index > 0) {
                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt())
                    setBackgroundColor(Color.parseColor("#333333"))
                    val margin = (8 * density).toInt()
                    (layoutParams as LinearLayout.LayoutParams).setMargins(0, margin, 0, margin)
                }
                layout.addView(divider)
            }

            val repeaterLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, (8 * density).toInt(), 0)
                setOnClickListener {
                    onDetailsClick(repeater.callsign, repeater.frequency)
                    close()
                }
            }

            val titleView = TextView(context).apply {
                text = "${repeater.callsign}${if (!repeater.name.isNullOrBlank()) " - ${repeater.name}" else ""}"
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            repeaterLayout.addView(titleView)

            val infoView = TextView(context).apply {
                val mode = if (!repeater.mode.isNullOrBlank()) " [${repeater.mode}]" else ""
                val town = if (!repeater.town.isNullOrBlank()) " @ ${repeater.town}" else if (!repeater.location.isNullOrBlank()) " @ ${repeater.location}" else ""
                text = "${repeater.frequency} MHz$mode$town"
                setTextColor(Color.WHITE)
                textSize = 13f
            }
            repeaterLayout.addView(infoView)
            
            val tapView = TextView(context).apply {
                text = "Tap for details ›"
                setTextColor(Color.parseColor("#888888"))
                textSize = 11f
                setPadding(0, 4, 0, 0)
            }
            repeaterLayout.addView(tapView)

            layout.addView(repeaterLayout)
        }
        
        if (repeaters.size > 5) {
            val moreView = TextView(context).apply {
                text = "+ ${repeaters.size - 5} more here"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 12f
                setPadding(0, 8, 0, 0)
                gravity = Gravity.CENTER
            }
            layout.addView(moreView)
        }
    }

    override fun onClose() {}
}
