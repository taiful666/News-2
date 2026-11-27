package com.taiful.tfnewscraft.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taiful.tfnewscraft.data.model.AiOutputLanguage
import com.taiful.tfnewscraft.data.model.AiStudioMode
import com.taiful.tfnewscraft.data.model.AiTextUiState
import com.taiful.tfnewscraft.data.model.BrandSettings
import com.taiful.tfnewscraft.data.model.CardDesignPreset
import com.taiful.tfnewscraft.data.model.CardHeadlineOption
import com.taiful.tfnewscraft.data.model.CardHookKind
import com.taiful.tfnewscraft.data.model.CardImageSource
import com.taiful.tfnewscraft.data.model.CardOptionsUiState
import com.taiful.tfnewscraft.data.model.CardTemplate
import com.taiful.tfnewscraft.data.model.CuratedLiveArticle
import com.taiful.tfnewscraft.data.model.FacebookPostLength
import com.taiful.tfnewscraft.data.model.HomeMode
import com.taiful.tfnewscraft.data.model.ImageGenUiState
import com.taiful.tfnewscraft.data.model.LiveFeedMetadata
import com.taiful.tfnewscraft.data.model.LiveNewsTab
import com.taiful.tfnewscraft.data.model.MaskSettings
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.data.model.NewspaperSource
import com.taiful.tfnewscraft.data.model.PhotoCardConfig
import com.taiful.tfnewscraft.data.model.ReelExportState
import com.taiful.tfnewscraft.data.model.ReelSlide
import com.taiful.tfnewscraft.data.model.ReelTheme
import com.taiful.tfnewscraft.data.repository.AiStudioRepository
import com.taiful.tfnewscraft.data.repository.BrandPreferencesRepository
import com.taiful.tfnewscraft.data.repository.NewsRepository
import com.taiful.tfnewscraft.export.MediaExportHelper
import com.taiful.tfnewscraft.export.PhotoCardRenderer
import com.taiful.tfnewscraft.export.ReelVideoExporter
import com.taiful.tfnewscraft.util.ExtractedVideoFrame
import com.taiful.tfnewscraft.util.LiveNewsClassifier
import com.taiful.tfnewscraft.util.MonetizationMasker
import com.taiful.tfnewscraft.util.VideoFrameExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

