package com.taiful.tfnewscraft.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.HomeMode
import com.taiful.tfnewscraft.data.model.LiveFeedMetadata
import com.taiful.tfnewscraft.data.model.LiveNewsTab
import com.taiful.tfnewscraft.data.model.NewspaperSource
import com.taiful.tfnewscraft.ui.components.CompactLiveCategoryTabs
import com.taiful.tfnewscraft.ui.components.CompactLiveNewsRow
import com.taiful.tfnewscraft.ui.components.CompactLiveToolbarRow
import com.taiful.tfnewscraft.ui.components.CompactSearchLinkBar
import com.taiful.tfnewscraft.ui.components.EditorialNewsCard
import com.taiful.tfnewscraft.ui.components.GitHubLiveStatusStrip
import com.taiful.tfnewscraft.ui.components.HomeLauncherCard
import com.taiful.tfnewscraft.ui.components.MultiReelsBasketBanner
import com.taiful.tfnewscraft.ui.components.NewspaperDirectoryCard
import com.taiful.tfnewscraft.ui.components.QuickLinkAndSearchCard
import com.taiful.tfnewscraft.ui.components.SectionBackHeader
import com.taiful.tfnewscraft.ui.components.isLikelyNewsUrl
import com.taiful.tfnewscraft.viewmodel.StudioViewModel

@Composable
fun NewsScreen(
    viewModel: StudioViewModel,
    onOpenAiStudio: () -> Unit,
    onOpenPhotoCard: () -> Unit,
    onOpenReels: () -> Unit,
    onRequestCleanWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSection by viewModel.activeHomeSection.collectAsStateWithLifecycle()

    if (activeSection == null) {
        CleanHomeHub(
            onSelectMode = { viewModel.openHomeSection(it) },
            modifier = modifier
        )
    } else {
        BackHandler {
            viewModel.returnToHomeHub()
        }
        HomeSectionWorkspace(
            section = activeSection!!,
            viewModel = viewModel,
            onBackToHome = { viewModel.returnToHomeHub() },
            onOpenAiStudio = onOpenAiStudio,
            onOpenPhotoCard = onOpenPhotoCard,
            onOpenReels = onOpenReels,
            onRequestCleanWorkspace = onRequestCleanWorkspace,
            modifier = modifier
        )
    }
}

/**
 * Ultra-clean Home Landing Hub: ONLY 3 clean buttons with zero subtitles or clutter.
 */
@Composable
private fun CleanHomeHub(
    onSelectMode: (HomeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            HomeLauncherCard(
                title = "লাইভ নিউজ",
                icon = Icons.Filled.RssFeed,
                accentStart = Color(0xFFD71920),
                accentEnd = Color(0xFF991B1B),
                testTag = "home_card_live_news",
                onClick = { onSelectMode(HomeMode.LIVE_NEWS) }
            )

            HomeLauncherCard(
                title = "মাল্টি নিউজ রিলস",
                icon = Icons.Filled.MovieCreation,
                accentStart = Color(0xFF1D4ED8),
                accentEnd = Color(0xFF1E3A8A),
                testTag = "home_card_multi_reels",
                onClick = { onSelectMode(HomeMode.MULTI_REELS) }
            )

            HomeLauncherCard(
                title = "পত্রিকা থেকে নিউজ বাছাই",
                icon = Icons.Filled.Public,
                accentStart = Color(0xFF0F766E),
                accentEnd = Color(0xFF115E59),
                testTag = "home_card_paper_and_video",
                onClick = { onSelectMode(HomeMode.PAPER_AND_VIDEO) }
            )
        }
    }
}

