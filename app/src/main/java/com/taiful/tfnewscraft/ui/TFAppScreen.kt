package com.taiful.tfnewscraft.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.taiful.tfnewscraft.bridge.InAppBrowserConfig
import com.taiful.tfnewscraft.bridge.NativeBridge
import kotlinx.coroutines.flow.SharedFlow

/**
 * Root Jetpack Compose screen hosting the TF FB Reels & Post Maker WebView via
 * WebViewAssetLoader (secure HTTPS origin) and an optional full-screen In-App Newspaper Browser.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TFAppScreen(
    initialUrl: String,
    nativeBridge: NativeBridge,
    inAppBrowserConfig: InAppBrowserConfig?,
    inAppBrowserScripts: SharedFlow<String>,
    onMainWebViewCreated: (WebView) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onMainBackPressed: () -> Unit,
    onInAppBrowserPageLoaded: () -> Unit,
    onInAppBrowserMessage: (String) -> Unit,
    onInAppBrowserClose: () -> Unit
) {
    BackHandler(enabled = inAppBrowserConfig == null) {
        onMainBackPressed()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_webview"),
                factory = { ctx ->
                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                        .build()

                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportMultipleWindows(false)
                        }

                        addJavascriptInterface(nativeBridge, "TFNative")

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>,
                                fileChooserParams: FileChooserParams
                            ): Boolean {
                                return onShowFileChooser(filePathCallback, fileChooserParams)
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        onMainWebViewCreated(this)
                        loadUrl(initialUrl)
                    }
                }
            )

            if (inAppBrowserConfig != null) {
                InAppBrowserOverlay(
                    config = inAppBrowserConfig,
                    scriptEvents = inAppBrowserScripts,
                    onPageLoaded = onInAppBrowserPageLoaded,
                    onMessageFromWebView = onInAppBrowserMessage,
                    onClose = onInAppBrowserClose
                )
            }
        }
    }
}
