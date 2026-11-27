package com.taiful.tfnewscraft.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.taiful.tfnewscraft.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zero-permission MediaStore saver (API 29+) and FileProvider share sheet helper
 * for PNG photo cards and MP4 reels.
 */
object MediaExportHelper {

    private const val APP_FOLDER = "TF FB Reels and Post Maker"

    fun savePngToGallery(context: Context, bitmap: Bitmap, prefix: String = "tf_card"): Result<Pair<String, Uri>> {
        return runCatching {
            val fileName = "${prefix}_${timestamp()}.png"
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("গ্যালারিতে ফাইল তৈরি করা যায়নি")
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: throw IllegalStateException("ছবি লেখা যায়নি")
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    itemUri
                } catch (e: Throwable) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir, APP_FOLDER)
                dir.mkdirs()
                val file = File(dir, fileName)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            fileName to uri
        }
    }

    fun saveMp4ToGallery(context: Context, sourceMp4: File, prefix: String = "tf_reel"): Result<Pair<String, Uri>> {
        return runCatching {
            val fileName = "${prefix}_${timestamp()}.mp4"
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$APP_FOLDER")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("ভিডিও ফাইল তৈরি করা যায়নি")
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        sourceMp4.inputStream().use { it.copyTo(out) }
                    } ?: throw IllegalStateException("ভিডিও লেখা যায়নি")
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    itemUri
                } catch (e: Throwable) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, APP_FOLDER)
                dir.mkdirs()
                val outFile = File(dir, fileName)
                sourceMp4.copyTo(outFile, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            }
            fileName to uri
        }
    }

    fun shareBitmapWithCaption(context: Context, bitmap: Bitmap, caption: String) {
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(shareDir, "tf_share_${timestamp()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.action_share_facebook))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareVideoUri(context: Context, uri: Uri, caption: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.action_share_facebook))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyMMdd_HHmmss", Locale.US).format(Date())
}
