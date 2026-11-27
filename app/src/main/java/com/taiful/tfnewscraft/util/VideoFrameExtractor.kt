package com.taiful.tfnewscraft.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedVideoFrame(
    val timestampMs: Long,
    val labelBn: String,
    val bitmap: Bitmap
)

data class LocalVideoExtractResult(
    val fileName: String,
    val durationMs: Long,
    val frames: List<ExtractedVideoFrame>
)

/**
 * Extracts crisp keyframe screenshots from a local device video URI using MediaMetadataRetriever
 * so creators can turn any phone video clip into News Photo Cards or Multi-News Reels slides.
 */
object VideoFrameExtractor {

    suspend fun extractFrames(context: Context, videoUri: Uri): LocalVideoExtractResult =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(1000L) ?: 10_000L

                val fileName = resolveDisplayName(context, videoUri)
                val samplePointsMs = listOf(
                    (durationMs * 0.08).toLong().coerceAtLeast(500L),
                    (durationMs * 0.28).toLong(),
                    (durationMs * 0.50).toLong(),
                    (durationMs * 0.72).toLong(),
                    (durationMs * 0.90).toLong()
                ).distinct()

                val frames = mutableListOf<ExtractedVideoFrame>()
                for (ms in samplePointsMs) {
                    val bmp = runCatching {
                        retriever.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }.getOrNull()
                    if (bmp != null) {
                        val scaled = scaleDownIfNeeded(bmp, maxDim = 1280)
                        if (scaled !== bmp) bmp.recycle()
                        frames.add(
                            ExtractedVideoFrame(
                                timestampMs = ms,
                                labelBn = formatTimestampBn(ms),
                                bitmap = scaled
                            )
                        )
                    }
                }
                LocalVideoExtractResult(
                    fileName = fileName,
                    durationMs = durationMs,
                    frames = frames
                )
            } finally {
                runCatching { retriever.release() }
            }
        }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return c.getString(idx).orEmpty().substringBeforeLast(".")
                }
            }
        }
        return "ভিডিও সংবাদ"
    }

    private fun scaleDownIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxDim) return src
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(
            src,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun formatTimestampBn(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        val raw = String.format("%d:%02d", min, sec)
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return raw.map { if (it in '0'..'9') bn[it - '0'] else it }.joinToString("")
    }
}
