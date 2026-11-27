package com.taiful.tfnewscraft.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.BrandSettings
import com.taiful.tfnewscraft.data.model.MaskSettings
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.data.model.NewspaperSource
import com.taiful.tfnewscraft.util.MonetizationMasker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class BrandPreferencesRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("tf_studio_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_BRAND = "brand_settings"
        private const val KEY_RECENT = "recent_articles"
        private const val KEY_CUSTOM_PAPERS = "custom_newspapers"
        private const val KEY_AD_FILTER = "paper_ad_filter_enabled"
        private const val KEY_MASK_FB = "mask_facebook_post"
        private const val KEY_MASK_CARD = "mask_photo_card"
        private const val KEY_MASK_CHAR = "mask_char"
        private const val KEY_MASK_WORDS = "mask_words_text"
        private const val LOGO_FILENAME = "page_logo.png"
    }

    fun loadBrandSettings(): BrandSettings {
        val raw = prefs.getString(KEY_BRAND, null) ?: return BrandSettings()
        return runCatching { json.decodeFromString<BrandSettings>(raw) }
            .getOrDefault(BrandSettings())
    }

    fun saveBrandSettings(settings: BrandSettings) {
        prefs.edit().putString(KEY_BRAND, json.encodeToString(settings)).apply()
    }

    fun loadAdFilterEnabled(): Boolean = prefs.getBoolean(KEY_AD_FILTER, true)

    fun saveAdFilterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AD_FILTER, enabled).apply()
    }

    fun loadCustomNewspapers(): List<NewspaperSource> {
        val raw = prefs.getString(KEY_CUSTOM_PAPERS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<NewspaperSource>>(raw) }
            .getOrDefault(emptyList())
    }

    fun saveCustomNewspapers(papers: List<NewspaperSource>) {
        prefs.edit().putString(KEY_CUSTOM_PAPERS, json.encodeToString(papers)).apply()
    }

    fun loadMaskSettings(): MaskSettings {
        val rawWords = prefs.getString(KEY_MASK_WORDS, null)
        val words = if (rawWords.isNullOrBlank()) {
            MonetizationMasker.DEFAULT_SENSITIVE_WORDS
        } else {
            rawWords.lines().map { it.trim() }.filter { it.length >= 2 }
        }
        return MaskSettings(
            maskFacebookPost = prefs.getBoolean(KEY_MASK_FB, true),
            maskPhotoCard = prefs.getBoolean(KEY_MASK_CARD, true),
            maskChar = prefs.getString(KEY_MASK_CHAR, "*") ?: "*",
            sensitiveWords = words.ifEmpty { MonetizationMasker.DEFAULT_SENSITIVE_WORDS }
        )
    }

    fun saveMaskSettings(settings: MaskSettings) {
        prefs.edit()
            .putBoolean(KEY_MASK_FB, settings.maskFacebookPost)
            .putBoolean(KEY_MASK_CARD, settings.maskPhotoCard)
            .putString(KEY_MASK_CHAR, settings.maskChar)
            .putString(KEY_MASK_WORDS, settings.sensitiveWords.joinToString("\n"))
            .apply()
    }

    fun saveLogoFromUri(uri: Uri): String? {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val maxDim = 512
            val scale = minOf(1f, maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height).toFloat())
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * scale).toInt().coerceAtLeast(1),
                    (rawBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also { rawBitmap.recycle() }
            } else {
                rawBitmap
            }
            val outFile = File(context.filesDir, LOGO_FILENAME)
            outFile.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            scaled.recycle()
            outFile.absolutePath
        }.getOrNull()
    }

    fun loadDefaultLogoBitmap(): Bitmap? {
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_tf_news_desk_logo)
        }.getOrNull()
    }

    fun loadLogoBitmap(path: String): Bitmap? {
        if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists()) {
                val custom = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                if (custom != null) return custom
            }
        }
        return loadDefaultLogoBitmap()
    }

    fun loadRecentArticles(): List<NewsArticle> {
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<NewsArticle>>(raw) }
            .getOrDefault(emptyList())
    }

    fun addRecentArticle(article: NewsArticle): List<NewsArticle> {
        val updated = (listOf(article) + loadRecentArticles().filterNot { it.id == article.id || it.title == article.title })
            .take(15)
        prefs.edit().putString(KEY_RECENT, json.encodeToString(updated)).apply()
        return updated
    }

    fun clearRecentArticles() {
        prefs.edit().remove(KEY_RECENT).apply()
    }
}
