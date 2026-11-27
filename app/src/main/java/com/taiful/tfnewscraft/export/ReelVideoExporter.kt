package com.taiful.tfnewscraft.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.core.content.ContextCompat
import com.taiful.tfnewscraft.data.model.BrandSettings
import com.taiful.tfnewscraft.data.model.ReelSlide
import com.taiful.tfnewscraft.service.TFWorkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Native Android MediaCodec (H.264) + MediaMuxer MP4 Facebook Reels video exporter.
 * Draws each 720x1280 animated frame directly onto the hardware encoder's input Surface.
 */
class ReelVideoExporter(
    private val context: Context,
    private val renderer: PhotoCardRenderer
) {
    companion object {
        private const val VIDEO_WIDTH = 720
        private const val VIDEO_HEIGHT = 1280
        private const val FPS = 24
        private const val BITRATE = 3_500_000
    }

    suspend fun exportSlidesToMp4(
        slides: List<ReelSlide>,
        slideBitmaps: Map<String, Bitmap?>,
        brand: BrandSettings,
        logo: Bitmap?,
        theme: com.taiful.tfnewscraft.data.model.ReelTheme = com.taiful.tfnewscraft.data.model.ReelTheme.BREAKING_RED,
        tickerText: String = "",
        onProgress: (percent: Int, statusText: String) -> Unit
    ): File = withContext(Dispatchers.Default) {
        require(slides.isNotEmpty()) { "রিলস তৈরির জন্য কমপক্ষে একটি খবর যোগ করুন।" }

        startWorkService()
        val outFile = File(context.cacheDir, "tf_reel_export_${System.currentTimeMillis()}.mp4")
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = slides.sumOf { it.durationSec.coerceIn(3, 12) * FPS }
            var frameIndex = 0

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    bufferInfo.presentationTimeUs =
                                        (frameIndex.toLong() * 1_000_000L) / FPS.toLong()
                                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            slides.forEachIndexed { slideIdx, slide ->
                val framesForSlide = slide.durationSec.coerceIn(3, 12) * FPS
                val slidePhoto = slide.customBitmap ?: slideBitmaps[slide.id]
                for (f in 0 until framesForSlide) {
                    coroutineContext.ensureActive()
                    val progressInSlide = f.toFloat() / framesForSlide.toFloat()
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        renderer.renderReelFrame(
                            canvas = canvas,
                            width = VIDEO_WIDTH,
                            height = VIDEO_HEIGHT,
                            slide = slide,
                            brand = brand,
                            photo = slidePhoto,
                            logo = logo,
                            slideProgress = progressInSlide,
                            slideIndex = slideIdx,
                            totalSlides = slides.size,
                            theme = theme,
                            tickerText = tickerText
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    drainEncoder(endOfStream = false)
                    frameIndex++

                    if (frameIndex % 6 == 0 || frameIndex == totalFrames) {
                        val pct = ((frameIndex * 100) / totalFrames.coerceAtLeast(1)).coerceIn(1, 99)
                        val msg = "স্লাইড ${slideIdx + 1}/${slides.size} এনকোড হচ্ছে ($pct%)"
                        TFWorkService.instance?.updateProgress(pct, msg)
                        onProgress(pct, msg)
                    }
                }
            }

            drainEncoder(endOfStream = true)
            inputSurface.release()
            TFWorkService.instance?.finishWork(
                done = true,
                doneTitle = "রিলস ভিডিও তৈরি হয়েছে",
                doneText = "গ্যালারিতে সেভ হয়েছে — এখন ফেসবুকে শেয়ার করুন"
            )
            outFile
        } catch (e: Throwable) {
            TFWorkService.instance?.finishWork(done = false, doneTitle = "", doneText = "")
            outFile.delete()
            throw e
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun startWorkService() {
        runCatching {
            val intent = Intent(context, TFWorkService::class.java).apply {
                action = TFWorkService.ACTION_START
                putExtra("title", "রিলস ভিডিও তৈরি হচ্ছে")
                putExtra("text", "অ্যাপ মিনিমাইজ করলেও কাজ চলবে")
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