@Composable
private fun HomeSectionWorkspace(
    section: HomeMode,
    viewModel: StudioViewModel,
    onBackToHome: () -> Unit,
    onOpenAiStudio: () -> Unit,
    onOpenPhotoCard: () -> Unit,
    onOpenReels: () -> Unit,
    onRequestCleanWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPaper by viewModel.selectedNewspaper.collectAsStateWithLifecycle()
    val liveArticles by viewModel.liveArticles.collectAsStateWithLifecycle()
    val liveNewsTab by viewModel.liveNewsTab.collectAsStateWithLifecycle()
    val curatedLiveArticles by viewModel.curatedLiveArticles.collectAsStateWithLifecycle()
    val liveTabCounts by viewModel.liveTabCounts.collectAsStateWithLifecycle()
    val liveFeedMeta by viewModel.liveFeedMeta.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoadingFeed by viewModel.isLoadingFeed.collectAsStateWithLifecycle()
    val feedError by viewModel.feedError.collectAsStateWithLifecycle()
    val isFetchingUrl by viewModel.isFetchingUrl.collectAsStateWithLifecycle()
    val activeArticle by viewModel.activeArticle.collectAsStateWithLifecycle()
    val reelSlides by viewModel.reelSlides.collectAsStateWithLifecycle()
    val customNewspapers by viewModel.customNewspapers.collectAsStateWithLifecycle()
    val adFilterEnabled by viewModel.adFilterEnabled.collectAsStateWithLifecycle()
    val extractedVideoFrames by viewModel.extractedVideoFrames.collectAsStateWithLifecycle()
    val extractedVideoTitle by viewModel.extractedVideoTitle.collectAsStateWithLifecycle()
    val isExtractingVideo by viewModel.isExtractingVideo.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var urlOrSearchInput by remember(section) { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var showAddPaperDialog by remember { mutableStateOf(false) }
    var selectedPaperCategory by remember { mutableStateOf("সব") }
    // Inside Multi-News Reels: 0 = লাইভ নিউজ, 1 = পত্রিকা, 2 = লিংক / কপি-পেস্ট
    var multiSourceTab by remember { mutableIntStateOf(0) }
    var copyPasteTitle by remember { mutableStateOf("") }
    var copyPasteBody by remember { mutableStateOf("") }
    var copyPasteSource by remember { mutableStateOf("") }
    var selectedVideoFrameIdx by remember { mutableIntStateOf(0) }
    var videoHeadlineInput by remember(extractedVideoTitle) { mutableStateOf(extractedVideoTitle) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoFrameIdx = 0
            viewModel.extractFramesFromLocalVideo(uri)
        }
    }

    val hasWorkspaceContent = activeArticle != null || reelSlides.isNotEmpty() || searchQuery.isNotBlank()

    val reelIndexByArticleKey = remember(reelSlides) {
        buildMap {
            reelSlides.forEachIndexed { idx, slide ->
                if (slide.articleId.isNotBlank()) put(slide.articleId, idx + 1)
                put(slide.headline, idx + 1)
            }
        }
    }

    val allBrowseablePapers = remember(customNewspapers) {
        viewModel.newspapers.filter { it.homeUrl.isNotBlank() } + customNewspapers
    }
    val paperCategories = remember(customNewspapers) {
        buildList {
            addAll(listOf("সব", "পত্রিকা", "টিভি", "অনলাইন", "আন্তর্জাতিক", "ইংরেজি"))
            if (customNewspapers.isNotEmpty()) add("কাস্টম")
        }
    }
    val filteredDirectoryPapers = remember(selectedPaperCategory, allBrowseablePapers) {
        if (selectedPaperCategory == "সব") {
            allBrowseablePapers
        } else {
            allBrowseablePapers.filter { it.category == selectedPaperCategory }
        }
    }

    fun submitInput() {
        val text = urlOrSearchInput.trim()
        if (text.isBlank()) return
        if (isLikelyNewsUrl(text)) {
            val addOnlyToReels = section == HomeMode.MULTI_REELS
            viewModel.importFromUrl(
                url = text,
                addToReelsOnly = addOnlyToReels,
                onImported = {
                    urlOrSearchInput = ""
                    if (!addOnlyToReels) onOpenPhotoCard()
                }
            )
        } else {
            viewModel.searchNews(text)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (section == HomeMode.LIVE_NEWS) 12.dp else 16.dp,
            vertical = if (section == HomeMode.LIVE_NEWS) 8.dp else 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (section == HomeMode.LIVE_NEWS) 7.dp else 12.dp)
    ) {
        // Section Back Header only for non-Live sections (Live News stays ultra-minimal since global Back/Forward/Home bar is above)
        if (section != HomeMode.LIVE_NEWS) {
            item {
                SectionBackHeader(
                    title = section.titleBn,
                    subtitle = when (section) {
                        HomeMode.MULTI_REELS -> "লাইভ, পত্রিকা বা কপি-পেস্ট থেকে একাধিক খবর"
                        HomeMode.PAPER_AND_VIDEO -> "পত্রিকায় ঢুকে খবর নিন বা লিংক পেস্ট করুন"
                        HomeMode.LIVE_NEWS -> ""
                    },
                    onBack = onBackToHome
                )
            }
        }

        when (section) {
            HomeMode.LIVE_NEWS -> {
                // 1 compact row with just 2 small pill buttons
                item {
                    CompactLiveToolbarRow(
                        meta = liveFeedMeta.copy(totalArticles = liveTabCounts.first + liveTabCounts.second),
                        isLoading = isLoadingFeed,
                        onRefresh = { viewModel.refreshLiveFeed(forceRefresh = true) },
                        onOpenManualDialog = { showManualDialog = true }
                    )
                }

                // 2-Tab switcher: বাংলাদেশ ও ভাইরাল | আন্তর্জাতিক (Iran/Israel/Palestine priority)
                item {
                    CompactLiveCategoryTabs(
                        selectedTab = liveNewsTab,
                        bdCount = liveTabCounts.first,
                        intlCount = liveTabCounts.second,
                        onSelectTab = { viewModel.selectLiveNewsTab(it) }
                    )
                }

                // Slim single-line search / link bar
                item {
                    CompactSearchLinkBar(
                        input = urlOrSearchInput,
                        isFetchingUrl = isFetchingUrl,
                        onInputChange = { urlOrSearchInput = it },
                        onPasteFromClipboard = {
                            clipboardManager.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let {
                                urlOrSearchInput = it
                            }
                        },
                        onClearInput = {
                            urlOrSearchInput = ""
                            if (searchQuery.isNotBlank()) viewModel.clearSearch()
                        },
                        onSubmit = { submitInput() }
                    )
                }

                item {
                    NewspaperFilterChipRow(
                        newspapers = viewModel.newspapers,
                        selectedId = selectedPaper.id,
                        searchQuery = searchQuery,
                        onSelectPaper = { viewModel.selectNewspaper(it) },
                        onClearSearch = {
                            urlOrSearchInput = ""
                            viewModel.clearSearch()
                        }
                    )
                }

                if (isLoadingFeed && curatedLiveArticles.isEmpty()) {
                    item { FeedLoadingBox(selectedPaper.nameBn) }
                } else if (feedError != null && curatedLiveArticles.isEmpty()) {
                    item {
                        FeedErrorCard(
                            message = feedError.orEmpty(),
                            onRetry = { viewModel.refreshLiveFeed(forceRefresh = true) },
                            onOpenDirectPaper = if (selectedPaper.homeUrl.isNotBlank()) {
                                { viewModel.openNewspaperBrowser(selectedPaper) }
                            } else null
                        )
                    }
                } else if (curatedLiveArticles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (liveNewsTab == LiveNewsTab.INTERNATIONAL) {
                                        "${selectedPaper.nameBn}-এ এই মুহূর্তে কোনো গুরুত্বপূর্ণ আন্তর্জাতিক খবর নেই।"
                                    } else {
                                        "${selectedPaper.nameBn}-এ এই মুহূর্তে কোনো রাজনৈতিক বা ভাইরাল খবর নেই।"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (selectedPaper.id != "all") {
                                        FilledTonalButton(
                                            onClick = { viewModel.selectNewspaper(viewModel.newspapers.first()) },
                                            modifier = Modifier.testTag("live_empty_show_all_papers")
                                        ) {
                                            Text("সব পত্রিকা দেখুন")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.refreshLiveFeed(forceRefresh = true) },
                                        modifier = Modifier.testTag("live_empty_refresh")
                                    ) {
                                        Text("রিফ্রেশ")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(curatedLiveArticles, key = { "${liveNewsTab.name}_${it.article.id}" }) { curated ->
                        val article = curated.article
                        val reelIdx = reelIndexByArticleKey[article.id] ?: reelIndexByArticleKey[article.title]
                        CompactLiveNewsRow(
                            article = article,
                            isSelected = activeArticle?.id == article.id,
                            reelSelectionNumber = reelIdx,
                            categoryBadge = curated.badgeBn,
                            isHotBadge = curated.isHotMiddleEast || curated.badgeBn == "রাজনীতি" || curated.badgeBn == "ভাইরাল" || curated.badgeBn == "ভাইরাল বিনোদন",
                            onSelect = { viewModel.selectArticle(article) },
                            onMakeCard = {
                                viewModel.selectArticle(article)
                                onOpenPhotoCard()
                            },
                            onOpenAi = {
                                viewModel.selectArticle(article)
                                onOpenAiStudio()
                            },
                            onReadFull = {
                                viewModel.selectArticle(article)
                                viewModel.openArticleReader()
                            },
                            onToggleReel = {
                                viewModel.toggleArticleInReelBasket(article)
                            }
                        )
                    }
                }
            }

            HomeMode.MULTI_REELS -> {
                item {
                    MultiReelsBasketBanner(
                        reelCount = reelSlides.size,
                        onCreateReels = onOpenReels,
                        onClearBasket = { viewModel.clearReelBasket() }
                    )
                }

                // 3-Source selector right inside Multi-News Reels
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = multiSourceTab == 0,
                            onClick = { multiSourceTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            modifier = Modifier.testTag("multi_source_live")
                        ) {
                            Text("লাইভ নিউজ", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = multiSourceTab == 1,
                            onClick = { multiSourceTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            modifier = Modifier.testTag("multi_source_papers")
                        ) {
                            Text("পত্রিকা", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = multiSourceTab == 2,
                            onClick = { multiSourceTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            modifier = Modifier.testTag("multi_source_copy_paste")
                        ) {
                            Text("লিংক/পেস্ট", maxLines = 1)
                        }
                    }
                }

                when (multiSourceTab) {
                    0 -> {
                        item {
                            NewspaperFilterChipRow(
                                newspapers = viewModel.newspapers,
                                selectedId = selectedPaper.id,
                                searchQuery = searchQuery,
                                onSelectPaper = { viewModel.selectNewspaper(it) },
                                onClearSearch = {
                                    urlOrSearchInput = ""
                                    viewModel.clearSearch()
                                }
                            )
                        }
                        if (isLoadingFeed && liveArticles.isEmpty()) {
                            item { FeedLoadingBox(selectedPaper.nameBn) }
                        } else if (feedError != null && liveArticles.isEmpty()) {
                            item {
                                FeedErrorCard(
                                    message = feedError.orEmpty(),
                                    onRetry = { viewModel.refreshLiveFeed(forceRefresh = true) },
                                    onOpenDirectPaper = if (selectedPaper.homeUrl.isNotBlank()) {
                                        { viewModel.openNewspaperBrowser(selectedPaper) }
                                    } else null
                                )
                            }
                        } else {
                            items(liveArticles, key = { "multi_${it.id}" }) { article ->
                                val reelIdx = reelIndexByArticleKey[article.id] ?: reelIndexByArticleKey[article.title]
                                EditorialNewsCard(
                                    article = article,
                                    isActiveCard = false,
                                    reelSelectionNumber = reelIdx,
                                    multiSelectMode = true,
                                    onMakeCard = {
                                        viewModel.selectArticle(article)
                                        onOpenPhotoCard()
                                    },
                                    onOpenAi = {
                                        viewModel.selectArticle(article)
                                        onOpenAiStudio()
                                    },
                                    onToggleReel = {
                                        viewModel.toggleArticleInReelBasket(article)
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        item {
                            Text(
                                text = "যেকোনো পত্রিকায় ঢুকে খবরের পেজে গিয়ে নিচের 'রিলসে যোগ' বাটনে চাপ দিন:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val paperRows = allBrowseablePapers.chunked(2)
                        items(paperRows, key = { row -> "multi_paper_" + row.joinToString("_") { it.id } }) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NewspaperDirectoryCard(
                                    paper = pair[0],
                                    onClick = { viewModel.openNewspaperBrowser(pair[0]) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (pair.size > 1) {
                                    NewspaperDirectoryCard(
                                        paper = pair[1],
                                        onClick = { viewModel.openNewspaperBrowser(pair[1]) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    else -> {
                        // Link paste + direct Copy-Paste text into Reels basket
                        item {
                            QuickLinkAndSearchCard(
                                input = urlOrSearchInput,
                                homeMode = HomeMode.MULTI_REELS,
                                isFetchingUrl = isFetchingUrl,
                                onInputChange = { urlOrSearchInput = it },
                                onPasteFromClipboard = {
                                    clipboardManager.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let {
                                        urlOrSearchInput = it
                                    }
                                },
                                onClearInput = { urlOrSearchInput = "" },
                                onSubmitPrimary = { submitInput() },
                                onOpenManualDialog = { showManualDialog = true }
                            )
                        }

                        item {
                            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "কপি করা খবর রিলসে যোগ করুন",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                                if (clip.isNotBlank()) {
                                                    if (copyPasteTitle.isBlank()) {
                                                        val lines = clip.lines().filter { it.isNotBlank() }
                                                        copyPasteTitle = lines.firstOrNull().orEmpty().take(120)
                                                        copyPasteBody = lines.drop(1).joinToString("\n").ifBlank { clip }
                                                    } else {
                                                        copyPasteBody = clip
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("reel_paste_clipboard_text_button")
                                        ) {
                                            Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("পেস্ট")
                                        }
                                    }

                                    OutlinedTextField(
                                        value = copyPasteTitle,
                                        onValueChange = { copyPasteTitle = it },
                                        label = { Text("খবরের শিরোনাম") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reel_manual_title_input")
                                    )
                                    OutlinedTextField(
                                        value = copyPasteBody,
                                        onValueChange = { copyPasteBody = it },
                                        label = { Text("বিস্তারিত লেখা (ঐচ্ছিক)") },
                                        minLines = 3,
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = copyPasteSource,
                                        onValueChange = { copyPasteSource = it },
                                        label = { Text("সূত্র / পত্রিকার নাম (ঐচ্ছিক)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.addManualArticleToReelBasket(
                                                title = copyPasteTitle,
                                                body = copyPasteBody,
                                                sourceName = copyPasteSource
                                            )
                                            copyPasteTitle = ""
                                            copyPasteBody = ""
                                            copyPasteSource = ""
                                        },
                                        enabled = copyPasteTitle.isNotBlank() || copyPasteBody.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reel_add_pasted_story_button")
                                    ) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("রিলস তালিকায় যোগ করুন")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HomeMode.PAPER_AND_VIDEO -> {
                item {
                    QuickLinkAndSearchCard(
                        input = urlOrSearchInput,
                        homeMode = HomeMode.PAPER_AND_VIDEO,
                        isFetchingUrl = isFetchingUrl,
                        onInputChange = { urlOrSearchInput = it },
                        onPasteFromClipboard = {
                            clipboardManager.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let {
                                urlOrSearchInput = it
                            }
                        },
                        onClearInput = { urlOrSearchInput = "" },
                        onSubmitPrimary = { submitInput() },
                        onOpenManualDialog = { showManualDialog = true }
                    )
                }

                // Restored Local Video-to-News Keyframe Extractor Card
                item {
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ফোন থেকে ভিডিও দিয়ে নিউজ তৈরি",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "যেকোনো ভিডিও বেছে নিন — অটোমেটিক ৫টি HD স্ক্রিনশট বের হবে",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                enabled = !isExtractingVideo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_local_video_button")
                            ) {
                                if (isExtractingVideo) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ভিডিও থেকে স্ক্রিনশট নেওয়া হচ্ছে…")
                                } else {
                                    Icon(Icons.Filled.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("গ্যালারি থেকে ভিডিও বাছাই করুন")
                                }
                            }

                            if (extractedVideoFrames.isNotEmpty()) {
                                val safeIdx = selectedVideoFrameIdx.coerceIn(0, extractedVideoFrames.lastIndex)
                                val chosenFrame = extractedVideoFrames[safeIdx]

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(extractedVideoFrames) { idx, frame ->
                                        val isPicked = idx == safeIdx
                                        Box(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .aspectRatio(16f / 10f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = if (isPicked) 3.dp else 1.dp,
                                                    color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedVideoFrameIdx = idx }
                                                .testTag("video_frame_thumb_$idx")
                                        ) {
                                            Image(
                                                bitmap = frame.bitmap.asImageBitmap(),
                                                contentDescription = "ভিডিও ফ্রেম ${frame.labelBn}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = frame.labelBn,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = videoHeadlineInput,
                                    onValueChange = { videoHeadlineInput = it },
                                    label = { Text("ভিডিও সংবাদের শিরোনাম") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("video_news_headline_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.useVideoFrameForCard(chosenFrame, videoHeadlineInput) {
                                                onOpenPhotoCard()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("video_frame_to_card_button")
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ফটো কার্ড")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.addVideoFrameToReelBasket(chosenFrame, videoHeadlineInput)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("video_frame_to_reel_button")
                                    ) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ রিলস")
                                    }
                                }
                            }
                        }
                    }
                }

                // Ad-Filter Toggle + Custom Newspaper Action Row
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "পত্রিকা ব্রাউজারে বিজ্ঞাপন ফিল্টার",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "পপ-আপ ও বিরক্তিকর বিজ্ঞাপন ব্লক করে দ্রুত খবর পড়ুন",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = adFilterEnabled,
                                    onCheckedChange = { viewModel.toggleAdFilter(it) },
                                    modifier = Modifier.testTag("ad_filter_switch")
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "বাংলাদেশের পত্রিকা ও টিভি (${filteredDirectoryPapers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { showAddPaperDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_custom_newspaper_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পত্রিকা যোগ", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paperCategories) { cat ->
                            FilterChip(
                                selected = selectedPaperCategory == cat,
                                onClick = { selectedPaperCategory = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("directory_cat_$cat")
                            )
                        }
                    }
                }

                val paperRows = filteredDirectoryPapers.chunked(2)
                items(paperRows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NewspaperDirectoryCard(
                            paper = pair[0],
                            onClick = { viewModel.openNewspaperBrowser(pair[0]) },
                            modifier = Modifier.weight(1f)
                        )
                        if (pair.size > 1) {
                            NewspaperDirectoryCard(
                                paper = pair[1],
                                onClick = { viewModel.openNewspaperBrowser(pair[1]) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualNewsDialog(
            isReelMode = section == HomeMode.MULTI_REELS,
            onDismiss = { showManualDialog = false },
            onSubmit = { title, body, source ->
                if (section == HomeMode.MULTI_REELS) {
                    viewModel.addManualArticleToReelBasket(title, body, source)
                    showManualDialog = false
                } else {
                    viewModel.importManualArticle(title, body, source)
                    showManualDialog = false
                    onOpenPhotoCard()
                }
            }
        )
    }

    if (showAddPaperDialog) {
        AddCustomNewspaperDialog(
            onDismiss = { showAddPaperDialog = false },
            onAdd = { name, url ->
                viewModel.addCustomNewspaper(name, url)
                showAddPaperDialog = false
            }
        )
    }
}

@Composable
private fun ActiveArticleBanner(
    title: String,
    sourceName: String,
    onClear: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenPhotoCard: () -> Unit,
    onOpenAiStudio: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "নির্বাচিত খবর • $sourceName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("clear_active_article_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "নির্বাচিত খবর সরান",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenPhotoCard,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("active_to_card_button")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কার্ড")
                }
                FilledTonalButton(
                    onClick = onOpenAiStudio,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("active_to_ai_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI লেখা")
                }
                OutlinedButton(
                    onClick = onOpenReader,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("active_to_reader_button")
                ) {
                    Icon(Icons.Filled.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মূল খবর")
                }
            }
        }
    }
}

@Composable
private fun NewspaperFilterChipRow(
    newspapers: List<NewspaperSource>,
    selectedId: String,
    searchQuery: String,
    onSelectPaper: (NewspaperSource) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "খোঁজার ফলাফল: \"$searchQuery\"",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onClearSearch) {
                    Text("ফিল্টার মুছুন")
                }
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(newspapers, key = { it.id }) { paper ->
                FilterChip(
                    selected = paper.id == selectedId && searchQuery.isBlank(),
                    onClick = { onSelectPaper(paper) },
                    label = { Text(paper.nameBn) },
                    modifier = Modifier.testTag("paper_chip_${paper.id}")
                )
            }
        }
    }
}

@Composable
private fun FeedLoadingBox(paperName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$paperName থেকে সর্বশেষ খবর আনা হচ্ছে…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeedErrorCard(
    message: String,
    onRetry: () -> Unit,
    onOpenDirectPaper: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) {
                    Text("আবার চেষ্টা করুন")
                }
                if (onOpenDirectPaper != null) {
                    FilledTonalButton(onClick = onOpenDirectPaper) {
                        Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পত্রিকায় ঢুকুন")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualNewsDialog(
    isReelMode: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, body: String, source: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isReelMode) "রিলসের জন্য খবর পেস্ট করুন" else stringResource(R.string.action_paste_news)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("সংবাদের শিরোনাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("বিস্তারিত সংবাদ") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("সূত্র (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, body, source) },
                modifier = Modifier.testTag("manual_news_submit_button")
            ) {
                Text(if (isReelMode) "রিলসে যোগ করুন" else "যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun AddCustomNewspaperDialog(
    onDismiss: () -> Unit,
    onAdd: (nameBn: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন পত্রিকা যোগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পত্রিকার নাম (যেমন: আমার সংবাদ)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_paper_name_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("ওয়েবসাইট লিংক (যেমন: https://example.com)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_paper_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank(),
                modifier = Modifier.testTag("custom_paper_submit_button")
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
