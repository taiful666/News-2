package com.taiful.tfnewscraft.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.taiful.tfnewscraft.data.model.NewspaperSource
import java.io.ByteArrayInputStream

private val AD_HOSTS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
    "googletagservices.com", "amazon-adsystem.com", "adnxs.com", "criteo.com", "taboola.com",
    "outbrain.com", "mgid.com", "adskeeper.com", "popads.net", "popcash.net", "propellerads.com",
    "adsterra.com", "monetag.com", "exoclick.com", "clickadu.com"
)

/**
 * Native In-App Newspaper Browser overlay allowing users to browse any Bangladeshi newspaper
 * directly, block ad networks, and pick the open article straight into a Photo Card or Multi-News Reels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewspaperBrowserOverlay(
    newspaper: NewspaperSource,
    reelCount: Int,
    isExtracting: Boolean,
    adFilterEnabled: Boolean = true,
    onPickForCard: (url: String, pageTitle: String) -> Unit,
    onAddToReels: (url: String, pageTitle: String) -> Unit,
    onOpenReels: () -> Unit,
    onClose: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember(newspaper.id) { mutableStateOf(newspaper.homeUrl) }
    var currentTitle by remember(newspaper.id) { mutableStateOf(newspaper.nameBn) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = newspaper.nameBn,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("paper_browser_close_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "বন্ধ করুন")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.testTag("paper_browser_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে")
                        }
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.testTag("paper_browser_forward_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "সামনে")
                        }
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.testTag("paper_browser_reload_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        titleContentColor = MaterialTheme.colorScheme.onSecondary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onPickForCard(currentUrl, currentTitle) },
                        enabled = !isExtracting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_pick_card_button")
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ফটো কার্ড", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { onAddToReels(currentUrl, currentTitle) },
                        enabled = !isExtracting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_add_reel_button")
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিলসে যোগ", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    if (reelCount > 0) {
                        FilledTonalButton(
                            onClick = onOpenReels,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("browser_open_reels_button")
                        ) {
                            Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$reelCount", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank()) {
                                currentTitle = title
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme?.lowercase().orEmpty()
                            if (scheme != "http" && scheme != "https") return true
                            if (adFilterEnabled && isAdHost(uri)) return true
                            return false
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val uri = request?.url ?: return null
                            if (adFilterEnabled && isAdHost(uri)) {
                                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (!url.isNullOrBlank()) currentUrl = url
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!url.isNullOrBlank()) currentUrl = url
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }
                    }

                    loadUrl(newspaper.homeUrl)
                    webViewRef = this
                }
            }
        )
    }
}

private fun isAdHost(uri: Uri): Boolean {
    val host = uri.host?.lowercase() ?: return false
    return AD_HOSTS.any { ad -> host == ad || host.endsWith(".$ad") }
}
