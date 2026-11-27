package com.taiful.tfnewscraft.bridge

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Native HTTP client used by the CapacitorHttp bridge so news articles, feeds,
 * and news images can be fetched directly on Android without browser CORS restrictions.
 */
object HttpFetcher {

    private const val MAX_BODY_BYTES = 25 * 1024 * 1024
    private const val MAX_REDIRECTS = 6

    fun execute(options: JSONObject): JSONObject {
        var currentUrl = options.optString("url").trim()
        require(currentUrl.startsWith("https://", ignoreCase = true) || currentUrl.startsWith("http://", ignoreCase = true)) {
            "Unsupported URL scheme"
        }

        val method = options.optString("method", "GET").uppercase()
        val responseType = options.optString("responseType", "text").lowercase()
        val connectTimeout = options.optInt("connectTimeout", 15000).coerceIn(1000, 60000)
        val readTimeout = options.optInt("readTimeout", 15000).coerceIn(1000, 60000)
        val headersJson = options.optJSONObject("headers")
        val bodyData = if (options.has("data") && !options.isNull("data")) options.opt("data") else null

        var redirects = 0
        while (true) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = method
                this.connectTimeout = connectTimeout
                this.readTimeout = readTimeout
                setRequestProperty("Accept-Encoding", "gzip")
                headersJson?.keys()?.forEach { key ->
                    val value = headersJson.optString(key)
                    if (value.isNotEmpty()) {
                        setRequestProperty(key, value)
                    }
                }
            }

            try {
                if (bodyData != null && method in setOf("POST", "PUT", "PATCH", "DELETE")) {
                    conn.doOutput = true
                    val payloadBytes = when (bodyData) {
                        is String -> bodyData.toByteArray(Charsets.UTF_8)
                        else -> bodyData.toString().toByteArray(Charsets.UTF_8)
                    }
                    conn.outputStream.use { it.write(payloadBytes) }
                }

                val status = conn.responseCode
                if (status in setOf(301, 302, 303, 307, 308) && redirects < MAX_REDIRECTS) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        currentUrl = URL(URL(currentUrl), loc).toString()
                        redirects++
                        conn.disconnect()
                        continue
                    }
                }

                val rawStream = if (status in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val encoding = conn.contentEncoding.orEmpty()
                val stream: InputStream? = if (rawStream != null && encoding.equals("gzip", ignoreCase = true)) {
                    GZIPInputStream(rawStream)
                } else {
                    rawStream
                }

                val bytes = stream?.use { readBounded(it) } ?: ByteArray(0)
                val respHeaders = JSONObject()
                conn.headerFields?.forEach { (k, v) ->
                    if (k != null && !v.isNullOrEmpty()) {
                        respHeaders.put(k, v.joinToString(", "))
                    }
                }

                val dataOut: Any = when (responseType) {
                    "blob", "arraybuffer" -> Base64.encodeToString(bytes, Base64.NO_WRAP)
                    else -> String(bytes, Charsets.UTF_8)
                }

                return JSONObject().apply {
                    put("status", status)
                    put("url", currentUrl)
                    put("headers", respHeaders)
                    put("data", dataOut)
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun readBounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            total += n
            if (total > MAX_BODY_BYTES) throw IllegalStateException("Response exceeds size limit")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
