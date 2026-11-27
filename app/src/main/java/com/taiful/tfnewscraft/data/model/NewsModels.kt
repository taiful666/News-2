package com.taiful.tfnewscraft.data.model

import android.graphics.Bitmap
import com.taiful.tfnewscraft.util.MonetizationMasker
import kotlinx.serialization.Serializable

enum class HomeMode(val titleBn: String, val subtitleBn: String) {
    LIVE_NEWS("লাইভ নিউজ", "প্রতি ১০ মি. গিটহাব ফিড"),
    MULTI_REELS("মাল্টি নিউজ রিলস", "একাধিক খবরে রিলস"),
    PAPER_AND_VIDEO("পত্রিকা ও ভিডিও", "পত্রিকায় ঢুকে / লিংক")
}

data class LiveFeedMetadata(
    val updatedIso: String = "",
    val updatedLabelBn: String = "",
    val isFromGitHubFeed: Boolean = false,
    val totalArticles: Int = 0
)

enum class LiveNewsTab(val labelBn: String) {
    BANGLADESH_HOT("বাংলাদেশ ও ভাইরাল"),
    INTERNATIONAL("আন্তর্জাতিক")
}

data class CuratedLiveArticle(
    val article: NewsArticle,
    val tab: LiveNewsTab,
    val badgeBn: String,
    val priorityScore: Int,
    val isHotMiddleEast: Boolean = false
)

@Serializable
data class NewspaperSource(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val domain: String,
    val homeUrl: String = "",
    val category: String = "পত্রিকা",
    val accentHex: Long = 0xFF153866L,
    val feedUrls: List<String> = emptyList()
)

@Serializable
data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val sourceId: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val publishedIso: String = "",
    val publishedLabel: String = ""
)

data class AiSubOption(
    val id: String,
    val labelBn: String
)

/**
 * Full 8 AI Studio Modes restored from the original app's MODES array.
 */
enum class AiStudioMode(
    val labelBn: String,
    val subtitleBn: String,
    val subOptions: List<AiSubOption>,
    val defaultOptionId: String
) {
    FACEBOOK(
        labelBn = "ফেসবুক পোস্ট",
        subtitleBn = "আকর্ষণীয় বাংলা ফেসবুক নিউজ পোস্ট ও হ্যাশট্যাগ",
        subOptions = listOf(
            AiSubOption("engage", "আকর্ষণীয় (ইমোজিসহ)"),
            AiSubOption("info", "তথ্যবহুল"),
            AiSubOption("formal", "আনুষ্ঠানিক")
        ),
        defaultOptionId = "engage"
    ),
    SUMMARY(
        labelBn = "সারসংক্ষেপ",
        subtitleBn = "মূল সংবাদের গোছানো বাংলা সারসংক্ষেপ",
        subOptions = listOf(
            AiSubOption("short", "ছোট (~৬০ শব্দ)"),
            AiSubOption("medium", "মাঝারি (~১২০ শব্দ)"),
            AiSubOption("long", "বিস্তারিত (~২৫০ শব্দ)")
        ),
        defaultOptionId = "medium"
    ),
    KEY_POINTS(
        labelBn = "মূল পয়েন্ট",
        subtitleBn = "বুলেট আকারে সংবাদের গুরুত্বপূর্ণ পয়েন্ট",
        subOptions = listOf(
            AiSubOption("5", "৫টি পয়েন্ট"),
            AiSubOption("7", "৭টি পয়েন্ট"),
            AiSubOption("10", "১০টি পয়েন্ট")
        ),
        defaultOptionId = "5"
    ),
    REWRITE(
        labelBn = "পুনর্লিখন",
        subtitleBn = "তথ্য ঠিক রেখে সম্পূর্ণ নতুন ভাষায় মৌলিক লেখা",
        subOptions = listOf(
            AiSubOption("pro", "পেশাদার সংবাদ ভাষা"),
            AiSubOption("simple", "সহজ ভাষা"),
            AiSubOption("formal", "আনুষ্ঠানিক প্রতিবেদন"),
            AiSubOption("story", "গল্পের ঢঙে")
        ),
        defaultOptionId = "pro"
    ),
    HEADLINES(
        labelBn = "শিরোনাম",
        subtitleBn = "ক্লিকযোগ্য ও আকর্ষণীয় একাধিক শিরোনাম",
        subOptions = listOf(
            AiSubOption("5", "৫টি শিরোনাম"),
            AiSubOption("8", "৮টি শিরোনাম"),
            AiSubOption("10", "১০টি শিরোনাম")
        ),
        defaultOptionId = "5"
    ),
    SHORT_NEWS(
        labelBn = "সংক্ষিপ্ত খবর",
        subtitleBn = "রিলস বা ব্রেকিং আপডেটের জন্য সংক্ষিপ্ত নিউজ",
        subOptions = listOf(
            AiSubOption("50", "~৫০ শব্দ"),
            AiSubOption("100", "~১০০ শব্দ"),
            AiSubOption("3l", "৩ লাইনে")
        ),
        defaultOptionId = "50"
    ),
    CREATIVE(
        labelBn = "সৃজনশীল লেখা",
        subtitleBn = "খবরের ওপর কবিতা, ছড়া, ব্যঙ্গরচনা বা গল্প",
        subOptions = listOf(
            AiSubOption("poem", "কবিতা"),
            AiSubOption("satire", "ব্যঙ্গরচনা"),
            AiSubOption("song", "হাস্যরসাত্মক গান"),
            AiSubOption("story", "ছোটগল্প"),
            AiSubOption("rhyme", "ছড়া")
        ),
        defaultOptionId = "poem"
    ),
    CUSTOM(
        labelBn = "নিজের নির্দেশনা",
        subtitleBn = "আপনার নিজস্ব প্রম্পট অনুযায়ী যেকোনো লেখা",
        subOptions = emptyList(),
        defaultOptionId = ""
    )
}

