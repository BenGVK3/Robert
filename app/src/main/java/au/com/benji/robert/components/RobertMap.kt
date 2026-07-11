package au.com.benji.robert.components

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RobertMap(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Refined JS to target N2YO map elements specifically
                        view?.loadUrl("javascript:(function() { " +
                                "var style = document.createElement('style');" +
                                "style.innerHTML = '* { box-sizing: border-box !important; } " +
                                "html, body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow: hidden !important; background: #000 !important; } " +
                                "table, #n2yowidget, .n2yowidget, #n2yomap, .n2yomap, .n2yowidget_main, tr, td { " +
                                "  width: 100% !important; height: 100% !important; border: none !important; " +
                                "  margin: 0 !important; padding: 0 !important; display: block !important; border-collapse: collapse !important; " +
                                "} " +
                                ".n2yowidget_header, .n2yowidget_footer, #header, #footer { display: none !important; } " +
                                "#map, #map_canvas, .leaflet-container { width: 100% !important; height: 100% !important; margin: 0 !important; padding: 0 !important; position: absolute !important; top: 0 !important; left: 0 !important; }';" +
                                "document.head.appendChild(style);" +
                                "function fixMap() { " +
                                "  if (typeof L !== 'undefined') { " +
                                "    var maps = document.querySelectorAll('.leaflet-container'); " +
                                "    maps.forEach(function(m) { " +
                                "      if(m._leaflet_map) { m._leaflet_map.invalidateSize(); } " +
                                "      else if (window.map) { window.map.invalidateSize(); } " +
                                "    }); " +
                                "  } " +
                                "} " +
                                "fixMap(); setTimeout(fixMap, 500); setTimeout(fixMap, 2000);" +
                                "})()")
                    }
                }
                
                // Fix for nested scrolling
                setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    false
                }

                // Performance and features
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    textZoom = 100
                }
                
                // Reset scale to 0 to use the natural viewport calculation
                setInitialScale(0)
                // Enable Cookies for persistent logins
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