private const val TAG = "StudioViewModel"

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Favourite colour variants of "গ্রিড পেপার" (Quote) and "বিশ্লেষণ" (Insight) + Breaking/Dark/Clipping,
         * restored directly from the original app's FAV_Q & FAV_I design strip.
         */
        val CARD_DESIGN_PRESETS: List<CardDesignPreset> = listOf(
            CardDesignPreset("qgrid_red", "গ্রিড · লাল", CardTemplate.GRID_QUOTE, 0xFFB91C1CL, 0xFFFFF200L, 0xFFF5EFE7L),
            CardDesignPreset("qgrid_green", "গ্রিড · সবুজ", CardTemplate.GRID_QUOTE, 0xFF065F46L, 0xFFFDE047L, 0xFFEDF4EEL),
            CardDesignPreset("qgrid_blue", "গ্রিড · নীল", CardTemplate.GRID_QUOTE, 0xFF1E3A8AL, 0xFFFFE14DL, 0xFFEAF0F8L),
            CardDesignPreset("qgrid_teal", "গ্রিড · টিল", CardTemplate.GRID_QUOTE, 0xFF0B4F6CL, 0xFFFFD200L, 0xFFE9F2F3L),
            CardDesignPreset("qgrid_plum", "গ্রিড · বেগুনি", CardTemplate.GRID_QUOTE, 0xFF6B21A8L, 0xFFFFF200L, 0xFFF2EDF7L),
            CardDesignPreset("qgrid_black", "গ্রিড · কালো", CardTemplate.GRID_QUOTE, 0xFF111111L, 0xFFFFD400L, 0xFFF4ECD8L),
            CardDesignPreset("insight_red", "বিশ্লেষণ · লাল", CardTemplate.INSIGHT_ANALYSIS, 0xFF9F1239L, 0xFFFFD200L, 0xFFFFFFFFL),
            CardDesignPreset("insight_green", "বিশ্লেষণ · সবুজ", CardTemplate.INSIGHT_ANALYSIS, 0xFF14532DL, 0xFFFACC15L, 0xFFF3F6EFL),
            CardDesignPreset("insight_navy", "বিশ্লেষণ · নেভি", CardTemplate.INSIGHT_ANALYSIS, 0xFF1E293BL, 0xFFFDE047L, 0xFFF4F5F8L),
            CardDesignPreset("insight_purple", "বিশ্লেষণ · বেগুনি", CardTemplate.INSIGHT_ANALYSIS, 0xFF4C1D95L, 0xFFFCD34DL, 0xFFF6F3FAL),
            CardDesignPreset("insight_orange", "বিশ্লেষণ · কমলা", CardTemplate.INSIGHT_ANALYSIS, 0xFFC2410CL, 0xFFFFF1A6L, 0xFFFBF5EEL),
            CardDesignPreset("breaking_red", "ব্রেকিং নিউজ", CardTemplate.BREAKING_NEWS, 0xFFD71920L, 0xFFFFD54FL, 0xFF0D1321L),
            CardDesignPreset("dark_studio", "ডার্ক স্টুডিও", CardTemplate.DARK_BROADCAST, 0xFFD97706L, 0xFFFBBF24L, 0xFF0B1430L),
            CardDesignPreset("classic_clip", "পত্রিকা ক্লিপিং", CardTemplate.CLASSIC_CLIPPING, 0xFFB91C1CL, 0xFFB91C1CL, 0xFFF3ECE0L)
        )
    }

    private val newsRepo = NewsRepository()
    private val aiRepo = AiStudioRepository()
    private val brandRepo = BrandPreferencesRepository(application)
    val cardRenderer = PhotoCardRenderer(application)
    private val reelExporter = ReelVideoExporter(application, cardRenderer)

    val newspapers: List<NewspaperSource> = NewsRepository.NEWSPAPERS

    private val _customNewspapers = MutableStateFlow(brandRepo.loadCustomNewspapers())
    val customNewspapers: StateFlow<List<NewspaperSource>> = _customNewspapers.asStateFlow()

    val browseableNewspapers: List<NewspaperSource>
        get() = (NewsRepository.NEWSPAPERS.filter { it.homeUrl.isNotBlank() } + _customNewspapers.value)

    private val _adFilterEnabled = MutableStateFlow(brandRepo.loadAdFilterEnabled())
    val adFilterEnabled: StateFlow<Boolean> = _adFilterEnabled.asStateFlow()

    // Home Page navigation: null = Clean Home Hub (3 cards only), non-null = inside selected feature
    private val _activeHomeSection = MutableStateFlow<HomeMode?>(null)
    val activeHomeSection: StateFlow<HomeMode?> = _activeHomeSection.asStateFlow()

    private val _selectedNewspaper = MutableStateFlow(newspapers.first())
    val selectedNewspaper: StateFlow<NewspaperSource> = _selectedNewspaper.asStateFlow()

    private val _allLiveArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    private val _liveArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val liveArticles: StateFlow<List<NewsArticle>> = _liveArticles.asStateFlow()

    // Curated Live News screen state (2 tabs: Bangladesh Politics/Viral vs International/Iran-Israel-Palestine)
    private val _liveNewsTab = MutableStateFlow(LiveNewsTab.BANGLADESH_HOT)
    val liveNewsTab: StateFlow<LiveNewsTab> = _liveNewsTab.asStateFlow()

    private val _curatedLiveArticles = MutableStateFlow<List<CuratedLiveArticle>>(emptyList())
    val curatedLiveArticles: StateFlow<List<CuratedLiveArticle>> = _curatedLiveArticles.asStateFlow()

    private val _liveTabCounts = MutableStateFlow(0 to 0)
    val liveTabCounts: StateFlow<Pair<Int, Int>> = _liveTabCounts.asStateFlow()

    private val _liveFeedMeta = MutableStateFlow(LiveFeedMetadata())
    val liveFeedMeta: StateFlow<LiveFeedMetadata> = _liveFeedMeta.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private val _isFetchingUrl = MutableStateFlow(false)
    val isFetchingUrl: StateFlow<Boolean> = _isFetchingUrl.asStateFlow()

    // Local Video-to-News extracted frames
    private val _extractedVideoFrames = MutableStateFlow<List<ExtractedVideoFrame>>(emptyList())
    val extractedVideoFrames: StateFlow<List<ExtractedVideoFrame>> = _extractedVideoFrames.asStateFlow()

    private val _extractedVideoTitle = MutableStateFlow("")
    val extractedVideoTitle: StateFlow<String> = _extractedVideoTitle.asStateFlow()

    private val _isExtractingVideo = MutableStateFlow(false)
    val isExtractingVideo: StateFlow<Boolean> = _isExtractingVideo.asStateFlow()

    // In-app Newspaper Browser state
    private val _activeBrowserPaper = MutableStateFlow<NewspaperSource?>(null)
    val activeBrowserPaper: StateFlow<NewspaperSource?> = _activeBrowserPaper.asStateFlow()

    // Original Article Reader sheet state
    private val _showArticleReader = MutableStateFlow(false)
    val showArticleReader: StateFlow<Boolean> = _showArticleReader.asStateFlow()

    private val _recentArticles = MutableStateFlow(brandRepo.loadRecentArticles())
    val recentArticles: StateFlow<List<NewsArticle>> = _recentArticles.asStateFlow()

    private val _activeArticle = MutableStateFlow<NewsArticle?>(null)
    val activeArticle: StateFlow<NewsArticle?> = _activeArticle.asStateFlow()

    private val _brandSettings = MutableStateFlow(brandRepo.loadBrandSettings())
    val brandSettings: StateFlow<BrandSettings> = _brandSettings.asStateFlow()

    private val _logoBitmap = MutableStateFlow<Bitmap?>(null)
    val logoBitmap: StateFlow<Bitmap?> = _logoBitmap.asStateFlow()

    // AI Studio state (8 modes + sub-options + output language + undo + News/Quote card hooks)
    private val _selectedAiMode = MutableStateFlow(AiStudioMode.FACEBOOK)
    val selectedAiMode: StateFlow<AiStudioMode> = _selectedAiMode.asStateFlow()

    private val _selectedAiSubOption = MutableStateFlow(AiStudioMode.FACEBOOK.defaultOptionId)
    val selectedAiSubOption: StateFlow<String> = _selectedAiSubOption.asStateFlow()

    private val _selectedAiLanguage = MutableStateFlow(AiOutputLanguage.AUTO)
    val selectedAiLanguage: StateFlow<AiOutputLanguage> = _selectedAiLanguage.asStateFlow()

    private val _aiTextState = MutableStateFlow<AiTextUiState>(AiTextUiState.Idle)
    val aiTextState: StateFlow<AiTextUiState> = _aiTextState.asStateFlow()

    private val _aiTextHistory = MutableStateFlow<List<String>>(emptyList())
    val canUndoAiText: Boolean get() = _aiTextHistory.value.isNotEmpty()

    private val _selectedHookKind = MutableStateFlow(CardHookKind.NEWS)
    val selectedHookKind: StateFlow<CardHookKind> = _selectedHookKind.asStateFlow()

    private val _cardOptionsState = MutableStateFlow<CardOptionsUiState>(CardOptionsUiState.Idle)
    val cardOptionsState: StateFlow<CardOptionsUiState> = _cardOptionsState.asStateFlow()

    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    private val _isSuggestingPrompt = MutableStateFlow(false)
    val isSuggestingPrompt: StateFlow<Boolean> = _isSuggestingPrompt.asStateFlow()

    // Photo Card Studio state (supports original / AI / upload / none + speaker photo)
    private val _activePresetId = MutableStateFlow(CARD_DESIGN_PRESETS.first().id)
    val activePresetId: StateFlow<String> = _activePresetId.asStateFlow()

    private val _cardConfig = MutableStateFlow(
        PhotoCardConfig(accentColorArgb = _brandSettings.value.defaultAccentArgb)
    )
    val cardConfig: StateFlow<PhotoCardConfig> = _cardConfig.asStateFlow()

    private val _originalArticleBitmap = MutableStateFlow<Bitmap?>(null)
    private val _aiGeneratedBitmap = MutableStateFlow<Bitmap?>(null)
    private val _uploadedCardBitmap = MutableStateFlow<Bitmap?>(null)
    private val _cardPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val cardPhotoBitmap: StateFlow<Bitmap?> = _cardPhotoBitmap.asStateFlow()

    private val _speakerPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val speakerPhotoBitmap: StateFlow<Bitmap?> = _speakerPhotoBitmap.asStateFlow()

    private val _renderedCardPreview = MutableStateFlow<Bitmap?>(null)
    val renderedCardPreview: StateFlow<Bitmap?> = _renderedCardPreview.asStateFlow()

    // Facebook Post, Monetization Masking & Reels Maker state
    private val _postReelsTabIndex = MutableStateFlow(0)
    val postReelsTabIndex: StateFlow<Int> = _postReelsTabIndex.asStateFlow()

    private val _facebookPostLength = MutableStateFlow(FacebookPostLength.MEDIUM)
    val facebookPostLength: StateFlow<FacebookPostLength> = _facebookPostLength.asStateFlow()

    private val _isPreparingFacebook = MutableStateFlow(false)
    val isPreparingFacebook: StateFlow<Boolean> = _isPreparingFacebook.asStateFlow()

    private val _maskSettings = MutableStateFlow(brandRepo.loadMaskSettings())
    val maskSettings: StateFlow<MaskSettings> = _maskSettings.asStateFlow()

    private val _lastMaskReport = MutableStateFlow("")
    val lastMaskReport: StateFlow<String> = _lastMaskReport.asStateFlow()

    private val _facebookCaption = MutableStateFlow("")
    val facebookCaption: StateFlow<String> = _facebookCaption.asStateFlow()

    private val _reelSlides = MutableStateFlow<List<ReelSlide>>(emptyList())
    val reelSlides: StateFlow<List<ReelSlide>> = _reelSlides.asStateFlow()

    private val _reelSlideBitmaps = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
    val reelSlideBitmaps: StateFlow<Map<String, Bitmap?>> = _reelSlideBitmaps.asStateFlow()

    private val _reelTheme = MutableStateFlow(ReelTheme.BREAKING_RED)
    val reelTheme: StateFlow<ReelTheme> = _reelTheme.asStateFlow()

    private val _reelTickerText = MutableStateFlow("")
    val reelTickerText: StateFlow<String> = _reelTickerText.asStateFlow()

    private val _reelVoiceScript = MutableStateFlow("")
    val reelVoiceScript: StateFlow<String> = _reelVoiceScript.asStateFlow()

    private val _isGeneratingVoiceScript = MutableStateFlow(false)
    val isGeneratingVoiceScript: StateFlow<Boolean> = _isGeneratingVoiceScript.asStateFlow()

    private val _customMusicUri = MutableStateFlow<String?>(null)
    val customMusicUri: StateFlow<String?> = _customMusicUri.asStateFlow()

    private val _customMusicName = MutableStateFlow("ডিফল্ট নিউজ মিউজিক")
    val customMusicName: StateFlow<String> = _customMusicName.asStateFlow()

    private val _reelExportState = MutableStateFlow<ReelExportState>(ReelExportState.Idle)
    val reelExportState: StateFlow<ReelExportState> = _reelExportState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _logoBitmap.value = brandRepo.loadLogoBitmap(_brandSettings.value.logoFilePath)
            refreshPreviewBitmap()
        }
        refreshLiveFeed(forceRefresh = false)
    }

    // ---------- Home Navigation, Newspapers, Ad-Filter & Reader ----------

    fun openHomeSection(mode: HomeMode) {
        _activeHomeSection.value = mode
    }

    fun returnToHomeHub() {
        _activeHomeSection.value = null
    }

    fun setPostReelsTab(index: Int) {
        _postReelsTabIndex.value = index.coerceIn(0, 1)
    }

    fun openArticleReader() {
        if (_activeArticle.value == null) {
            emitToast("প্রথমে একটি খবর নির্বাচন করুন")
            return
        }
        _showArticleReader.value = true
    }

    fun closeArticleReader() {
        _showArticleReader.value = false
    }

    fun updateActiveArticleText(newTitle: String, newBody: String) {
        val current = _activeArticle.value ?: return
        val updated = current.copy(
            title = newTitle.trim().ifBlank { current.title },
            body = newBody.trim(),
            summary = newBody.trim().take(280)
        )
        _activeArticle.value = updated
        _cardConfig.update { it.copy(headline = updated.title) }
        schedulePreviewRefresh()
        emitToast("সংবাদের লেখা সংরক্ষিত হয়েছে")
    }

    fun toggleAdFilter(enabled: Boolean) {
        _adFilterEnabled.value = enabled
        brandRepo.saveAdFilterEnabled(enabled)
        emitToast(if (enabled) "বিজ্ঞাপন ফিল্টার চালু হয়েছে" else "বিজ্ঞাপন ফিল্টার বন্ধ হয়েছে")
    }

    fun addCustomNewspaper(nameBn: String, rawUrl: String) {
        val cleanName = nameBn.trim()
        val normalizedUrl = rawUrl.trim().let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
        }
        val host = runCatching { URL(normalizedUrl).host.removePrefix("www.") }.getOrDefault("")
        if (cleanName.isBlank() || host.isBlank() || !host.contains(".")) {
            emitToast("পত্রিকার নাম ও সঠিক ওয়েবসাইট লিংক দিন")
            return
        }
        val paper = NewspaperSource(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            nameBn = cleanName,
            nameEn = cleanName.take(2),
            domain = host,
            homeUrl = normalizedUrl,
            category = "কাস্টম",
            accentHex = 0xFF0F766EL
        )
        val updated = _customNewspapers.value + paper
        _customNewspapers.value = updated
        brandRepo.saveCustomNewspapers(updated)
        emitToast("$cleanName তালিকায় যুক্ত হয়েছে")
    }

    fun openNewspaperBrowser(paper: NewspaperSource) {
        if (paper.homeUrl.isBlank()) {
            emitToast("এই পত্রিকার সরাসরি লিংক পাওয়া যায়নি")
            return
        }
        _activeBrowserPaper.value = paper
    }

    fun closeNewspaperBrowser() {
        _activeBrowserPaper.value = null
    }

    // ---------- Local Video-to-News Frame Extraction ----------

    fun extractFramesFromLocalVideo(videoUri: Uri) {
        viewModelScope.launch {
            _isExtractingVideo.value = true
            try {
                val result = VideoFrameExtractor.extractFrames(getApplication(), videoUri)
                _extractedVideoFrames.value = result.frames
                _extractedVideoTitle.value = result.fileName
                if (result.frames.isEmpty()) {
                    emitToast("ভিডিও থেকে কোনো স্ক্রিনশট নেওয়া যায়নি")
                } else {
                    emitToast("ভিডিও থেকে ${result.frames.size}টি স্ক্রিনশট তৈরি হয়েছে")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video frame extraction failed", e)
                emitToast("ভিডিওটি পড়া যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            } finally {
                _isExtractingVideo.value = false
            }
        }
    }

    fun useVideoFrameForCard(frame: ExtractedVideoFrame, headlineHint: String, onDone: () -> Unit) {
        val cleanTitle = headlineHint.trim().ifBlank { _extractedVideoTitle.value.ifBlank { "ভিডিও সংবাদ" } }
        val article = NewsArticle(
            id = "video_${UUID.randomUUID()}",
            title = cleanTitle,
            summary = "ভিডিও স্ক্রিনশট (${frame.labelBn}) থেকে তৈরি সংবাদ",
            body = cleanTitle,
            sourceName = _brandSettings.value.pageName,
            publishedLabel = "ভিডিও (${frame.labelBn})"
        )
        _activeArticle.value = article
        _uploadedCardBitmap.value = frame.bitmap
        _cardPhotoBitmap.value = frame.bitmap
        _cardConfig.update {
            it.copy(
                headline = cleanTitle,
                imageSource = CardImageSource.UPLOAD,
                sourceCredit = _brandSettings.value.pageName
            )
        }
        schedulePreviewRefresh()
        emitToast("ভিডিওর স্ক্রিনশট ফটো কার্ডে বসানো হয়েছে")
        onDone()
    }

    fun addVideoFrameToReelBasket(frame: ExtractedVideoFrame, headlineHint: String) {
        val cleanTitle = headlineHint.trim().ifBlank { _extractedVideoTitle.value.ifBlank { "ভিডিও সংবাদ" } }
        val slideId = UUID.randomUUID().toString()
        val slide = ReelSlide(
            id = slideId,
            articleId = slideId,
            headline = cleanTitle,
            highlightWord = cleanTitle.split(" ").firstOrNull { it.length >= 4 }.orEmpty(),
            badgeText = "ভিডিও নিউজ",
            sourceName = _brandSettings.value.pageName,
            imageUrl = "",
            customBitmap = frame.bitmap,
            durationSec = 5
        )
        _reelSlides.update { it + slide }
        _reelSlideBitmaps.update { it + (slideId to frame.bitmap) }
        emitToast("ভিডিও স্ক্রিনশট রিলসে যোগ হয়েছে (${_reelSlides.value.size}টি)")
    }

    // ---------- 10-Min GitHub Live Feed & URL Fetching ----------

    fun refreshLiveFeed(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _isLoadingFeed.value = true
            _feedError.value = null
            try {
                val bundle = newsRepo.fetchLiveNewsBundle(forceRefresh = forceRefresh)
                _allLiveArticles.value = bundle.articles
                _liveFeedMeta.value = bundle.metadata
                applyFeedFilter()
                if (bundle.articles.isEmpty()) {
                    _feedError.value = "এই মুহূর্তে কোনো খবর লোড হয়নি। রিফ্রেশ করুন অথবা লিংক পেস্ট করুন।"
                } else if (forceRefresh) {
                    emitToast("সর্বশেষ ${bundle.articles.size}টি খবর আপডেট হয়েছে")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load GitHub live feed", e)
                _feedError.value = e.localizedMessage ?: "খবর লোড করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
            } finally {
                _isLoadingFeed.value = false
            }
        }
    }

    fun selectLiveNewsTab(tab: LiveNewsTab) {
        if (_liveNewsTab.value != tab) {
            _liveNewsTab.value = tab
            applyCuratedLiveFilter(_liveArticles.value)
        }
    }

    fun selectNewspaper(source: NewspaperSource) {
        _selectedNewspaper.value = source
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = ""
        }
        if (_allLiveArticles.value.isEmpty()) {
            refreshLiveFeed(forceRefresh = false)
        } else {
            applyFeedFilter()
        }
    }

    fun searchNews(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        if (trimmed.isBlank()) {
            applyFeedFilter()
            return
        }
        val localMatches = _allLiveArticles.value.filter { article ->
            article.title.contains(trimmed, ignoreCase = true) ||
                article.summary.contains(trimmed, ignoreCase = true) ||
                article.sourceName.contains(trimmed, ignoreCase = true)
        }
        _liveArticles.value = localMatches
        applyCuratedLiveFilter(localMatches, isSearchMode = true)

        viewModelScope.launch {
            _isLoadingFeed.value = localMatches.isEmpty()
            _feedError.value = null
            try {
                val remote = newsRepo.searchLiveNews(trimmed)
                val merged = (localMatches + remote).distinctBy { it.title.trim() }
                if (_searchQuery.value == trimmed) {
                    _liveArticles.value = merged
                    applyCuratedLiveFilter(merged, isSearchMode = true)
                    if (merged.isEmpty()) {
                        _feedError.value = "\"$trimmed\" নিয়ে কোনো খবর পাওয়া যায়নি।"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                if (localMatches.isEmpty()) {
                    _feedError.value = "খবর খোঁজা যায়নি। ইন্টারনেট পরীক্ষা করুন।"
                }
            } finally {
                _isLoadingFeed.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _feedError.value = null
        applyFeedFilter()
    }

    private fun applyFeedFilter() {
        val all = _allLiveArticles.value
        val paper = _selectedNewspaper.value
        val filtered = if (paper.id == "all") {
            all
        } else {
            all.filter { article ->
                article.sourceId.equals(paper.id, ignoreCase = true) ||
                    article.sourceName.contains(paper.nameBn, ignoreCase = true) ||
                    (paper.domain.isNotBlank() && article.sourceUrl.contains(paper.domain.substringBefore("/"), ignoreCase = true))
            }
        }
        _liveArticles.value = filtered
        applyCuratedLiveFilter(filtered, isSearchMode = false)
        _feedError.value = if (filtered.isEmpty() && all.isNotEmpty()) {
            "${paper.nameBn}-এর কোনো খবর সর্বশেষ ফিডে নেই। সরাসরি পত্রিকায় ঢুকতে নিচের বাটনে ট্যাপ করুন।"
        } else {
            null
        }
    }

    private fun applyCuratedLiveFilter(baseArticles: List<NewsArticle>, isSearchMode: Boolean = _searchQuery.value.isNotBlank()) {
        if (isSearchMode) {
            val nonBlocked = baseArticles.filterNot { LiveNewsClassifier.isBlockedFromLiveNews(it) }
            val mapped = nonBlocked.map { article ->
                LiveNewsClassifier.classify(article) ?: CuratedLiveArticle(
                    article = article,
                    tab = _liveNewsTab.value,
                    badgeBn = "খোঁজ",
                    priorityScore = 60
                )
            }
            _curatedLiveArticles.value = LiveNewsClassifier.sortForDisplay(mapped)
            return
        }

        val allClassified = LiveNewsClassifier.classifyAll(baseArticles)
        val bdItems = allClassified.filter { it.tab == LiveNewsTab.BANGLADESH_HOT }
        val intlItems = allClassified.filter { it.tab == LiveNewsTab.INTERNATIONAL }
        _liveTabCounts.value = bdItems.size to intlItems.size

        val activeList = when (_liveNewsTab.value) {
            LiveNewsTab.BANGLADESH_HOT -> bdItems
            LiveNewsTab.INTERNATIONAL -> intlItems
        }
        _curatedLiveArticles.value = LiveNewsClassifier.sortForDisplay(activeList)
    }

    fun importFromUrl(url: String, addToReelsOnly: Boolean = false, onImported: () -> Unit = {}) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            emitToast("একটি সঠিক সংবাদ বা ভিডিওর লিংক দিন")
            return
        }
        viewModelScope.launch {
            _isFetchingUrl.value = true
            try {
                val article = newsRepo.fetchArticleFromUrl(trimmed)
                if (addToReelsOnly) {
                    addArticleToReelBasket(article)
                    onImported()
                } else {
                    selectArticle(article)
                    emitToast("লিংক থেকে খবর সফলভাবে আনা হয়েছে")
                    onImported()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "URL import failed", e)
                emitToast("লিংক থেকে খবর আনা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            } finally {
                _isFetchingUrl.value = false
            }
        }
    }

    fun importBrowserPageToCard(url: String, titleHint: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isFetchingUrl.value = true
            try {
                val article = newsRepo.fetchArticleFromUrl(url, titleHint)
                selectArticle(article)
                _activeBrowserPaper.value = null
                emitToast("পত্রিকা থেকে খবর আনা হয়েছে")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Browser import failed", e)
                emitToast("এই পেজ থেকে খবর আনা যায়নি। নির্দিষ্ট কোনো খবরের ভেতরে ঢুকে চেষ্টা করুন।")
            } finally {
                _isFetchingUrl.value = false
            }
        }
    }

    fun addBrowserPageToReels(url: String, titleHint: String) {
        viewModelScope.launch {
            _isFetchingUrl.value = true
            try {
                val article = newsRepo.fetchArticleFromUrl(url, titleHint)
                addArticleToReelBasket(article)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Browser add to reels failed", e)
                emitToast("খবরটি রিলসে যোগ করা যায়নি। একটি নির্দিষ্ট খবরের পেজে যান।")
            } finally {
                _isFetchingUrl.value = false
            }
        }
    }

    fun importManualArticle(title: String, body: String, sourceName: String) {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() && cleanBody.isBlank()) {
            emitToast("কমপক্ষে শিরোনাম বা বিবরণ লিখুন")
            return
        }
        val article = NewsArticle(
            id = "manual_${UUID.randomUUID()}",
            title = cleanTitle.ifBlank { cleanBody.lineSequence().firstOrNull().orEmpty().take(90) },
            summary = cleanBody.take(260),
            body = cleanBody,
            sourceName = sourceName.trim().ifBlank { _brandSettings.value.pageName },
            publishedLabel = "নিজস্ব সংবাদ"
        )
        selectArticle(article)
        emitToast("সংবাদ যুক্ত হয়েছে")
    }

    fun clearActiveArticle() {
        _activeArticle.value = null
    }

    fun clearWorkspace() {
        _activeArticle.value = null
        _reelSlides.value = emptyList()
        _reelSlideBitmaps.value = emptyMap()
        _reelExportState.value = ReelExportState.Idle
        _reelVoiceScript.value = ""
        _reelTickerText.value = ""
        _aiTextState.value = AiTextUiState.Idle
        _aiTextHistory.value = emptyList()
        _cardOptionsState.value = CardOptionsUiState.Idle
        _imageGenState.value = ImageGenUiState.Idle
        _originalArticleBitmap.value = null
        _aiGeneratedBitmap.value = null
        _uploadedCardBitmap.value = null
        _cardPhotoBitmap.value = null
        _speakerPhotoBitmap.value = null
        _extractedVideoFrames.value = emptyList()
        _extractedVideoTitle.value = ""
        _cardConfig.value = PhotoCardConfig(accentColorArgb = _brandSettings.value.defaultAccentArgb)
        _facebookCaption.value = ""
        _lastMaskReport.value = ""
        _searchQuery.value = ""
        _selectedNewspaper.value = newspapers.first()
        _liveNewsTab.value = LiveNewsTab.BANGLADESH_HOT
        _activeBrowserPaper.value = null
        _showArticleReader.value = false
        _activeHomeSection.value = null
        applyFeedFilter()
        schedulePreviewRefresh()
        emitToast("হোম পেজ ও নির্বাচিত সব কাজ পরিষ্কার করা হয়েছে")
    }

    fun selectArticle(article: NewsArticle) {
        _activeArticle.value = article
        _recentArticles.value = brandRepo.addRecentArticle(article)

        val suggestedHighlight = article.title.split(" ").firstOrNull { it.length >= 4 }.orEmpty()
        _cardConfig.update {
            it.copy(
                headline = article.title,
                highlightWord = suggestedHighlight,
                sourceCredit = article.sourceName,
                ownComment = article.summary.take(110),
                imageSource = CardImageSource.ORIGINAL
            )
        }
        val rawCaption = buildString {
            appendLine(article.title)
            if (article.summary.isNotBlank()) {
                appendLine()
                appendLine(article.summary.take(360))
            }
            appendLine()
            append("#বাংলাদেশ #সর্বশেষ_সংবাদ #নিউজ")
        }
        setCaptionWithAutoMask(rawCaption)

        viewModelScope.launch {
            var enriched = article
            if ((article.imageUrl.isBlank() || article.body.length < 180) && article.sourceUrl.startsWith("http")) {
                runCatching { newsRepo.fetchArticleFromUrl(article.sourceUrl) }.getOrNull()?.let { fetched ->
                    enriched = article.copy(
                        body = fetched.body.ifBlank { article.body },
                        summary = fetched.summary.ifBlank { article.summary },
                        imageUrl = fetched.imageUrl.ifBlank { article.imageUrl }
                    )
                    _activeArticle.value = enriched
                }
            }
            if (enriched.imageUrl.isNotBlank()) {
                val bmp = newsRepo.loadBitmapFromUrl(enriched.imageUrl)
                if (bmp != null) {
                    _originalArticleBitmap.value = bmp
                    if (_cardConfig.value.imageSource == CardImageSource.ORIGINAL) {
                        _cardPhotoBitmap.value = bmp
                    }
                }
            }
            refreshPreviewBitmap()
        }
    }

    // ---------- AI Studio (All 8 Modes + Sub-Options + Language + Undo + 1-Tap Prep) ----------

    fun setAiMode(mode: AiStudioMode) {
        _selectedAiMode.value = mode
        _selectedAiSubOption.value = mode.defaultOptionId
    }

    fun setAiSubOption(optionId: String) {
        _selectedAiSubOption.value = optionId
    }

    fun setAiOutputLanguage(lang: AiOutputLanguage) {
        _selectedAiLanguage.value = lang
    }

    fun setCardHookKind(kind: CardHookKind) {
        _selectedHookKind.value = kind
    }

    fun undoAiText() {
        val hist = _aiTextHistory.value
        if (hist.isEmpty()) return
        val prev = hist.last()
        _aiTextHistory.value = hist.dropLast(1)
        _aiTextState.value = AiTextUiState.Success(prev)
        emitToast("পূর্বের লেখায় ফিরে যাওয়া হয়েছে")
    }

    fun generateAiPost(extraInstruction: String = "") {
        val article = _activeArticle.value ?: run {
            emitToast("প্রথমে একটি খবর নির্বাচন করুন বা লিখুন")
            return
        }
        val previousText = (_aiTextState.value as? AiTextUiState.Success)?.text
        if (!previousText.isNullOrBlank()) {
            _aiTextHistory.update { (it + previousText).takeLast(6) }
        }
        val mode = _selectedAiMode.value
        val subOpt = _selectedAiSubOption.value
        val lang = _selectedAiLanguage.value

        viewModelScope.launch {
            _aiTextState.value = AiTextUiState.Loading
            try {
                var finalText = ""
                aiRepo.streamStudioPost(article, mode, subOpt, lang, extraInstruction).collect { partial ->
                    finalText = partial
                    _aiTextState.value = AiTextUiState.Streaming(partial)
                }
                if (finalText.isNotBlank()) {
                    _aiTextState.value = AiTextUiState.Success(finalText)
                    if (mode == AiStudioMode.FACEBOOK) {
                        setCaptionWithAutoMask(finalText)
                    }
                } else {
                    _aiTextState.value = AiTextUiState.Error("মডেল থেকে কোনো লেখা আসেনি।")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI post timed out", e)
                _aiTextState.value = AiTextUiState.Error("৬০ সেকেন্ডের মধ্যে উত্তর আসেনি। আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI post failed", e)
                _aiTextState.value = AiTextUiState.Error(e.localizedMessage ?: "AI লেখা তৈরি করা যায়নি।")
            }
        }
    }

    fun applyTextToCardHeadline(text: String) {
        val firstLine = text.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty().take(130)
        if (firstLine.isBlank()) return
        _cardConfig.update { it.copy(headline = firstLine) }
        schedulePreviewRefresh()
        emitToast("ফটো কার্ডের শিরোনামে বসানো হয়েছে")
    }

    fun applyTextToCardComment(text: String) {
        val snippet = text.trim().take(160)
        if (snippet.isBlank()) return
        _cardConfig.update { it.copy(ownComment = snippet, showComment = true) }
        schedulePreviewRefresh()
        emitToast("ফটো কার্ডের মন্তব্যে বসানো হয়েছে")
    }

    /**
     * Restored 1-tap "ফেসবুকের জন্য প্রস্তুত করুন" (`prepFbBtn`):
     * Generates a fresh monetization-safe Facebook post + 3 card headline hooks at once!
     */
    fun prepareForFacebookOneTap(onReady: () -> Unit = {}) {
        val article = _activeArticle.value ?: run {
            emitToast("প্রথমে একটি খবর নির্বাচন করুন")
            return
        }
        viewModelScope.launch {
            _isPreparingFacebook.value = true
            try {
                val postText = aiRepo.prepareFacebookPost(article, _facebookPostLength.value)
                setCaptionWithAutoMask(postText)
                runCatching {
                    val options = aiRepo.generateCardHeadlineOptions(article, _selectedHookKind.value)
                    _cardOptionsState.value = CardOptionsUiState.Success(options)
                    options.firstOrNull()?.let { applyCardOption(it, silent = true) }
                }
                emitToast("ফেসবুক পোস্ট ও ফটো কার্ড প্রস্তুত হয়েছে")
                onReady()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "1-tap FB prep failed", e)
                emitToast("প্রস্তুত করা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            } finally {
                _isPreparingFacebook.value = false
            }
        }
    }

    fun generateCardHeadlineOptions() {
        val article = _activeArticle.value ?: run {
            emitToast("প্রথমে একটি খবর নির্বাচন করুন")
            return
        }
        viewModelScope.launch {
            _cardOptionsState.value = CardOptionsUiState.Loading
            try {
                val options = aiRepo.generateCardHeadlineOptions(article, _selectedHookKind.value)
                _cardOptionsState.value = CardOptionsUiState.Success(options)
            } catch (e: TimeoutCancellationException) {
                _cardOptionsState.value = CardOptionsUiState.Error("সময়সীমা শেষ হয়ে গেছে। আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Card options failed", e)
                _cardOptionsState.value = CardOptionsUiState.Error(e.localizedMessage ?: "হেডলাইন তৈরি করা যায়নি।")
            }
        }
    }

    fun applyCardOption(option: CardHeadlineOption, silent: Boolean = false) {
        _cardConfig.update {
            it.copy(
                headline = option.headline,
                highlightWord = option.highlightWord,
                ownComment = option.subCaption,
                badgeText = option.categoryBadge,
                showComment = option.subCaption.isNotBlank(),
                speakerName = option.speakerName.ifBlank { it.speakerName },
                speakerRole = option.speakerRole.ifBlank { it.speakerRole },
                template = if (option.speakerName.isNotBlank()) CardTemplate.GRID_QUOTE else it.template
            )
        }
        schedulePreviewRefresh()
        if (!silent) emitToast("হেডলাইন ফটো কার্ডে বসানো হয়েছে")
    }

    fun suggestAiImagePrompt(onSuggested: (String) -> Unit) {
        val article = _activeArticle.value ?: run {
            emitToast("প্রথমে একটি খবর নির্বাচন করুন")
            return
        }
        viewModelScope.launch {
            _isSuggestingPrompt.value = true
            try {
                val prompt = aiRepo.suggestImagePrompt(article)
                onSuggested(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitToast("প্রম্পট সাজেস্ট করা যায়নি")
            } finally {
                _isSuggestingPrompt.value = false
            }
        }
    }

    fun generateAiImage(customPrompt: String, aspectLabel: String = "1:1") {
        val prompt = customPrompt.trim().ifBlank { _activeArticle.value?.title.orEmpty() }
        if (prompt.isBlank()) {
            emitToast("ছবি তৈরির জন্য একটি বিষয় বা প্রম্পট লিখুন")
            return
        }
        viewModelScope.launch {
            _imageGenState.value = ImageGenUiState.Loading
            try {
                val bmp = aiRepo.generateEditorialImage(prompt, aspectLabel)
                _aiGeneratedBitmap.value = bmp
                _imageGenState.value = ImageGenUiState.Success(bmp, prompt)
            } catch (e: TimeoutCancellationException) {
                _imageGenState.value = ImageGenUiState.Error("ছবি তৈরি করতে ৬০ সেকেন্ডের বেশি সময় লেগেছে। আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image failed", e)
                _imageGenState.value = ImageGenUiState.Error(e.localizedMessage ?: "AI ছবি তৈরি করা যায়নি।")
            }
        }
    }

    fun applyAiImageToCard(bitmap: Bitmap) {
        _aiGeneratedBitmap.value = bitmap
        _cardPhotoBitmap.value = bitmap
        _cardConfig.update { it.copy(imageSource = CardImageSource.AI) }
        schedulePreviewRefresh()
        emitToast("AI ছবি ফটো কার্ডে যুক্ত হয়েছে")
    }

    fun saveAiImageToGallery(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = MediaExportHelper.savePngToGallery(getApplication(), bitmap)
            res.onSuccess { (name, _) -> emitToast("AI ছবি গ্যালারিতে সেভ হয়েছে: $name") }
                .onFailure { emitToast("ছবি সেভ করা যায়নি") }
        }
    }

    // ---------- Photo Card Studio (Presets, Next Design, Reset, Speaker Photo & Flip, Image Source) ----------

    fun applyDesignPreset(preset: CardDesignPreset) {
        _activePresetId.value = preset.id
        _cardConfig.update {
            it.copy(
                template = preset.template,
                accentColorArgb = preset.accentArgb,
                highlightColorArgb = preset.highlightArgb,
                paperBgColorArgb = preset.paperBgArgb
            )
        }
        schedulePreviewRefresh()
    }

    fun cycleNextCardDesign() {
        val idx = CARD_DESIGN_PRESETS.indexOfFirst { it.id == _activePresetId.value }
        val next = CARD_DESIGN_PRESETS[(idx + 1) % CARD_DESIGN_PRESETS.size]
        applyDesignPreset(next)
        emitToast("ডিজাইন: ${next.labelBn}")
    }

    fun resetCardDesign() {
        val currentHeadline = _cardConfig.value.headline
        val currentSource = _cardConfig.value.sourceCredit
        val preset = CARD_DESIGN_PRESETS.first()
        _activePresetId.value = preset.id
        _cardConfig.value = PhotoCardConfig(
            headline = currentHeadline,
            sourceCredit = currentSource,
            accentColorArgb = preset.accentArgb,
            highlightColorArgb = preset.highlightArgb,
            paperBgColorArgb = preset.paperBgArgb
        )
        schedulePreviewRefresh()
        emitToast("ফটো কার্ড ডিজাইন রিসেট হয়েছে")
    }

    fun setCardImageSource(source: CardImageSource) {
        _cardConfig.update { it.copy(imageSource = source) }
        _cardPhotoBitmap.value = when (source) {
            CardImageSource.ORIGINAL -> _originalArticleBitmap.value
            CardImageSource.AI -> _aiGeneratedBitmap.value
            CardImageSource.UPLOAD -> _uploadedCardBitmap.value
            CardImageSource.NONE -> null
        }
        schedulePreviewRefresh()
    }

    fun updateCardConfig(transform: (PhotoCardConfig) -> PhotoCardConfig) {
        _cardConfig.update(transform)
        schedulePreviewRefresh()
    }

    fun setCustomCardPhotoFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = decodeBitmapFromUri(uri)
            if (bmp != null) {
                _uploadedCardBitmap.value = bmp
                _cardPhotoBitmap.value = bmp
                _cardConfig.update { it.copy(imageSource = CardImageSource.UPLOAD) }
                refreshPreviewBitmap()
                emitToast("ছবি ফটো কার্ডে যুক্ত হয়েছে")
            } else {
                emitToast("ছবিটি পড়া যায়নি")
            }
        }
    }

    fun setSpeakerPhotoFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = decodeBitmapFromUri(uri)
            if (bmp != null) {
                _speakerPhotoBitmap.value = bmp
                _cardConfig.update { it.copy(template = CardTemplate.GRID_QUOTE) }
                refreshPreviewBitmap()
                emitToast("বক্তার ছবি যুক্ত হয়েছে")
            } else {
                emitToast("ছবিটি পড়া যায়নি")
            }
        }
    }

    fun removeSpeakerPhoto() {
        _speakerPhotoBitmap.value = null
        schedulePreviewRefresh()
        emitToast("বক্তার ছবি সরানো হয়েছে")
    }

    fun toggleFlipSpeakerPhoto() {
        _cardConfig.update { it.copy(flipSpeakerPhoto = !it.flipSpeakerPhoto) }
        schedulePreviewRefresh()
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? = runCatching {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }.getOrNull()

    fun savePhotoCardToGallery() {
        viewModelScope.launch(Dispatchers.Default) {
            val fullRes = cardRenderer.renderPhotoCard(
                config = _cardConfig.value,
                brand = _brandSettings.value,
                photo = _cardPhotoBitmap.value,
                logo = _logoBitmap.value,
                speakerPhoto = _speakerPhotoBitmap.value,
                maskSettings = _maskSettings.value,
                scaleFactor = 1.0f
            )
            val result = MediaExportHelper.savePngToGallery(getApplication(), fullRes)
            fullRes.recycle()
            result.onSuccess { (name, _) ->
                emitToast("গ্যালারিতে সেভ হয়েছে: $name")
            }.onFailure { err ->
                emitToast("সেভ করা যায়নি: ${err.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    fun sharePhotoCard(caption: String = _facebookCaption.value) {
        viewModelScope.launch(Dispatchers.Default) {
            val fullRes = cardRenderer.renderPhotoCard(
                config = _cardConfig.value,
                brand = _brandSettings.value,
                photo = _cardPhotoBitmap.value,
                logo = _logoBitmap.value,
                speakerPhoto = _speakerPhotoBitmap.value,
                maskSettings = _maskSettings.value,
                scaleFactor = 1.0f
            )
            withContext(Dispatchers.Main) {
                runCatching {
                    MediaExportHelper.shareBitmapWithCaption(getApplication(), fullRes, caption)
                }.onFailure { err ->
                    emitToast("শেয়ার করা যায়নি: ${err.localizedMessage ?: "ত্রুটি"}")
                }
            }
            fullRes.recycle()
        }
    }

    // ---------- Facebook Post & Monetization Masking Engine ----------

    fun setFacebookPostLength(length: FacebookPostLength) {
        _facebookPostLength.value = length
    }

    fun updateFacebookCaption(text: String) {
        _facebookCaption.value = text
    }

    private fun setCaptionWithAutoMask(rawText: String) {
        val ms = _maskSettings.value
        if (ms.maskFacebookPost) {
            val res = MonetizationMasker.maskText(rawText, ms.maskChar, ms.sensitiveWords)
            _facebookCaption.value = res.maskedText
            _lastMaskReport.value = if (res.matchedWords.isNotEmpty()) {
                "${res.matchedWords.size}টি সংবেদনশীল শব্দ মাস্ক হয়েছে: ${res.matchedWords.take(6).joinToString(", ")}"
            } else ""
        } else {
            _facebookCaption.value = rawText
            _lastMaskReport.value = ""
        }
    }

    fun maskFacebookCaptionNow() {
        val ms = _maskSettings.value
        val res = MonetizationMasker.maskText(_facebookCaption.value, ms.maskChar, ms.sensitiveWords)
        _facebookCaption.value = res.maskedText
        if (res.matchedWords.isNotEmpty()) {
            val msg = "${res.matchedWords.size}টি শব্দ মাস্ক করা হয়েছে: ${res.matchedWords.take(5).joinToString(", ")}"
            _lastMaskReport.value = msg
            emitToast(msg)
        } else {
            _lastMaskReport.value = "কোনো সংবেদনশীল শব্দ পাওয়া যায়নি — পোস্ট নিরাপদ আছে"
            emitToast("কোনো সংবেদনশীল শব্দ পাওয়া যায়নি")
        }
    }

    fun updateMaskSettings(transform: (MaskSettings) -> MaskSettings) {
        val updated = transform(_maskSettings.value)
        _maskSettings.value = updated
        brandRepo.saveMaskSettings(updated)
        schedulePreviewRefresh()
    }

    fun resetMaskWordsToDefault() {
        updateMaskSettings { it.copy(sensitiveWords = MonetizationMasker.DEFAULT_SENSITIVE_WORDS) }
        emitToast("ডিফল্ট শব্দ তালিকা ফিরিয়ে আনা হয়েছে")
    }

    fun copyFacebookCaption() {
        val text = _facebookCaption.value.trim()
        if (text.isBlank()) {
            emitToast("কপি করার মতো কোনো ক্যাপশন নেই")
            return
        }
        MediaExportHelper.copyToClipboard(getApplication(), "Facebook Caption", text)
        emitToast("ক্যাপশন কপি হয়েছে")
    }

    // ---------- Multi-News Reels Studio (Theme, Ticker, Voice-Script, Reorder, Custom Music) ----------

    fun setReelTheme(theme: ReelTheme) {
        _reelTheme.value = theme
    }

    fun shuffleReelTheme() {
        val all = ReelTheme.entries
        val next = all[(all.indexOf(_reelTheme.value) + 1) % all.size]
        _reelTheme.value = next
        emitToast("রিলস থিম: ${next.labelBn}")
    }

    fun updateReelTickerText(text: String) {
        _reelTickerText.value = text
    }

    fun updateReelVoiceScript(text: String) {
        _reelVoiceScript.value = text
    }

    fun setCustomMusicFromUri(uri: Uri, displayName: String) {
        _customMusicUri.value = uri.toString()
        _customMusicName.value = displayName.ifBlank { "কাস্টম মিউজিক" }
        emitToast("কাস্টম ব্যাকগ্রাউন্ড মিউজিক যুক্ত হয়েছে")
    }

    fun resetToDefaultMusic() {
        _customMusicUri.value = null
        _customMusicName.value = "ডিফল্ট নিউজ মিউজিক"
        emitToast("ডিফল্ট নিউজ মিউজিক চালু হয়েছে")
    }

    fun generateReelVoiceScriptAi() {
        val slides = _reelSlides.value
        if (slides.isEmpty()) {
            emitToast("প্রথমে রিলস তালিকায় কমপক্ষে ১টি খবর যোগ করুন")
            return
        }
        viewModelScope.launch {
            _isGeneratingVoiceScript.value = true
            try {
                val script = aiRepo.generateReelVoiceScript(slides)
                _reelVoiceScript.value = script
                emitToast("ভয়েস-ওভার স্ক্রিপ্ট তৈরি হয়েছে")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Reel voice script failed", e)
                emitToast("স্ক্রিপ্ট তৈরি করা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}")
            } finally {
                _isGeneratingVoiceScript.value = false
            }
        }
    }

    fun toggleArticleInReelBasket(article: NewsArticle) {
        val existing = _reelSlides.value.firstOrNull {
            (it.articleId.isNotBlank() && it.articleId == article.id) || it.headline == article.title
        }
        if (existing != null) {
            _reelSlides.update { it.filterNot { s -> s.id == existing.id } }
            emitToast("রিলস তালিকা থেকে সরানো হয়েছে (${_reelSlides.value.size}টি বাকি)")
        } else {
            addArticleToReelBasket(article)
        }
    }

    fun addArticleToReelBasket(article: NewsArticle) {
        val current = _reelSlides.value
        if (current.any { (it.articleId.isNotBlank() && it.articleId == article.id) || it.headline == article.title }) {
            emitToast("এই খবরটি আগেই রিলস তালিকায় আছে")
            return
        }
        val slideId = UUID.randomUUID().toString()
        val slide = ReelSlide(
            id = slideId,
            articleId = article.id,
            headline = article.title,
            highlightWord = article.title.split(" ").firstOrNull { it.length >= 4 }.orEmpty(),
            badgeText = "ব্রেকিং নিউজ",
            sourceName = article.sourceName.ifBlank { _brandSettings.value.pageName },
            imageUrl = article.imageUrl,
            durationSec = 5
        )
        _reelSlides.value = current + slide
        emitToast("রিলস তালিকায় যোগ হয়েছে (${_reelSlides.value.size}টি)")

        if (article.imageUrl.isNotBlank()) {
            viewModelScope.launch {
                val bmp = newsRepo.loadBitmapFromUrl(article.imageUrl)
                if (bmp != null) {
                    _reelSlideBitmaps.update { it + (slideId to bmp) }
                }
            }
        }
    }

    fun addManualArticleToReelBasket(title: String, body: String, sourceName: String) {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() && cleanBody.isBlank()) {
            emitToast("কমপক্ষে শিরোনাম বা সংবাদের লেখা পেস্ট করুন")
            return
        }
        val article = NewsArticle(
            id = "manual_reel_${UUID.randomUUID()}",
            title = cleanTitle.ifBlank { cleanBody.lineSequence().firstOrNull().orEmpty().take(110) },
            summary = cleanBody.take(260),
            body = cleanBody,
            sourceName = sourceName.trim().ifBlank { _brandSettings.value.pageName },
            publishedLabel = "কপি করা সংবাদ"
        )
        addArticleToReelBasket(article)
    }

    fun clearReelBasket() {
        _reelSlides.value = emptyList()
        _reelSlideBitmaps.value = emptyMap()
        _reelExportState.value = ReelExportState.Idle
        emitToast("রিলস বাস্কেট পরিষ্কার করা হয়েছে")
    }

    fun addCurrentCardToReelBasket() {
        val cfg = _cardConfig.value
        if (cfg.headline.isBlank()) {
            emitToast("প্রথমে একটি খবর বা শিরোনাম দিন")
            return
        }
        val slideId = UUID.randomUUID().toString()
        val slide = ReelSlide(
            id = slideId,
            articleId = _activeArticle.value?.id.orEmpty(),
            headline = cfg.headline,
            highlightWord = cfg.highlightWord,
            badgeText = cfg.badgeText,
            sourceName = cfg.sourceCredit.ifBlank { _brandSettings.value.pageName },
            imageUrl = _activeArticle.value?.imageUrl.orEmpty(),
            customBitmap = _cardPhotoBitmap.value,
            durationSec = 5
        )
        _reelSlides.value = _reelSlides.value + slide
        emitToast("ফটো কার্ড রিলস তালিকায় যোগ হয়েছে")
    }

    fun removeReelSlide(slideId: String) {
        _reelSlides.update { it.filterNot { s -> s.id == slideId } }
    }

    fun moveReelSlide(index: Int, delta: Int) {
        val list = _reelSlides.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        _reelSlides.value = list
    }

    fun updateReelSlideDuration(slideId: String, sec: Int) {
        _reelSlides.update { list ->
            list.map { if (it.id == slideId) it.copy(durationSec = sec.coerceIn(3, 10)) else it }
        }
    }

    fun exportReelMp4() {
        val slides = _reelSlides.value
        if (slides.isEmpty()) {
            emitToast("কমপক্ষে একটি খবর রিলস তালিকায় যোগ করুন")
            return
        }
        if (_reelExportState.value is ReelExportState.Exporting) return

        viewModelScope.launch {
            _reelExportState.value = ReelExportState.Exporting(1, "ভিডিও এনকোডার প্রস্তুত হচ্ছে…")
            try {
                val bitmapMap = _reelSlideBitmaps.value.toMutableMap()
                slides.forEach { slide ->
                    if (slide.customBitmap == null && bitmapMap[slide.id] == null && slide.imageUrl.isNotBlank()) {
                        bitmapMap[slide.id] = newsRepo.loadBitmapFromUrl(slide.imageUrl)
                    }
                }
                _reelSlideBitmaps.value = bitmapMap

                val mp4File = reelExporter.exportSlidesToMp4(
                    slides = slides,
                    slideBitmaps = bitmapMap,
                    brand = _brandSettings.value,
                    logo = _logoBitmap.value,
                    theme = _reelTheme.value,
                    tickerText = _reelTickerText.value,
                    onProgress = { pct, status ->
                        _reelExportState.value = ReelExportState.Exporting(pct, status)
                    }
                )
                val saved = MediaExportHelper.saveMp4ToGallery(getApplication(), mp4File)
                mp4File.delete()
                saved.onSuccess { (fileName, uri) ->
                    _reelExportState.value = ReelExportState.Success(fileName, uri.toString())
                    emitToast("রিলস ভিডিও গ্যালারিতে সেভ হয়েছে: $fileName")
                }.onFailure { err ->
                    _reelExportState.value = ReelExportState.Error(err.localizedMessage ?: "ভিডিও সেভ করা যায়নি")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Reel export failed", e)
                _reelExportState.value = ReelExportState.Error(e.localizedMessage ?: "ভিডিও তৈরি করা যায়নি")
            }
        }
    }

    fun shareExportedReel(uriString: String) {
        runCatching {
            MediaExportHelper.shareVideoUri(getApplication(), Uri.parse(uriString), _facebookCaption.value)
        }.onFailure { err ->
            emitToast("শেয়ার করা যায়নি: ${err.localizedMessage ?: "ত্রুটি"}")
        }
    }

    // ---------- Brand Settings ----------

    fun updateBrandSettings(transform: (BrandSettings) -> BrandSettings) {
        val updated = transform(_brandSettings.value)
        _brandSettings.value = updated
        brandRepo.saveBrandSettings(updated)
        schedulePreviewRefresh()
    }

    fun savePickedLogo(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = brandRepo.saveLogoFromUri(uri)
            if (savedPath != null) {
                val updated = _brandSettings.value.copy(logoFilePath = savedPath)
                _brandSettings.value = updated
                brandRepo.saveBrandSettings(updated)
                _logoBitmap.value = brandRepo.loadLogoBitmap(savedPath)
                refreshPreviewBitmap()
                emitToast("পেজের লোগো সেভ হয়েছে")
            } else {
                emitToast("লোগো সেভ করা যায়নি")
            }
        }
    }

    fun removeLogo() {
        val updated = _brandSettings.value.copy(logoFilePath = "")
        _brandSettings.value = updated
        brandRepo.saveBrandSettings(updated)
        viewModelScope.launch(Dispatchers.IO) {
            _logoBitmap.value = brandRepo.loadDefaultLogoBitmap()
            refreshPreviewBitmap()
        }
        emitToast("ডিফল্ট TF News Desk লোগো ফিরিয়ে আনা হয়েছে")
    }

    private fun schedulePreviewRefresh() {
        viewModelScope.launch(Dispatchers.Default) {
            refreshPreviewBitmap()
        }
    }

    private fun refreshPreviewBitmap() {
        val bmp = cardRenderer.renderPhotoCard(
            config = _cardConfig.value,
            brand = _brandSettings.value,
            photo = _cardPhotoBitmap.value,
            logo = _logoBitmap.value,
            speakerPhoto = _speakerPhotoBitmap.value,
            maskSettings = _maskSettings.value,
            scaleFactor = 0.65f
        )
        _renderedCardPreview.value = bmp
    }

    private fun emitToast(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }
}