enum class AiOutputLanguage(val labelBn: String) {
    AUTO("স্বয়ংক্রিয়"),
    BENGALI("বাংলা"),
    ENGLISH("English")
}

enum class CardHookKind(val labelBn: String) {
    NEWS("সংবাদ কার্ড"),
    QUOTE("উক্তি / বক্তব্য কার্ড")
}

@Serializable
data class CardHeadlineOption(
    val headline: String,
    val highlightWord: String,
    val subCaption: String,
    val categoryBadge: String,
    val speakerName: String = "",
    val speakerRole: String = ""
)

enum class CardTemplate(val labelBn: String, val taglineBn: String, val isQuoteStyle: Boolean = false) {
    GRID_QUOTE("গ্রিড পেপার (উক্তি)", "উদ্ধৃতি ও বক্তার ছবি", isQuoteStyle = true),
    INSIGHT_ANALYSIS("বিশ্লেষণ কার্ড", "মতামত ও ফ্রেমড ছবি"),
    BREAKING_NEWS("ব্রেকিং নিউজ", "পূর্ণ ছবি ও লাল ব্যানার"),
    DARK_BROADCAST("ডার্ক স্টুডিও", "সিনেমাটিক ব্রডকাস্ট"),
    CLASSIC_CLIPPING("পত্রিকা ক্লিপিং", "ক্লাসিক নিউজপ্রিন্ট")
}

data class CardDesignPreset(
    val id: String,
    val labelBn: String,
    val template: CardTemplate,
    val accentArgb: Long,
    val highlightArgb: Long,
    val paperBgArgb: Long
)

enum class CardAspectRatio(val labelBn: String, val widthPx: Int, val heightPx: Int) {
    SQUARE("১:১ পোস্ট", 1080, 1080),
    PORTRAIT("৪:৫ ফিড", 1080, 1350),
    STORY("৯:১৬ স্টোরি", 1080, 1920)
}

enum class CardFontFamily(val labelBn: String) {
    SANS("সাধারণ (নোটো)"),
    SERIF("সেরিফ (পত্রিকা)"),
    TIRO("টিরো বাংলা")
}

enum class CardTextAlign(val labelBn: String) {
    LEFT("বামে"),
    CENTER("মাঝে"),
    RIGHT("ডানে")
}

enum class LogoPosition(val labelBn: String) {
    TOP_LEFT("উপরে বামে"),
    TOP_CENTER("উপরে মাঝে"),
    TOP_RIGHT("উপরে ডানে"),
    BOTTOM_LEFT("নিচে বামে"),
    BOTTOM_RIGHT("নিচে ডানে")
}

enum class CardImageSource(val labelBn: String) {
    ORIGINAL("মূল ছবি"),
    AI("AI ছবি"),
    UPLOAD("গ্যালারি"),
    NONE("ছবি ছাড়া")
}

