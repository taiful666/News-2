package com.taiful.tfnewscraft.bridge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.taiful.tfnewscraft.BuildConfig
import com.taiful.tfnewscraft.service.TFWorkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class InAppBrowserConfig(
    val url: String,
    val title: String,
    val toolbarColorHex: String = "#1877F2",
    val toolbarTextColorHex: String = "#FFFFFF",
    val darkBackground: Boolean = false,
    val blockedHosts: List<String> = emptyList()
)

interface NativeHostCallbacks {
    fun requestImagePick(onResult: (Result<JSONObject>) -> Unit)
    fun requestNotificationPermissionIfNeeded()
    fun openInAppBrowser(config: InAppBrowserConfig)
    fun closeInAppBrowser()
    fun executeInAppBrowserScript(code: String)
    fun exitApplication()
    fun setMainWebViewAwake(awake: Boolean)
    fun evaluateOnMainWebView(js: String)
}

/**
 * JavascriptInterface exposed as `window.TFNative` inside the main WebView.
 * All async operations resolve/reject Promises via `window.__tfResolve(...)`.
 */
class NativeBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val host: NativeHostCallbacks
) {

    @JavascriptInterface
    fun httpRequest(callId: String, argsJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val opts = JSONObject(argsJson)
                val result = HttpFetcher.execute(opts)
                resolve(callId, result)
            } catch (e: Throwable) {
                reject(callId, e.message ?: "HTTP request failed")
            }
        }
    }

    @JavascriptInterface
    fun pickImage(callId: String, @Suppress("UNUSED_PARAMETER") argsJson: String) {
        scope.launch(Dispatchers.Main) {
            host.requestImagePick { result ->
                result.onSuccess { resolve(callId, it) }
                    .onFailure { reject(callId, it.message ?: "Image pick failed") }
            }
        }
    }

    @JavascriptInterface
    fun writeFile(callId: String, argsJson: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val opts = JSONObject(argsJson)
                val result = MediaFileHelper.writeFile(context, opts)
                resolve(callId, result)
            } catch (e: Throwable) {
                reject(callId, e.message ?: "Write failed")
            }
        }
    }

    @JavascriptInterface
    fun share(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val opts = JSONObject(argsJson)
                MediaFileHelper.launchShare(context, opts)
                resolve(callId, JSONObject().put("ok", true))
            } catch (e: Throwable) {
                reject(callId, e.message ?: "Share failed")
            }
        }
    }

    @JavascriptInterface
    fun workStart(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val opts = JSONObject(argsJson)
                host.requestNotificationPermissionIfNeeded()
                val intent = Intent(context, TFWorkService::class.java).apply {
                    action = TFWorkService.ACTION_START
                    putExtra("title", opts.optString("title", "ভিডিও তৈরি হচ্ছে"))
                    putExtra("text", opts.optString("text", ""))
                }
                ContextCompat.startForegroundService(context, intent)
                host.setMainWebViewAwake(true)
                resolve(callId, JSONObject().put("ok", true))
            } catch (e: Throwable) {
                reject(callId, e.message ?: "Failed to start export service")
            }
        }
    }

    @JavascriptInterface
    fun workUpdate(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val opts = JSONObject(argsJson)
                TFWorkService.instance?.updateProgress(
                    opts.optInt("percent", 0),
                    opts.optString("text", "")
                )
            } catch (_: Throwable) {
            }
            resolve(callId, JSONObject().put("ok", true))
        }
    }

    @JavascriptInterface
    fun workStop(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val opts = JSONObject(argsJson)
                TFWorkService.instance?.finishWork(
                    done = opts.optBoolean("done", false),
                    doneTitle = opts.optString("title", ""),
                    doneText = opts.optString("text", "")
                )
                host.setMainWebViewAwake(false)
            } catch (_: Throwable) {
            }
            resolve(callId, JSONObject().put("ok", true))
        }
    }

    @JavascriptInterface
    fun iabOpen(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val opts = JSONObject(argsJson)
                val url = opts.optString("url").trim()
                require(url.startsWith("http://", true) || url.startsWith("https://", true)) {
                    "Invalid browser URL"
                }
                val blocked = mutableListOf<String>()
                val arr: JSONArray? = opts.optJSONArray("blockedHosts")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val h = arr.optString(i).trim()
                        if (h.isNotEmpty()) blocked.add(h.lowercase())
                    }
                }
                host.openInAppBrowser(
                    InAppBrowserConfig(
                        url = url,
                        title = opts.optString("title", ""),
                        toolbarColorHex = opts.optString("toolbarColor", "#1877F2"),
                        toolbarTextColorHex = opts.optString("toolbarTextColor", "#FFFFFF"),
                        darkBackground = opts.optString("backgroundColor", "white").equals("black", true),
                        blockedHosts = blocked
                    )
                )
                resolve(callId, JSONObject().put("ok", true))
            } catch (e: Throwable) {
                reject(callId, e.message ?: "Failed to open in-app browser")
            }
        }
    }

    @JavascriptInterface
    fun iabClose(callId: String, @Suppress("UNUSED_PARAMETER") argsJson: String) {
        scope.launch(Dispatchers.Main) {
            host.closeInAppBrowser()
            resolve(callId, JSONObject().put("ok", true))
        }
    }

    @JavascriptInterface
    fun iabExecute(callId: String, argsJson: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val code = JSONObject(argsJson).optString("code")
                if (code.isNotBlank()) {
                    host.executeInAppBrowserScript(code)
                }
                resolve(callId, JSONObject().put("ok", true))
            } catch (e: Throwable) {
                reject(callId, e.message ?: "Script execution failed")
            }
        }
    }

    @JavascriptInterface
    fun getAppInfo(callId: String, @Suppress("UNUSED_PARAMETER") argsJson: String) {
        val info = JSONObject().apply {
            put("name", "TF FB Reels & Post Maker")
            put("id", BuildConfig.APPLICATION_ID)
            put("version", BuildConfig.VERSION_NAME)
            put("build", BuildConfig.VERSION_CODE.toString())
        }
        resolve(callId, info)
    }

    @JavascriptInterface
    fun exitApp() {
        scope.launch(Dispatchers.Main) {
            host.exitApplication()
        }
    }

    fun emitEvent(scopeName: String, eventName: String, payloadJson: String = "{}") {
        val js = "window.__tfEmit && window.__tfEmit(${JSONObject.quote(scopeName)}, ${JSONObject.quote(eventName)}, $payloadJson);"
        host.evaluateOnMainWebView(js)
    }

    private fun resolve(callId: String, payload: JSONObject) {
        val js = "window.__tfResolve && window.__tfResolve(${JSONObject.quote(callId)}, true, $payload);"
        host.evaluateOnMainWebView(js)
    }

    private fun reject(callId: String, message: String) {
        val js = "window.__tfResolve && window.__tfResolve(${JSONObject.quote(callId)}, false, ${JSONObject.quote(message)});"
        host.evaluateOnMainWebView(js)
    }
}

fun WebView.keepAwakePolicy(awake: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, !awake)
    }
    if (awake) {
        resumeTimers()
    }
}
