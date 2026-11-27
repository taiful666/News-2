package com.taiful.tfnewscraft.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.BrandSettings
import com.taiful.tfnewscraft.data.model.CardFontFamily
import com.taiful.tfnewscraft.data.model.CardImageSource
import com.taiful.tfnewscraft.data.model.CardTemplate
import com.taiful.tfnewscraft.data.model.CardTextAlign
import com.taiful.tfnewscraft.data.model.LogoPosition
import com.taiful.tfnewscraft.data.model.MaskSettings
import com.taiful.tfnewscraft.data.model.PhotoCardConfig
import com.taiful.tfnewscraft.data.model.ReelSlide
import com.taiful.tfnewscraft.data.model.ReelTheme
import com.taiful.tfnewscraft.util.MonetizationMasker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-resolution native Android Canvas renderer for Bengali News Photo Cards (including Quote
 * cards with Speaker Photo + Flip, font/alignment/logo position controls, photo zoom, and
 * auto monetization masking) and 9:16 Facebook Reels video frames with custom themes & tickers.
 */
class PhotoCardRenderer(context: Context) {

    private val sansTypeface: Typeface =
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
    private val serifTypeface: Typeface =
        ResourcesCompat.getFont(context, R.font.noto_serif_bengali) ?: Typeface.SERIF
    private val tiroTypeface: Typeface =
        ResourcesCompat.getFont(context, R.font.tiro_bangla) ?: serifTypeface

    private fun resolveHeadlineTypeface(font: CardFontFamily): Typeface = when (font) {
        CardFontFamily.SANS -> sansTypeface
        CardFontFamily.SERIF -> serifTypeface
        CardFontFamily.TIRO -> tiroTypeface
    }