data class PhotoCardConfig(
    val template: CardTemplate = CardTemplate.GRID_QUOTE,
    val aspectRatio: CardAspectRatio = CardAspectRatio.SQUARE,
    val fontFamily: CardFontFamily = CardFontFamily.SERIF,
    val textAlign: CardTextAlign = CardTextAlign.LEFT,
    val logoPosition: LogoPosition = LogoPosition.TOP_LEFT,
    val logoScale: Float = 1.0f,
    val imageSource: CardImageSource = CardImageSource.ORIGINAL,
    val photoZoom: Float = 1.0f,
    val headline: String = "",
    val highlightWord: String = "",
    val showHighlight: Boolean = true,
    val badgeText: String = "সর্বশেষ সংবাদ",
    val ownComment: String = "",
    val showComment: Boolean = false,
    val speakerName: String = "",
    val speakerRole: String = "",
    val flipSpeakerPhoto: Boolean = false,
    val sourceCredit: String = "",
    val showDateBadge: Boolean = true,
    val showSourceCredit: Boolean = true,
    val maskSensitiveWords: Boolean = true,
    val accentColorArgb: Long = 0xFFB91C1CL,
    val highlightColorArgb: Long = 0xFFFFD54FL,
    val paperBgColorArgb: Long = 0xFFF5EFE7L,
    val headlineScale: Float = 1.0f
)

enum class FacebookPostLength(val labelBn: String, val targetWords: Int) {
    SHORT("ছোট (~৬০ শব্দ)", 60),
    MEDIUM("মাঝারি (~১২০ শব্দ)", 120),
    LONG("বিস্তারিত (~২০০ শব্দ)", 200)
}

data class MaskSettings(
    val maskFacebookPost: Boolean = true,
    val maskPhotoCard: Boolean = true,
    val maskChar: String = "*",
    val sensitiveWords: List<String> = MonetizationMasker.DEFAULT_SENSITIVE_WORDS
)

enum class ReelTheme(
    val labelBn: String,
    val accentArgb: Long,
    val bgTopArgb: Long,
    val bgBottomArgb: Long,
    val highlightArgb: Long
) {
    BREAKING_RED("ব্রেকিং রেড", 0xFFD71920L, 0xFF1A1025L, 0xFF090D18L, 0xFFFFD54FL),
    MIDNIGHT_BLUE("মিডনাইট ব্লু", 0xFF2563EBL, 0xFF0F254DL, 0xFF070E1FL, 0xFFFDE047L),
    EMERALD_DESK("এমারেল্ড ডেস্ক", 0xFF059669L, 0xFF0B2B26L, 0xFF061311L, 0xFFFACC15L),
    ROYAL_PLUM("রয়াল স্টুডিও", 0xFF7C3AEDL, 0xFF22133EL, 0xFF0B0816L, 0xFFFDE047L),
    GOLD_BROADCAST("গোল্ড ব্রডকাস্ট", 0xFFD97706L, 0xFF1F1B14L, 0xFF0B0A08L, 0xFFFFF1A6L)
}

@Serializable
data class BrandSettings(
    val pageName: String = "TF News Desk",
    val pageHandle: String = "@tfnewsdesk",
    val logoFilePath: String = "",
    val defaultAccentArgb: Long = 0xFFD71920L,
    val darkMode: Boolean = false
)

data class ReelSlide(
    val id: String,
    val articleId: String = "",
    val headline: String,
    val highlightWord: String,
    val badgeText: String,
    val sourceName: String,
    val imageUrl: String,
    val customBitmap: Bitmap? = null,
    val durationSec: Int = 5
)

sealed interface AiTextUiState {
    data object Idle : AiTextUiState
    data object Loading : AiTextUiState
    data class Streaming(val text: String) : AiTextUiState
    data class Success(val text: String) : AiTextUiState
    data class Error(val message: String) : AiTextUiState
}

sealed interface CardOptionsUiState {
    data object Idle : CardOptionsUiState
    data object Loading : CardOptionsUiState
    data class Success(val options: List<CardHeadlineOption>) : CardOptionsUiState
    data class Error(val message: String) : CardOptionsUiState
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap, val promptUsed: String) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

sealed interface ReelExportState {
    data object Idle : ReelExportState
    data class Exporting(val percent: Int, val statusText: String) : ReelExportState
    data class Success(val fileName: String, val uriString: String) : ReelExportState
    data class Error(val message: String) : ReelExportState
}
