package com.taiful.tfnewscraft

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.taiful.tfnewscraft.bridge.InAppBrowserConfig
import com.taiful.tfnewscraft.bridge.MediaFileHelper
import com.taiful.tfnewscraft.bridge.NativeBridge
import com.taiful.tfnewscraft.bridge.NativeHostCallbacks
import com.taiful.tfnewscraft.bridge.keepAwakePolicy
import com.taiful.tfnewscraft.ui.TFAppScreen
import com.taiful.tfnewscraft.ui.theme.TFReelsPostMakerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity(), NativeHostCallbacks {

    companion object {
        private const val BASE_APP_URL = "https://appassets.androidplatform.net/assets/www/index.html"
    }

    private var mainWebView: WebView? = null
    private lateinit var nativeBridge: NativeBridge

    private var inAppBrowserConfig by mutableStateOf<InAppBrowserConfig?>(null)
    private val inAppBrowserScripts = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var pendingImagePickCallback: ((Result<JSONObject>) -> Unit)? = null
    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        nativeBridge = NativeBridge(
            context = this,
            scope = lifecycleScope,
            host = this
        )

        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            val cb = pendingImagePickCallback
            pendingImagePickCallback = null
            if (cb == null) return@registerForActivityResult
            if (uri == null) {
                cb(Result.success(JSONObject().put("cancelled", true)))
                return@registerForActivityResult
            }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = MediaFileHelper.readPickedImage(applicationContext, uri)
                    cb(Result.success(result))
                } catch (e: Throwable) {
                    cb(Result.failure(e))
                }
            }
        }

        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val callback = pendingFileChooserCallback
            pendingFileChooserCallback = null
            if (callback == null) return@registerForActivityResult

            val data = result.data
            val uris = mutableListOf<Uri>()
            if (result.resultCode == RESULT_OK && data != null) {
                val clip = data.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) {
                        clip.getItemAt(i)?.uri?.let { uris.add(it) }
                    }
                } else {
                    data.data?.let { uris.add(it) }
                }
            }
            callback.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Foreground service continues gracefully even if notification is denied */ }

        val launchUrl = buildStartUrl(intent)

        setContent {
            TFReelsPostMakerTheme {
                TFAppScreen(
                    initialUrl = launchUrl,
                    nativeBridge = nativeBridge,
                    inAppBrowserConfig = inAppBrowserConfig,
                    inAppBrowserScripts = inAppBrowserScripts,
                    onMainWebViewCreated = { wv -> mainWebView = wv },
                    onShowFileChooser = { callback, params -> handleWebFileChooser(callback, params) },
                    onMainBackPressed = {
                        val canGoBack = mainWebView?.canGoBack() == true
                        nativeBridge.emitEvent("App", "backButton", "{\"canGoBack\":$canGoBack}")
                    },
                    onInAppBrowserPageLoaded = {
                        nativeBridge.emitEvent("InAppBrowser", "browserPageLoaded", "{}")
                    },
                    onInAppBrowserMessage = { json ->
                        nativeBridge.emitEvent("InAppBrowser", "messageFromWebview", json)
                    },
                    onInAppBrowserClose = {
                        closeInAppBrowser()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedUrl = buildStartUrl(intent)
        if (sharedUrl != BASE_APP_URL) {
            mainWebView?.loadUrl(sharedUrl)
        }
    }

    override fun onResume() {
        super.onResume()
        mainWebView?.onResume()
        nativeBridge.emitEvent("App", "resume", "{}")
    }

    override fun onPause() {
        super.onPause()
        // Keep timers active when background export is running
        if (com.taiful.tfnewscraft.service.TFWorkService.instance == null) {
            mainWebView?.onPause()
        }
    }

    override fun onDestroy() {
        mainWebView?.apply {
            removeJavascriptInterface("TFNative")
            destroy()
        }
        mainWebView = null
        super.onDestroy()
    }

    private fun buildStartUrl(intent: Intent?): String {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            return BASE_APP_URL
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
        if (text.isEmpty() && subject.isEmpty()) return BASE_APP_URL

        val builder = Uri.parse(BASE_APP_URL).buildUpon()
        val urlMatch = Regex("https?://\\S+").find(text)?.value
        if (!urlMatch.isNullOrBlank()) {
            builder.appendQueryParameter("url", urlMatch)
        }
        if (subject.isNotEmpty()) {
            builder.appendQueryParameter("title", subject)
        }
        if (text.isNotEmpty()) {
            builder.appendQueryParameter("text", text)
        }
        return builder.build().toString()
    }

    private fun handleWebFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = filePathCallback

        val acceptTypes = params.acceptTypes?.filter { it.isNotBlank() }.orEmpty()
        val mimeType = when {
            acceptTypes.size == 1 && acceptTypes[0].contains("/") -> acceptTypes[0]
            acceptTypes.isNotEmpty() && acceptTypes.all { it.startsWith("image/") || it.startsWith(".") } -> "image/*"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            if (acceptTypes.size > 1 && acceptTypes.all { it.contains("/") }) {
                putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.toTypedArray())
            }
            if (params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        return try {
            fileChooserLauncher.launch(intent)
            true
        } catch (_: Throwable) {
            pendingFileChooserCallback = null
            false
        }
    }

    override fun requestImagePick(onResult: (Result<JSONObject>) -> Unit) {
        pendingImagePickCallback?.invoke(Result.success(JSONObject().put("cancelled", true)))
        pendingImagePickCallback = onResult
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Throwable) {
            pendingImagePickCallback = null
            onResult(Result.failure(e))
        }
    }

    override fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun openInAppBrowser(config: InAppBrowserConfig) {
        inAppBrowserConfig = config
    }

    override fun closeInAppBrowser() {
        val wasOpen = inAppBrowserConfig != null
        inAppBrowserConfig = null
        if (wasOpen) {
            nativeBridge.emitEvent("InAppBrowser", "closeEvent", "{}")
        }
    }

    override fun executeInAppBrowserScript(code: String) {
        inAppBrowserScripts.tryEmit(code)
    }

    override fun exitApplication() {
        finish()
    }

    override fun setMainWebViewAwake(awake: Boolean) {
        mainWebView?.keepAwakePolicy(awake)
    }

    override fun evaluateOnMainWebView(js: String) {
        runOnUiThread {
            mainWebView?.evaluateJavascript(js, null)
        }
    }
}
