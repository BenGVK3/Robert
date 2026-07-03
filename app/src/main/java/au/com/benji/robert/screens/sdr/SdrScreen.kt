package au.com.benji.robert.screens.sdr

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdrScreen(
    paddingValues: PaddingValues,
    viewModel: SdrViewModel = viewModel()
) {
    val kiwisdrUrl by viewModel.kiwisdrUrl.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Capture the saved URL once when the screen starts.
    val initialUrl = remember { kiwisdrUrl }

    // Lifecycle observer to stop/start audio when tabbing out of the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                    webView?.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView?.onResume()
                    webView?.resumeTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("KiwiSDR", style = MaterialTheme.typography.titleMedium)
                        Text("Web-based Software Defined Radio", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        webView?.loadUrl("http://kiwisdr.com/public/")
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { 
                                    if (it.startsWith("http") && !it.contains(".xyz") && it != "about:blank") {
                                        viewModel.saveUrl(it) 
                                    }
                                }
                            }
                        }
                        
                        loadUrl(initialUrl)
                        webView = this
                    }
                },
                update = {
                    // Update logic if needed
                },
                onRelease = {
                    it.onPause()
                    it.stopLoading()
                    it.loadUrl("about:blank")
                    it.destroy()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
