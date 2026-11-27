package com.taiful.tfnewscraft.bridge

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Handles zero-permission MediaStore saving (API 29+), FileProvider caching/sharing,
 * and native image reading/shrinking for the logo photo picker.
 */
object MediaFileHelper {

    private const val KEEP_ORIGINAL_MAX = 12 * 1024 * 1024
    private const val READ_MAX = 80 * 1024 * 1024
    private const val SHRINK_TO = 1600
    private const val APP_FOLDER = "TF FB Reels and Post Maker"

    fun readPickedImage(context: Context, uri: Uri): JSONObject {
        val cr = context.contentResolver
        var mime = cr.getType(uri).orEmpty()
        var name = resolveDisplayName(cr, uri) ?: "logo"
        var bytes = readUriBytes(cr, uri)
        require(bytes.isNotEmpty()) { "Empty image file" }

        if (bytes.size > KEEP_ORIGINAL_MAX) {
            val shrunk = shrinkToPng(cr, uri, bytes)
            if (shrunk != null) {
                bytes = shrunk
                mime = "image/png"
                name = name.replace(Regex("\\.[^.]+$"), "") + ".png"
            }
        }

        return JSONObject().apply {
            put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
            put("mime", mime)
            put("name", name)
            put("size", bytes.size)
        }
    }

    fun writeFile(context: Context, options: JSONObject): JSONObject {
        val relPath = options.optString("path").trim().trimStart('/')
        val b64 = options.optString("data")
        val directory = options.optString("directory", "CACHE").uppercase()
        val rawMime = options.optString("mime").trim()
        require(relPath.isNotEmpty()) { "Missing path" }

        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val fileName = relPath.substringAfterLast('/').ifBlank { "file" }
        val mime = rawMime.ifBlank { guessMimeType(fileName) }

        if (directory == "CACHE") {
            val outFile = File(context.cacheDir, relPath)
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(bytes)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            return JSONObject().put("uri", contentUri.toString())
        }

        // EXTERNAL_STORAGE: save via zero-permission MediaStore on API 29+, else app external dir
        val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, mime, bytes)
        } else {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val outFile = File(File(baseDir, APP_FOLDER), fileName)
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }

        return JSONObject().put("uri", savedUri.toString())
    }

    private fun saveViaMediaStore(context: Context, fileName: String, mime: String, bytes: ByteArray): Uri {
        val resolver = context.contentResolver
        val isImage = mime.startsWith("image/")
        val isVideo = mime.startsWith("video/")

        val (collection, relativePath) = when {
            isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER"
            isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_MOVIES}/$APP_FOLDER"
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_DOWNLOADS}/$APP_FOLDER"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Could not create MediaStore entry")

        try {
            resolver.openOutputStream(itemUri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("Could not open MediaStore stream")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            return itemUri
        } catch (e: Throwable) {
            resolver.delete(itemUri, null, null)
            throw e
        }
    }

    fun launchShare(context: Context, options: JSONObject) {
        val title = options.optString("title")
        val text = options.optString("text")
        val url = options.optString("url")
        val dialogTitle = options.optString("dialogTitle").ifBlank { "শেয়ার করুন" }
        val filesArr: JSONArray? = options.optJSONArray("files")

        val uris = ArrayList<Uri>()
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) {
                val u = filesArr.optString(i)
                if (u.isNotBlank()) uris.add(Uri.parse(u))
            }
        }

        val combinedText = listOf(text, url).filter { it.isNotBlank() }.joinToString("\n")
        val intent = if (uris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                if (combinedText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, combinedText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else if (uris.size == 1) {
            val singleUri = uris[0]
            val mime = context.contentResolver.getType(singleUri) ?: "*/*"
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, singleUri)
                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                if (combinedText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, combinedText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, combinedText.ifBlank { title })
            }
        }

        val chooser = Intent.createChooser(intent, dialogTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun resolveDisplayName(cr: ContentResolver, uri: Uri): String? {
        try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment
    }

    private fun readUriBytes(cr: ContentResolver, uri: Uri): ByteArray {
        cr.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                total += n
                if (total > READ_MAX) throw IllegalStateException("File too large")
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        } ?: throw IllegalStateException("Cannot open selected image")
    }

    private fun shrinkToPng(cr: ContentResolver, uri: Uri, bytes: ByteArray): ByteArray? {
        return try {
            val bm: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(cr, uri)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val maxDim = maxOf(w, h)
                    if (maxDim > SHRINK_TO) {
                        decoder.setTargetSize(
                            maxOf(1, w * SHRINK_TO / maxDim),
                            maxOf(1, h * SHRINK_TO / maxDim)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                while (maxDim / (sample * 2) >= SHRINK_TO) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
                    inSampleSize = sample
                })
            }
            if (bm == null) return null
            val out = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.PNG, 100, out)
            bm.recycle()
            out.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "html", "htm" -> "text/html"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }
}
