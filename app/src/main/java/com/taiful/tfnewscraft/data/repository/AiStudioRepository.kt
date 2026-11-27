package com.taiful.tfnewscraft.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.taiful.tfnewscraft.data.model.AiOutputLanguage
import com.taiful.tfnewscraft.data.model.AiStudioMode
import com.taiful.tfnewscraft.data.model.CardHeadlineOption
import com.taiful.tfnewscraft.data.model.CardHookKind
import com.taiful.tfnewscraft.data.model.FacebookPostLength
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.data.model.ReelSlide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiStudioRepository"

/**
 * Firebase AI Logic repository powered by Gemini 3.8 Flash (streaming writing, structured JSON card hooks,
 * prompt suggestion, Reel voice-scripts) and Gemini Nano Banana 2.1 (copyright-free editorial image generation).
 */
class AiStudioRepository {

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val cardOptionsModel by lazy {
        val optionSchema = Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Catchy Bengali photo card headline or direct quote, 8 to 15 words, crisp and Facebook monetization-safe"),
                "highlightWord" to Schema.string(description = "1 to 3 exact key Bengali words from the headline to highlight"),
                "subCaption" to Schema.string(description = "One short punchy editorial comment or context line in Bengali"),
                "categoryBadge" to Schema.string(description = "Short 1-2 word Bengali category tag such as ব্রেকিং, রাজনীতি, অর্থনীতি, খেলা, প্রযুক্তি, মতামত"),
                "speakerName" to Schema.string(description = "Speaker full name in Bengali if this is a quote card, otherwise empty string"),
                "speakerRole" to Schema.string(description = "Speaker title/role in Bengali if this is a quote card, otherwise empty string")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "options" to Schema.array(
                    items = optionSchema,
                    description = "Exactly 3 distinct Bengali photo card headline options"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun streamStudioPost(
        article: NewsArticle,
        mode: AiStudioMode,
        subOptionId: String,
        language: AiOutputLanguage,
        extraInstruction: String
    ): Flow<String> = flow {
        val prompt = buildStudioPrompt(article, mode, subOptionId, language, extraInstruction)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            textModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
        }
    }

    suspend fun prepareFacebookPost(
        article: NewsArticle,
        length: FacebookPostLength
    ): String {
        val prompt = """
            আপনি একজন অভিজ্ঞ বাংলাদেশি ফেসবুক নিউজ এডিটর।
            নিচের সংবাদটি পড়ে ফেসবুক পেজের জন্য একটি আকর্ষণীয়, সম্পূর্ণ মৌলিক এবং মনিটাইজেশন-সেফ বাংলা পোস্ট লিখুন।
            - দৈর্ঘ্য: প্রায় ${length.targetWords} শব্দ।
            - প্রথমে একটি আকর্ষণীয় হুক লাইন দিন।
            - কোনো পত্রিকা বা চ্যানেলের নাম লিখবেন না।
            - শেষে পাঠকদের মতামত জানতে ১টি প্রশ্ন ও ৪-৫টি প্রাসঙ্গিক বাংলা হ্যাশট্যাগ দিন।

            শিরোনাম: ${article.title}
            বিবরণ: ${(article.body.ifBlank { article.summary }).take(2200)}
        """.trimIndent()

        val response = withTimeout(60_000L) { textModel.generateContent(prompt) }
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("ফেসবুক পোস্ট তৈরি করা যায়নি।")
    }

    suspend fun generateCardHeadlineOptions(
        article: NewsArticle,
        kind: CardHookKind
    ): List<CardHeadlineOption> {
        val kindPrompt = if (kind == CardHookKind.QUOTE) {
            """
            এই সংবাদ থেকে বক্তার সবচেয়ে শক্তিশালী ও চমকপ্রদ ৩টি 'উক্তি/বক্তব্য কার্ড' (Quote Card) অপশন তৈরি করুন।
            প্রতিটি অপশনে speakerName (বক্তার নাম) ও speakerRole (বক্তার পদবি/পরিচয়) অবশ্যই বাংলায় দিন।
            """.trimIndent()
        } else {
            """
            এই সংবাদের জন্য ফেসবুক ফটো কার্ডের ৩টি আলাদা, আকর্ষণীয়, কপিরাইট ও মনিটাইজেশন-সেফ বাংলা 'সংবাদ কার্ড' হেডলাইন তৈরি করুন।
            """.trimIndent()
        }

        val prompt = """
            আপনি একজন অভিজ্ঞ বাংলাদেশি নিউজরুম ফটো কার্ড এডিটর।
            $kindPrompt
            কোনো সহিংস বা নিষিদ্ধ শব্দ সরাসরি ব্যবহার করবেন না।

            শিরোনাম: ${article.title}
            বিবরণ: ${(article.body.ifBlank { article.summary }).take(1600)}
        """.trimIndent()

        val response = withTimeout(60_000L) {
            cardOptionsModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("মডেল থেকে কোনো ডেটা আসেনি।")
        val root = JSONObject(rawJson)
        val arr = root.getJSONArray("options")
        val options = mutableListOf<CardHeadlineOption>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            options.add(
                CardHeadlineOption(
                    headline = item.optString("headline").trim(),
                    highlightWord = item.optString("highlightWord").trim(),
                    subCaption = item.optString("subCaption").trim(),
                    categoryBadge = item.optString("categoryBadge").trim().ifBlank {
                        if (kind == CardHookKind.QUOTE) "মতামত" else "সর্বশেষ"
                    },
                    speakerName = item.optString("speakerName").trim(),
                    speakerRole = item.optString("speakerRole").trim()
                )
            )
        }
        if (options.isEmpty()) {
            throw IllegalStateException("কার্ড হেডলাইন তৈরি করা যায়নি।")
        }
        return options
    }

    suspend fun suggestImagePrompt(article: NewsArticle): String {
        val prompt = """
            Based on this news story, write a single crisp English visual prompt (max 35 words) for an editorial photojournalism illustration.
            Do NOT include any text, logos, watermarks, or real politician faces if sensitive; focus on symbolic, dramatic newsroom visual storytelling.
            Title: ${article.title}
            Summary: ${article.summary.take(500)}
            Return ONLY the visual prompt text.
        """.trimIndent()
        val response = withTimeout(45_000L) { textModel.generateContent(prompt) }
        return response.text?.trim()?.takeIf { it.isNotBlank() } ?: article.title
    }

    suspend fun generateReelVoiceScript(slides: List<ReelSlide>): String {
        val numbered = slides.mapIndexed { idx, s ->
            "${idx + 1}. ${s.headline}"
        }.joinToString("\n")

        val prompt = """
            আপনি একজন অভিজ্ঞ বাংলাদেশি টিভি ও ফেসবুক রিলস নিউজ প্রেজেন্টার।
            নিচের ${slides.size}টি খবরের জন্য একটি প্রাণবন্ত, টানটান বাংলা ভয়েস-ওভার স্ক্রিপ্ট লিখুন।
            গুরুত্বপূর্ণ নিয়ম:
            - কোনো পত্রিকা বা টিভি চ্যানেলের নাম কখনোই বলবেন না—শুধু মূল খবরটি বলুন।
            - সংখ্যা ও সংক্ষিপ্ত রূপ এমনভাবে লিখুন যেন ভয়েস সহজে পড়তে পারে।
            - নিচের ফরম্যাটে লিখুন:
              শুরু: (৫ সেকেন্ডের আকর্ষণীয় ওপেনিং)
              ১: (প্রথম খবরের ২-৩ বাক্যের প্রাণবন্ত বিবরণ)
              ...
              শেষ: (দর্শকদের ফলো ও কমেন্ট করার আহ্বান)

            খবরগুলোর তালিকা:
            $numbered
        """.trimIndent()

        val response = withTimeout(60_000L) { textModel.generateContent(prompt) }
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("ভয়েস স্ক্রিপ্ট তৈরি করা যায়নি।")
    }

    suspend fun generateEditorialImage(promptText: String, aspectLabel: String = "1:1"): Bitmap {
        val enrichedPrompt = """
            Create a clean, high-impact editorial news illustration suitable for a professional digital news photo card (framing $aspectLabel).
            No text, no watermarks, no newspaper logos, sharp lighting, cinematic photojournalism composition.
            Subject: $promptText
        """.trimIndent()

        val response = withTimeout(60_000L) {
            imageModel.generateContent(enrichedPrompt)
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()

        return imagePart?.image ?: run {
            Log.e(TAG, "Image model returned no ImagePart")
            throw IllegalStateException("মডেল কোনো ছবি তৈরি করেনি। প্রম্পট পরিবর্তন করে আবার চেষ্টা করুন।")
        }
    }

    private fun buildStudioPrompt(
        article: NewsArticle,
        mode: AiStudioMode,
        subOptionId: String,
        language: AiOutputLanguage,
        extraInstruction: String
    ): String {
        val contentSnippet = (article.body.ifBlank { article.summary }).take(2600)
        val subLabel = mode.subOptions.firstOrNull { it.id == subOptionId }?.labelBn.orEmpty()
        val langLine = when (language) {
            AiOutputLanguage.AUTO -> "খবরের মূল ভাষায় (বাংলা হলে বাংলায়) লিখুন।"
            AiOutputLanguage.BENGALI -> "সম্পূর্ণ প্রাঞ্জল বাংলায় লিখুন।"
            AiOutputLanguage.ENGLISH -> "Write the complete output in crisp, natural English."
        }

        val modeGuide = when (mode) {
            AiStudioMode.FACEBOOK -> "ফেসবুক পোস্ট লিখুন ($subLabel)। প্রথমে আকর্ষণীয় হুক, ২-৩ ছোট প্যারাগ্রাফে মূল তথ্য, শেষে পাঠকের প্রশ্ন ও ৪-৫টি হ্যাশট্যাগ।"
            AiStudioMode.SUMMARY -> "সংবাদটির পরিষ্কার সারসংক্ষেপ লিখুন ($subLabel)।"
            AiStudioMode.KEY_POINTS -> "সংবাদটির সবচেয়ে গুরুত্বপূর্ণ তথ্যগুলো বুলেট পয়েন্টে সাজান ($subLabel)।"
            AiStudioMode.REWRITE -> "মূল তথ্য শতভাগ ঠিক রেখে সম্পূর্ণ নতুন ও মৌলিক ভাষায় সংবাদটি পুনর্লিখন করুন ($subLabel)।"
            AiStudioMode.HEADLINES -> "সংবাদটির জন্য ক্লিকযোগ্য ও মানসম্মত শিরোনাম তৈরি করুন ($subLabel)।"
            AiStudioMode.SHORT_NEWS -> "রিলস বা তাৎক্ষণিক আপডেটের জন্য সংক্ষিপ্ত খবর লিখুন ($subLabel)।"
            AiStudioMode.CREATIVE -> "সংবাদটির বিষয়বস্তু নিয়ে সৃজনশীল লেখা তৈরি করুন ($subLabel)।"
            AiStudioMode.CUSTOM -> "ব্যবহারকারীর নিচের নির্দেশনা অনুসরণ করে সুন্দরভাবে উত্তর লিখুন।"
        }

        return buildString {
            appendLine("আপনি একজন প্রফেশনাল ডিজিটাল নিউজ এডিটর।")
            appendLine("কোনো পত্রিকা বা চ্যানেলের নাম লেখার ভেতরে উল্লেখ করবেন না। মূল তথ্য বিকৃত করবেন না।")
            appendLine(langLine)
            appendLine()
            appendLine("কাজের ধরন: $modeGuide")
            if (extraInstruction.isNotBlank()) {
                appendLine("অতিরিক্ত নির্দেশনা: $extraInstruction")
            }
            appendLine()
            appendLine("মূল শিরোনাম: ${article.title}")
            if (contentSnippet.isNotBlank()) {
                appendLine("মূল বিবরণ:")
                appendLine(contentSnippet)
            }
        }
    }
}