    private fun resolveLayoutAlignment(align: CardTextAlign): Layout.Alignment = when (align) {
        CardTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
        CardTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
        CardTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

    private fun applyMaskIfEnabled(
        text: String,
        config: PhotoCardConfig,
        maskSettings: MaskSettings
    ): String {
        if (!config.maskSensitiveWords || !maskSettings.maskPhotoCard || text.isBlank()) return text
        return MonetizationMasker.maskText(
            text = text,
            maskSymbol = maskSettings.maskChar,
            words = maskSettings.sensitiveWords
        ).maskedText
    }

    fun renderPhotoCard(
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        speakerPhoto: Bitmap? = null,
        maskSettings: MaskSettings = MaskSettings(),
        scaleFactor: Float = 1.0f
    ): Bitmap {
        val w = (config.aspectRatio.widthPx * scaleFactor).toInt().coerceAtLeast(360)
        val h = (config.aspectRatio.heightPx * scaleFactor).toInt().coerceAtLeast(360)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val accent = config.accentColorArgb.toInt()
        val effectivePhoto = if (config.imageSource == CardImageSource.NONE) null else photo

        when (config.template) {
            CardTemplate.BREAKING_NEWS -> drawBreakingNews(canvas, w, h, config, brand, effectivePhoto, logo, maskSettings, accent)
            CardTemplate.GRID_QUOTE -> drawGridQuote(canvas, w, h, config, brand, effectivePhoto, speakerPhoto, logo, maskSettings, accent)
            CardTemplate.INSIGHT_ANALYSIS -> drawInsightAnalysis(canvas, w, h, config, brand, effectivePhoto, logo, maskSettings, accent)
            CardTemplate.DARK_BROADCAST -> drawDarkBroadcast(canvas, w, h, config, brand, effectivePhoto, logo, maskSettings, accent)
            CardTemplate.CLASSIC_CLIPPING -> drawClassicClipping(canvas, w, h, config, brand, effectivePhoto, logo, maskSettings, accent)
        }
        return out
    }

    fun renderReelFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        slide: ReelSlide,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        slideProgress: Float,
        slideIndex: Int,
        totalSlides: Int,
        theme: ReelTheme = ReelTheme.BREAKING_RED,
        tickerText: String = ""
    ) {
        val accent = theme.accentArgb.toInt()
        val bgTop = theme.bgTopArgb.toInt()
        val bgBottom = theme.bgBottomArgb.toInt()
        val hlColor = theme.highlightArgb.toInt()

        canvas.drawColor(bgBottom)

        // 1. Animated Ken Burns photo on upper 68%
        val photoHeight = height * 0.68f
        if (photo != null) {
            canvas.save()
            canvas.clipRect(0f, 0f, width.toFloat(), photoHeight)
            val zoom = 1.0f + 0.10f * slideProgress
            val panX = (slideProgress - 0.5f) * width * 0.04f
            drawBitmapCenterCrop(
                canvas,
                photo,
                RectF(
                    -width * (zoom - 1f) / 2f + panX,
                    -photoHeight * (zoom - 1f) / 2f,
                    width * (1f + (zoom - 1f) / 2f) + panX,
                    photoHeight * (1f + (zoom - 1f) / 2f)
                )
            )
            canvas.restore()
        } else {
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), photoHeight,
                    bgTop, bgBottom, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), photoHeight, bgPaint)
        }

        // Dramatic gradient scrim blending photo into lower news studio panel
        val scrimMid = Color.argb(210, Color.red(bgBottom), Color.green(bgBottom), Color.blue(bgBottom))
        val scrim = Paint().apply {
            shader = LinearGradient(
                0f, photoHeight * 0.35f, 0f, height.toFloat(),
                intArrayOf(Color.TRANSPARENT, scrimMid, bgBottom),
                floatArrayOf(0f, 0.45f, 0.72f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, photoHeight * 0.35f, width.toFloat(), height.toFloat(), scrim)

        // Top story progress bars
        val topPad = height * 0.045f
        val sidePad = width * 0.055f
        val gap = width * 0.015f
        val barWidth = (width - sidePad * 2f - gap * (totalSlides - 1)) / totalSlides.coerceAtLeast(1)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(95, 255, 255, 255) }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        for (i in 0 until totalSlides) {
            val left = sidePad + i * (barWidth + gap)
            val rect = RectF(left, topPad, left + barWidth, topPad + height * 0.006f)
            canvas.drawRoundRect(rect, 6f, 6f, trackPaint)
            val frac = when {
                i < slideIndex -> 1f
                i == slideIndex -> slideProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            if (frac > 0f) {
                canvas.drawRoundRect(
                    RectF(left, topPad, left + barWidth * frac, topPad + height * 0.006f),
                    6f, 6f, fillPaint
                )
            }
        }

        // Top brand pill
        val headerY = topPad + height * 0.032f
        drawBrandAtPosition(canvas, width, height, sidePad, LogoPosition.TOP_LEFT, width * 0.065f, brand, logo, darkText = false, topYOverride = headerY)

        // Category badge pill
        val revealAlpha = (slideProgress * 5f).coerceIn(0f, 1f)
        val slideOffsetY = (1f - revealAlpha) * height * 0.025f
        val contentTop = height * 0.55f + slideOffsetY

        drawBadgePill(
            canvas,
            text = slide.badgeText.ifBlank { "সর্বশেষ সংবাদ" },
            x = sidePad,
            y = contentTop,
            bgColor = accent,
            textColor = Color.WHITE,
            textSize = width * 0.034f
        )

        // Accent bar + animated headline
        val headlineTop = contentTop + height * 0.055f
        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(
            RectF(sidePad, headlineTop, sidePad + width * 0.014f, headlineTop + height * 0.18f),
            8f, 8f, accentBarPaint
        )

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(serifTypeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = width * 0.062f
            alpha = (revealAlpha * 255).toInt()
        }
        val spannable = buildHighlightedSpan(
            text = slide.headline.ifBlank { "সর্বশেষ সংবাদ আপডেট" },
            highlight = slide.highlightWord,
            highlightColor = hlColor
        )
        drawMultilineText(
            canvas,
            spannable,
            textPaint,
            left = sidePad + width * 0.038f,
            top = headlineTop,
            maxWidth = (width - sidePad * 2f - width * 0.038f).toInt(),
            maxLines = 5,
            alignment = Layout.Alignment.ALIGN_NORMAL
        )

        // Animated scrolling bottom news ticker strip
        val tickerTop = height * 0.895f
        val tickerHeight = height * 0.056f
        canvas.drawRect(0f, tickerTop, width.toFloat(), tickerTop + tickerHeight, Paint().apply { color = accent })
        val tickerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = width * 0.032f
        }
        val effectiveTicker = tickerText.trim().ifBlank {
            "${brand.pageName}  •  ${slide.sourceName.ifBlank { "নিউজ ডেস্ক" }}  •  ${todayBanglaDate()}"
        }
        val fullTicker = "$effectiveTicker   ✦   $effectiveTicker"
        val measured = tickerPaint.measureText("$effectiveTicker   ✦   ")
        val scrollOffset = (slideProgress * measured * 0.65f) % measured.coerceAtLeast(1f)
        canvas.save()
        canvas.clipRect(0f, tickerTop, width.toFloat(), tickerTop + tickerHeight)
        canvas.drawText(fullTicker, sidePad - scrollOffset, tickerTop + tickerHeight * 0.68f, tickerPaint)
        canvas.restore()
    }

    private fun drawBreakingNews(
        canvas: Canvas,
        w: Int,
        h: Int,
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        maskSettings: MaskSettings,
        accent: Int
    ) {
        canvas.drawColor(Color.parseColor("#0D1321"))
        val photoBottom = h * 0.62f

        if (photo != null) {
            drawBitmapCenterCrop(canvas, photo, RectF(0f, 0f, w.toFloat(), photoBottom), zoom = config.photoZoom)
        } else {
            val p = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), photoBottom,
                    Color.parseColor("#1D2D50"), Color.parseColor("#0D1321"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), photoBottom, p)
        }

        val scrim = Paint().apply {
            shader = LinearGradient(
                0f, photoBottom * 0.35f, 0f, h.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#E60D1321"), Color.parseColor("#FF0D1321")),
                floatArrayOf(0f, 0.45f, 0.65f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, photoBottom * 0.35f, w.toFloat(), h.toFloat(), scrim)

        val pad = w * 0.06f
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.012f, Paint().apply { color = accent })

        drawBrandAtPosition(canvas, w, h, pad, config.logoPosition, w * 0.062f * config.logoScale, brand, logo, darkText = false)
        if (config.showDateBadge && config.logoPosition != LogoPosition.TOP_RIGHT) {
            drawDatePillRight(canvas, w - pad, h * 0.042f, w * 0.026f, darkSurface = true)
        }

        val badgeY = h * 0.49f
        drawBadgePill(
            canvas,
            text = config.badgeText.ifBlank { "ব্রেকিং নিউজ" },
            x = pad,
            y = badgeY,
            bgColor = accent,
            textColor = Color.WHITE,
            textSize = w * 0.031f
        )

        val headlineY = badgeY + h * 0.068f
        val hlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(resolveHeadlineTypeface(config.fontFamily), Typeface.BOLD)
            color = Color.WHITE
            textSize = w * 0.056f * config.headlineScale
        }
        val rawHeadline = applyMaskIfEnabled(
            config.headline.ifBlank { "আপনার সংবাদের শিরোনাম এখানে দেখা যাবে" },
            config,
            maskSettings
        )
        val spannable = if (config.showHighlight) {
            buildHighlightedSpan(rawHeadline, config.highlightWord, config.highlightColorArgb.toInt())
        } else rawHeadline

        val usedH = drawMultilineText(
            canvas, spannable, hlPaint, pad, headlineY, (w - pad * 2).toInt(), 4,
            resolveLayoutAlignment(config.textAlign)
        )

        if (config.showComment && config.ownComment.isNotBlank()) {
            val cPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sansTypeface
                color = Color.parseColor("#CBD5E1")
                textSize = w * 0.030f
            }
            drawMultilineText(
                canvas,
                applyMaskIfEnabled(config.ownComment, config, maskSettings),
                cPaint,
                pad,
                headlineY + usedH + h * 0.022f,
                (w - pad * 2).toInt(),
                2,
                resolveLayoutAlignment(config.textAlign)
            )
        }

        drawCardFooter(canvas, w, h, pad, brand, config, accent, darkMode = true)
    }

    private fun drawGridQuote(
        canvas: Canvas,
        w: Int,
        h: Int,
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        speakerPhoto: Bitmap?,
        logo: Bitmap?,
        maskSettings: MaskSettings,
        accent: Int
    ) {
        val bg = config.paperBgColorArgb.toInt()
        canvas.drawColor(bg)
        val gridPaint = Paint().apply {
            color = Color.argb(38, Color.red(accent), Color.green(accent), Color.blue(accent))
            strokeWidth = 1.4f
        }
        val step = w / 22f
        var x = step
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
            x += step
        }
        var y = step
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
            y += step
        }

        val pad = w * 0.065f
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.015f, Paint().apply { color = accent })

        drawBrandAtPosition(canvas, w, h, pad, config.logoPosition, w * 0.060f * config.logoScale, brand, logo, darkText = true)
        if (config.showDateBadge && config.logoPosition != LogoPosition.TOP_RIGHT) {
            drawDatePillRight(canvas, w - pad, h * 0.052f, w * 0.025f, darkSurface = false)
        }

        // Giant decorative quote mark
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tiroTypeface
            color = Color.argb(65, Color.red(accent), Color.green(accent), Color.blue(accent))
            textSize = w * 0.22f
        }
        canvas.drawText("“", pad * 0.85f, h * 0.29f, quotePaint)

        drawBadgePill(
            canvas,
            text = config.badgeText.ifBlank { "মতামত" },
            x = pad,
            y = h * 0.155f,
            bgColor = accent,
            textColor = Color.WHITE,
            textSize = w * 0.028f
        )

        val hlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(resolveHeadlineTypeface(config.fontFamily), Typeface.BOLD)
            color = Color.parseColor("#18181B")
            textSize = w * 0.055f * config.headlineScale
        }
        val rawHeadline = applyMaskIfEnabled(
            config.headline.ifBlank { "সংবাদের মূল বক্তব্য বা উদ্ধৃতি এখানে বসবে" },
            config,
            maskSettings
        )
        val spannable = if (config.showHighlight) {
            buildHighlightedSpan(rawHeadline, config.highlightWord, accent)
        } else rawHeadline

        val usedH = drawMultilineText(
            canvas, spannable, hlPaint, pad, h * 0.24f, (w - pad * 2).toInt(), 5,
            resolveLayoutAlignment(config.textAlign)
        )

        if (config.showComment && config.ownComment.isNotBlank()) {
            val boxTop = (h * 0.26f + usedH).coerceAtMost(h * 0.56f)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(24, 0, 0, 0) }
            canvas.drawRoundRect(RectF(pad, boxTop, w - pad, boxTop + h * 0.092f), 16f, 16f, boxPaint)
            val cPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sansTypeface
                color = Color.parseColor("#27272A")
                textSize = w * 0.029f
            }
            drawMultilineText(
                canvas,
                applyMaskIfEnabled(config.ownComment, config, maskSettings),
                cPaint,
                pad + w * 0.03f,
                boxTop + h * 0.018f,
                (w - pad * 2.6f).toInt(),
                2,
                resolveLayoutAlignment(config.textAlign)
            )
        }

        // Speaker photo (preferred on quote card) or story inset photo, supporting horizontal flip!
        val portraitBmp = speakerPhoto ?: photo
        if (portraitBmp != null) {
            val imgW = w * 0.34f
            val imgH = h * 0.25f
            val left = w - pad - imgW
            val top = h * 0.64f
            val frameRect = RectF(left, top, left + imgW, top + imgH)
            drawRoundedBitmap(
                canvas,
                portraitBmp,
                frameRect,
                radius = 24f,
                zoom = if (speakerPhoto != null) 1.0f else config.photoZoom,
                flipHorizontal = config.flipSpeakerPhoto
            )
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = accent
            }
            canvas.drawRoundRect(frameRect, 24f, 24f, border)
        }

        // Speaker name & role nameplate box on lower-left (or source credit fallback)
        val hasSpeaker = config.speakerName.isNotBlank()
        if (hasSpeaker) {
            val boxLeft = pad
            val boxTop = h * 0.72f
            val maxBoxW = if (portraitBmp != null) w * 0.50f else w - pad * 2
            val nameBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
            val nameRect = RectF(boxLeft, boxTop, boxLeft + maxBoxW, boxTop + h * 0.068f)
            canvas.drawRoundRect(nameRect, 12f, 12f, nameBg)
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(sansTypeface, Typeface.BOLD)
                color = Color.WHITE
                textSize = w * 0.031f
            }
            canvas.drawText(config.speakerName.take(28), boxLeft + w * 0.025f, boxTop + h * 0.044f, namePaint)
            if (config.speakerRole.isNotBlank()) {
                val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = sansTypeface
                    color = Color.parseColor("#3F3F46")
                    textSize = w * 0.025f
                }
                canvas.drawText(config.speakerRole.take(36), boxLeft + w * 0.01f, boxTop + h * 0.102f, rolePaint)
            }
        } else if (config.showSourceCredit && config.sourceCredit.isNotBlank()) {
            val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(sansTypeface, Typeface.BOLD)
                color = Color.parseColor("#18181B")
                textSize = w * 0.030f
            }
            canvas.drawText("— ${config.sourceCredit}", pad, h * 0.80f, tagPaint)
        }

        drawCardFooter(canvas, w, h, pad, brand, config, accent, darkMode = false)
    }

    private fun drawInsightAnalysis(
        canvas: Canvas,
        w: Int,
        h: Int,
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        maskSettings: MaskSettings,
        accent: Int
    ) {
        canvas.drawColor(config.paperBgColorArgb.toInt())
        val pad = w * 0.06f
        canvas.drawRect(0f, 0f, w * 0.022f, h.toFloat(), Paint().apply { color = accent })

        drawBrandAtPosition(canvas, w, h, pad, config.logoPosition, w * 0.058f * config.logoScale, brand, logo, darkText = true)
        drawBadgePill(
            canvas,
            text = config.badgeText.ifBlank { "বিশ্লেষণ" },
            x = w - pad - w * 0.24f,
            y = h * 0.042f,
            bgColor = accent,
            textColor = Color.WHITE,
            textSize = w * 0.027f
        )

        val hlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(resolveHeadlineTypeface(config.fontFamily), Typeface.BOLD)
            color = Color.parseColor("#0F172A")
            textSize = w * 0.054f * config.headlineScale
        }
        val rawHeadline = applyMaskIfEnabled(
            config.headline.ifBlank { "মূল শিরোনাম ও বিশ্লেষণ এখানে থাকবে" },
            config,
            maskSettings
        )
        val spannable = if (config.showHighlight) {
            buildHighlightedSpan(rawHeadline, config.highlightWord, accent)
        } else rawHeadline

        val hlTop = h * 0.145f
        val hlHeight = drawMultilineText(
            canvas, spannable, hlPaint, pad, hlTop, (w - pad * 2).toInt(), 4,
            resolveLayoutAlignment(config.textAlign)
        )

        val photoTop = (hlTop + hlHeight + h * 0.03f).coerceAtMost(h * 0.44f)
        val photoBottom = h * 0.80f
        val photoRect = RectF(pad, photoTop, w - pad, photoBottom)
        if (photo != null) {
            drawRoundedBitmap(canvas, photo, photoRect, radius = 24f, zoom = config.photoZoom)
        } else {
            canvas.drawRoundRect(photoRect, 24f, 24f, Paint().apply { color = Color.parseColor("#E2E8F0") })
        }

        if (config.showComment && config.ownComment.isNotBlank()) {
            val calloutRect = RectF(pad + w * 0.03f, photoBottom - h * 0.11f, w - pad - w * 0.03f, photoBottom - h * 0.02f)
            canvas.drawRoundRect(calloutRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(225, 15, 23, 42) })
            val cp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sansTypeface
                color = Color.WHITE
                textSize = w * 0.028f
            }
            drawMultilineText(
                canvas,
                applyMaskIfEnabled(config.ownComment, config, maskSettings),
                cp,
                calloutRect.left + w * 0.025f,
                calloutRect.top + h * 0.016f,
                (calloutRect.width() - w * 0.05f).toInt(),
                2,
                resolveLayoutAlignment(config.textAlign)
            )
        }

        drawCardFooter(canvas, w, h, pad, brand, config, accent, darkMode = false)
    }

    private fun drawDarkBroadcast(
        canvas: Canvas,
        w: Int,
        h: Int,
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        maskSettings: MaskSettings,
        accent: Int
    ) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#0B1430"), Color.parseColor("#070B14"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val pad = w * 0.06f
        val photoRect = RectF(pad, h * 0.13f, w - pad, h * 0.56f)
        if (photo != null) {
            drawRoundedBitmap(canvas, photo, photoRect, radius = 22f, zoom = config.photoZoom)
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.argb(120, 255, 213, 79)
            }
            canvas.drawRoundRect(photoRect, 22f, 22f, border)
        }

        drawBrandAtPosition(canvas, w, h, pad, config.logoPosition, w * 0.058f * config.logoScale, brand, logo, darkText = false)
        if (config.showDateBadge && config.logoPosition != LogoPosition.TOP_RIGHT) {
            drawDatePillRight(canvas, w - pad, h * 0.048f, w * 0.025f, darkSurface = true)
        }

        val goldPaint = Paint().apply { color = Color.parseColor("#F59E0B") }
        canvas.drawRoundRect(RectF(pad, h * 0.59f, pad + w * 0.16f, h * 0.598f), 6f, 6f, goldPaint)

        val hlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(resolveHeadlineTypeface(config.fontFamily), Typeface.BOLD)
            color = Color.WHITE
            textSize = w * 0.054f * config.headlineScale
        }
        val rawHeadline = applyMaskIfEnabled(
            config.headline.ifBlank { "সিনেমাটিক ব্রডকাস্ট সংবাদ শিরোনাম" },
            config,
            maskSettings
        )
        val spannable = if (config.showHighlight) {
            buildHighlightedSpan(rawHeadline, config.highlightWord, config.highlightColorArgb.toInt())
        } else rawHeadline

        drawMultilineText(
            canvas, spannable, hlPaint, pad, h * 0.625f, (w - pad * 2).toInt(), 4,
            resolveLayoutAlignment(config.textAlign)
        )

        drawCardFooter(canvas, w, h, pad, brand, config, accent, darkMode = true)
    }

    private fun drawClassicClipping(
        canvas: Canvas,
        w: Int,
        h: Int,
        config: PhotoCardConfig,
        brand: BrandSettings,
        photo: Bitmap?,
        logo: Bitmap?,
        maskSettings: MaskSettings,
        accent: Int
    ) {
        canvas.drawColor(Color.parseColor("#F3ECE0"))
        val pad = w * 0.065f
        val ink = Color.parseColor("#1C1917")
        val rulePaint = Paint().apply { color = ink; strokeWidth = 5f }
        val thinRule = Paint().apply { color = Color.parseColor("#A8A29E"); strokeWidth = 2f }

        canvas.drawLine(pad, h * 0.04f, w - pad, h * 0.04f, rulePaint)
        canvas.drawLine(pad, h * 0.048f, w - pad, h * 0.048f, thinRule)

        val mastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(tiroTypeface, Typeface.BOLD)
            color = ink
            textSize = w * 0.062f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(brand.pageName.ifBlank { "TF News Desk" }, w / 2f, h * 0.115f, mastPaint)

        canvas.drawLine(pad, h * 0.14f, w - pad, h * 0.14f, thinRule)
        canvas.drawLine(pad, h * 0.148f, w - pad, h * 0.148f, rulePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            color = Color.parseColor("#57534E")
            textSize = w * 0.025f
        }
        canvas.drawText(config.badgeText.ifBlank { "বিশেষ প্রতিবেদন" }, pad, h * 0.18f, subPaint)
        if (config.showDateBadge) {
            subPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(todayBanglaDate(), w - pad, h * 0.18f, subPaint)
        }

        val hlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(resolveHeadlineTypeface(config.fontFamily), Typeface.BOLD)
            color = ink
            textSize = w * 0.056f * config.headlineScale
        }
        val rawHeadline = applyMaskIfEnabled(
            config.headline.ifBlank { "সংবাদপত্র ক্লিপিং শিরোনাম এখানে প্রদর্শিত হবে" },
            config,
            maskSettings
        )
        val spannable = if (config.showHighlight) {
            buildHighlightedSpan(rawHeadline, config.highlightWord, accent)
        } else rawHeadline

        val hlHeight = drawMultilineText(
            canvas, spannable, hlPaint, pad, h * 0.21f, (w - pad * 2).toInt(), 4,
            resolveLayoutAlignment(config.textAlign)
        )

        val photoTop = (h * 0.23f + hlHeight).coerceAtMost(h * 0.48f)
        val photoRect = RectF(pad, photoTop, w - pad, h * 0.84f)
        if (photo != null) {
            drawRoundedBitmap(canvas, photo, photoRect, radius = 8f, zoom = config.photoZoom)
            canvas.drawRect(photoRect, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = ink })
        }

        if (logo != null && (config.logoPosition == LogoPosition.BOTTOM_LEFT || config.logoPosition == LogoPosition.BOTTOM_RIGHT)) {
            drawBrandAtPosition(canvas, w, h, pad, config.logoPosition, w * 0.052f * config.logoScale, brand, logo, darkText = true)
        }

        canvas.drawLine(pad, h * 0.875f, w - pad, h * 0.875f, thinRule)
        drawCardFooter(canvas, w, h, pad, brand, config, accent, darkMode = false)
    }

    private fun drawBrandAtPosition(
        canvas: Canvas,
        w: Int,
        h: Int,
        pad: Float,
        position: LogoPosition,
        logoSize: Float,
        brand: BrandSettings,
        logo: Bitmap?,
        darkText: Boolean,
        topYOverride: Float? = null
    ) {
        val clampedSize = logoSize.coerceIn(w * 0.036f, w * 0.11f)
        val nameText = brand.pageName.ifBlank { "TF News Desk" }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            color = if (darkText) Color.parseColor("#18181B") else Color.WHITE
            textSize = clampedSize * 0.46f
        }
        val textW = namePaint.measureText(nameText)
        val totalW = (if (logo != null) clampedSize + clampedSize * 0.25f else 0f) + textW

        val y = topYOverride ?: when (position) {
            LogoPosition.TOP_LEFT, LogoPosition.TOP_CENTER, LogoPosition.TOP_RIGHT -> h * 0.038f
            LogoPosition.BOTTOM_LEFT, LogoPosition.BOTTOM_RIGHT -> h * 0.835f
        }
        val startX = when (position) {
            LogoPosition.TOP_LEFT, LogoPosition.BOTTOM_LEFT -> pad
            LogoPosition.TOP_CENTER -> (w - totalW) / 2f
            LogoPosition.TOP_RIGHT, LogoPosition.BOTTOM_RIGHT -> (w - pad - totalW).coerceAtLeast(pad)
        }

        var textX = startX
        if (logo != null) {
            val rect = RectF(startX, y, startX + clampedSize, y + clampedSize)
            drawRoundedBitmap(canvas, logo, rect, radius = clampedSize * 0.24f)
            textX += clampedSize + clampedSize * 0.25f
        }
        canvas.drawText(nameText, textX, y + clampedSize * 0.65f, namePaint)
    }

    private fun drawDatePillRight(canvas: Canvas, rightX: Float, y: Float, textSize: Float, darkSurface: Boolean) {
        val text = todayBanglaDate()
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            color = if (darkSurface) Color.WHITE else Color.parseColor("#3F3F46")
            this.textSize = textSize
        }
        val tw = tp.measureText(text)
        val padH = textSize * 0.7f
        val padV = textSize * 0.42f
        val rect = RectF(rightX - tw - padH * 2, y, rightX, y + textSize + padV * 2)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (darkSurface) Color.argb(140, 15, 23, 42) else Color.parseColor("#E7E5E4")
        }
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, bg)
        canvas.drawText(text, rect.left + padH, rect.bottom - padV * 0.9f, tp)
    }

    private fun drawBadgePill(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        bgColor: Int,
        textColor: Int,
        textSize: Float
    ) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            color = textColor
            this.textSize = textSize
        }
        val tw = tp.measureText(text)
        val padH = textSize * 0.75f
        val padV = textSize * 0.40f
        val rect = RectF(x, y, x + tw + padH * 2, y + textSize + padV * 2)
        canvas.drawRoundRect(rect, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor })
        canvas.drawText(text, x + padH, rect.bottom - padV * 0.95f, tp)
    }

    private fun drawCardFooter(
        canvas: Canvas,
        w: Int,
        h: Int,
        pad: Float,
        brand: BrandSettings,
        config: PhotoCardConfig,
        accent: Int,
        darkMode: Boolean
    ) {
        val footerY = h * 0.935f
        val mutedColor = if (darkMode) Color.parseColor("#94A3B8") else Color.parseColor("#57534E")
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            color = mutedColor
            textSize = w * 0.025f
        }
        val leftText = brand.pageHandle.ifBlank { brand.pageName }
        canvas.drawText(leftText, pad, footerY, tp)

        if (config.showSourceCredit && config.sourceCredit.isNotBlank()) {
            tp.textAlign = Paint.Align.RIGHT
            canvas.drawText("সূত্র: ${config.sourceCredit}", w - pad, footerY, tp)
        }
        canvas.drawRect(0f, h - h * 0.01f, w.toFloat(), h.toFloat(), Paint().apply { color = accent })
    }

    private fun buildHighlightedSpan(text: String, highlight: String, highlightColor: Int): CharSequence {
        if (highlight.isBlank()) return text
        val idx = text.indexOf(highlight.trim(), ignoreCase = true)
        if (idx < 0) return text
        return SpannableString(text).apply {
            setSpan(
                ForegroundColorSpan(highlightColor),
                idx,
                idx + highlight.trim().length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: CharSequence,
        paint: TextPaint,
        left: Float,
        top: Float,
        maxWidth: Int,
        maxLines: Int,
        alignment: Layout.Alignment
    ): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.coerceAtLeast(100))
            .setAlignment(alignment)
            .setLineSpacing(4f, 1.18f)
            .setMaxLines(maxLines)
            .build()
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun drawBitmapCenterCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: RectF,
        zoom: Float = 1.0f,
        flipHorizontal: Boolean = false
    ) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val safeZoom = zoom.coerceIn(1.0f, 2.5f)
        val dstRatio = dst.width() / dst.height()
        val bmpRatio = bw / bh

        val baseW: Float
        val baseH: Float
        if (bmpRatio > dstRatio) {
            baseH = bh
            baseW = bh * dstRatio
        } else {
            baseW = bw
            baseH = bw / dstRatio
        }
        val cropW = (baseW / safeZoom).toInt().coerceAtLeast(1)
        val cropH = (baseH / safeZoom).toInt().coerceAtLeast(1)
        val left = ((bw - cropW) / 2f).toInt().coerceAtLeast(0)
        val top = ((bh - cropH) / 2f).toInt().coerceAtLeast(0)
        val srcRect = Rect(left, top, (left + cropW).coerceAtMost(bitmap.width), (top + cropH).coerceAtMost(bitmap.height))

        if (flipHorizontal) {
            canvas.save()
            canvas.scale(-1f, 1f, dst.centerX(), dst.centerY())
            canvas.drawBitmap(bitmap, srcRect, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, srcRect, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }

    private fun drawRoundedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: RectF,
        radius: Float,
        zoom: Float = 1.0f,
        flipHorizontal: Boolean = false
    ) {
        val path = Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(path)
        drawBitmapCenterCrop(canvas, bitmap, dst, zoom = zoom, flipHorizontal = flipHorizontal)
        canvas.restore()
    }

    private fun todayBanglaDate(): String {
        val raw = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        return toBanglaDigits(raw)
    }

    private fun toBanglaDigits(input: String): String {
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return buildString(input.length) {
            for (ch in input) {
                if (ch in '0'..'9') append(bn[ch - '0']) else append(ch)
            }
        }
    }
}
