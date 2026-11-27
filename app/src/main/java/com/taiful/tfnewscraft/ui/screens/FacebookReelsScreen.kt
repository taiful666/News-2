package com.taiful.tfnewscraft.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.FacebookPostLength
import com.taiful.tfnewscraft.data.model.ReelExportState
import com.taiful.tfnewscraft.data.model.ReelTheme
import com.taiful.tfnewscraft.export.MediaExportHelper
import com.taiful.tfnewscraft.util.MonetizationMasker
import com.taiful.tfnewscraft.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private val MASK_SYMBOLS = listOf("*", "•", "-", ".", "'")

@Composable
fun FacebookReelsScreen(
    viewModel: StudioViewModel,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSection by viewModel.postReelsTabIndex.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = selectedSection == 0,
                onClick = { viewModel.setPostReelsTab(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("segment_facebook_post")
            ) {
                Text("ফেসবুক পোস্ট")
            }
            SegmentedButton(
                selected = selectedSection == 1,
                onClick = { viewModel.setPostReelsTab(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("segment_reels_studio")
            ) {
                Text("রিলস ভিডিও মেকার")
            }
        }

        if (selectedSection == 0) {
            FacebookPostPane(viewModel = viewModel)
        } else {
            ReelsStudioPane(
                viewModel = viewModel,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}

@Composable
private fun FacebookPostPane(viewModel: StudioViewModel) {
    val caption by viewModel.facebookCaption.collectAsStateWithLifecycle()
    val brand by viewModel.brandSettings.collectAsStateWithLifecycle()
    val logoBitmap by viewModel.logoBitmap.collectAsStateWithLifecycle()
    val cardPreview by viewModel.renderedCardPreview.collectAsStateWithLifecycle()
    val activeArticle by viewModel.activeArticle.collectAsStateWithLifecycle()
    val postLength by viewModel.facebookPostLength.collectAsStateWithLifecycle()
    val isPreparingFacebook by viewModel.isPreparingFacebook.collectAsStateWithLifecycle()
    val maskSettings by viewModel.maskSettings.collectAsStateWithLifecycle()
    val lastMaskReport by viewModel.lastMaskReport.collectAsStateWithLifecycle()

    var showMaskWordEditor by remember { mutableStateOf(false) }
    var customWordsText by remember(maskSettings.sensitiveWords) {
        mutableStateOf(maskSettings.sensitiveWords.joinToString(", "))
    }

    val flaggedWords = remember(caption, maskSettings.sensitiveWords) {
        MonetizationMasker.findSensitiveWords(caption, maskSettings.sensitiveWords)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1-Tap Prepare for Facebook + Post Length selector
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("১-ট্যাপে ফেসবুকের জন্য প্রস্তুত করুন", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FacebookPostLength.entries) { len ->
                            FilterChip(
                                selected = postLength == len,
                                onClick = { viewModel.setFacebookPostLength(len) },
                                label = { Text(len.labelBn, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("fb_length_${len.name.lowercase()}")
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.prepareForFacebookOneTap() },
                        enabled = activeArticle != null && !isPreparingFacebook,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prepare_fb_one_tap_button")
                    ) {
                        if (isPreparingFacebook) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPreparingFacebook) "ফেসবুক পোস্ট ও কার্ড তৈরি হচ্ছে…" else "ফেসবুকের জন্য প্রস্তুত করুন")
                    }
                }
            }
        }

        // Restored Full 95+ Word Monetization Masking Studio
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (flaggedWords.isEmpty()) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = if (flaggedWords.isEmpty()) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            Text(
                                text = if (flaggedWords.isEmpty()) {
                                    "মনিটাইজেশন সেফটি: ক্যাপশনে কোনো সংবেদনশীল শব্দ নেই (${maskSettings.sensitiveWords.size}টি শব্দ মনিটর হচ্ছে)"
                                } else {
                                    "সতর্কতা: ${flaggedWords.size}টি সংবেদনশীল শব্দ (${flaggedWords.take(5).joinToString(", ")}) — মাস্ক করুন"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (flaggedWords.isEmpty()) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }

                    if (lastMaskReport.isNotBlank()) {
                        Text(
                            text = lastMaskReport,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.maskFacebookCaptionNow() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mask_caption_now_button")
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এখনই মাস্ক করুন")
                        }
                        OutlinedButton(
                            onClick = { showMaskWordEditor = !showMaskWordEditor },
                            modifier = Modifier.testTag("toggle_mask_words_editor_button")
                        ) {
                            Text("শব্দ তালিকা")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showMaskWordEditor) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Mask Symbol Selector (* • - . ')
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "চিহ্ন:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MASK_SYMBOLS.forEach { sym ->
                            FilterChip(
                                selected = maskSettings.maskChar == sym,
                                onClick = { viewModel.updateMaskSettings { it.copy(maskChar = sym) } },
                                label = { Text(sym, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("mask_sym_$sym")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ফেসবুক পোস্টে অটো মাস্কিং", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = maskSettings.maskFacebookPost,
                            onCheckedChange = { checked ->
                                viewModel.updateMaskSettings { it.copy(maskFacebookPost = checked) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ফটো কার্ডে অটো মাস্কিং", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = maskSettings.maskPhotoCard,
                            onCheckedChange = { checked ->
                                viewModel.updateMaskSettings { it.copy(maskPhotoCard = checked) }
                            }
                        )
                    }

                    if (showMaskWordEditor) {
                        OutlinedTextField(
                            value = customWordsText,
                            onValueChange = { customWordsText = it },
                            label = { Text("সংবেদনশীল শব্দসমূহ (কমা দিয়ে আলাদা করুন)") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_mask_words_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val parsed = customWordsText
                                        .split(",", "\n", "।")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                    viewModel.updateMaskSettings { it.copy(sensitiveWords = parsed) }
                                    showMaskWordEditor = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_mask_words_button")
                            ) {
                                Text("তালিকা সংরক্ষণ")
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetMaskWordsToDefault() },
                                modifier = Modifier.testTag("reset_mask_words_button")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ডিফল্ট")
                            }
                        }
                    }
                }
            }
        }

        // Caption editor
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("পোস্ট ক্যাপশন ও হ্যাশট্যাগ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { viewModel.updateFacebookCaption(it) },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("facebook_caption_input"),
                        placeholder = { Text("এখানে আপনার ফেসবুক পোস্টের ক্যাপশন লিখুন…") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.copyFacebookCaption() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_caption_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্যাপশন কপি")
                        }
                        Button(
                            onClick = { viewModel.sharePhotoCard(caption) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_fb_post_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফেসবুকে পোস্ট")
                        }
                    }
                }
            }
        }

        // Facebook Feed Preview Card
        item {
            Text("ফেসবুক ফিড প্রিভিউ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap!!.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_page_logo),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_tf_news_desk_logo),
                                    contentDescription = stringResource(R.string.cd_page_logo),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Column {
                            Text(brand.pageName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "এখনই • পাবলিক",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    cardPreview?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_photo_card_preview),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelsStudioPane(
    viewModel: StudioViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    val slides by viewModel.reelSlides.collectAsStateWithLifecycle()
    val slideBitmaps by viewModel.reelSlideBitmaps.collectAsStateWithLifecycle()
    val brand by viewModel.brandSettings.collectAsStateWithLifecycle()
    val logoBitmap by viewModel.logoBitmap.collectAsStateWithLifecycle()
    val exportState by viewModel.reelExportState.collectAsStateWithLifecycle()
    val reelTheme by viewModel.reelTheme.collectAsStateWithLifecycle()
    val reelTickerText by viewModel.reelTickerText.collectAsStateWithLifecycle()
    val reelVoiceScript by viewModel.reelVoiceScript.collectAsStateWithLifecycle()
    val isGeneratingVoiceScript by viewModel.isGeneratingVoiceScript.collectAsStateWithLifecycle()
    val customMusicUri by viewModel.customMusicUri.collectAsStateWithLifecycle()
    val customMusicName by viewModel.customMusicName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var musicEnabled by remember { mutableStateOf(true) }
    var activeSlideIdx by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0.25f) }
    var isSpeakingTts by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    // Custom Background Audio Picker (zero broad storage permissions)
    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: "কাস্টম অডিও"
            viewModel.setCustomMusicFromUri(uri, name)
        }
    }

    // Native Android TextToSpeech for Bengali Voice-Over preview
    val ttsEngine = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val bnLocale = Locale("bn", "BD")
                val res = engine?.setLanguage(bnLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale("bn"))
                }
                ttsReady = true
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ttsEngine.stop()
                ttsEngine.shutdown()
            }
        }
    }

    // Background Music MediaPlayer (switches dynamically between bundled default & picked custom audio)
    val mediaPlayer = remember(customMusicUri) {
        runCatching {
            if (!customMusicUri.isNullOrBlank()) {
                MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customMusicUri))
                    isLooping = true
                    prepare()
                }
            } else {
                val resId = context.resources.getIdentifier("tf_news_default", "raw", context.packageName)
                if (resId != 0) MediaPlayer.create(context, resId)?.apply { isLooping = true } else null
            }
        }.getOrNull()
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            runCatching {
                if (mediaPlayer?.isPlaying == true) mediaPlayer.stop()
                mediaPlayer?.release()
            }
        }
    }

    LaunchedEffect(isPlaying, musicEnabled, mediaPlayer) {
        runCatching {
            if (isPlaying && musicEnabled) {
                if (mediaPlayer?.isPlaying == false) mediaPlayer.start()
            } else {
                if (mediaPlayer?.isPlaying == true) mediaPlayer.pause()
            }
        }
    }

    LaunchedEffect(isPlaying, slides) {
        if (!isPlaying || slides.isEmpty()) return@LaunchedEffect
        while (isPlaying && slides.isNotEmpty()) {
            val idx = activeSlideIdx.coerceIn(0, slides.lastIndex)
            val durationMs = slides[idx].durationSec * 1000L
            val stepMs = 50L
            val steps = (durationMs / stepMs).toInt().coerceAtLeast(1)
            for (s in 0..steps) {
                if (!isPlaying) break
                slideProgress = s.toFloat() / steps.toFloat()
                delay(stepMs)
            }
            if (isPlaying) {
                activeSlideIdx = (idx + 1) % slides.size
                slideProgress = 0f
            }
        }
    }

    val previewFrame: Bitmap? = remember(
        slides, activeSlideIdx, slideProgress, slideBitmaps, brand, logoBitmap, reelTheme, reelTickerText
    ) {
        if (slides.isEmpty()) {
            null
        } else {
            val idx = activeSlideIdx.coerceIn(0, slides.lastIndex)
            val slide = slides[idx]
            val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bmp)
            viewModel.cardRenderer.renderReelFrame(
                canvas = canvas,
                width = 360,
                height = 640,
                slide = slide,
                brand = brand,
                photo = slide.customBitmap ?: slideBitmaps[slide.id],
                logo = logoBitmap,
                slideProgress = slideProgress,
                slideIndex = idx,
                totalSlides = slides.size,
                theme = reelTheme,
                tickerText = reelTickerText
            )
            bmp
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (slides.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("রিলস তালিকায় এখনো কোনো খবর নেই", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "হোম ট্যাবের মাল্টি নিউজ রিলস বা লাইভ নিউজ থেকে (+) বাটনে চাপ দিন অথবা বর্তমান ফটো কার্ডটিকে রিলসে যুক্ত করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.addCurrentCardToReelBasket() },
                            modifier = Modifier.testTag("add_current_card_reel_button")
                        ) {
                            Text("বর্তমান কার্ড রিলসে যোগ করুন")
                        }
                    }
                }
            }
        } else {
            // 9:16 Live Animated Reel Preview
            item {
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            previewFrame?.let { frame ->
                                Image(
                                    bitmap = frame.asImageBitmap(),
                                    contentDescription = "রিলস প্রিভিউ",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier.testTag("reel_play_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "থামান" else "প্রিভিউ চালান")
                            }

                            OutlinedButton(
                                onClick = { musicEnabled = !musicEnabled },
                                modifier = Modifier.testTag("reel_music_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (musicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (musicEnabled) "মিউজিক চালু" else "মিউট")
                            }
                        }
                    }
                }
            }

            // Restored Reel Broadcast Themes, Ticker & Custom Background Music Card
            item {
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("রিলস থিম, টিকার ও মিউজিক", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            TextButton(
                                onClick = { viewModel.shuffleReelTheme() },
                                modifier = Modifier.testTag("reel_shuffle_theme_button")
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("শাফল")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ReelTheme.entries) { theme ->
                                FilterChip(
                                    selected = reelTheme == theme,
                                    onClick = { viewModel.setReelTheme(theme) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(theme.accentArgb))
                                        )
                                    },
                                    label = { Text(theme.labelBn, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("reel_theme_${theme.name.lowercase()}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reelTickerText,
                            onValueChange = { viewModel.updateReelTickerText(it) },
                            label = { Text("স্ক্রলিং টিকার টেক্সট (ঐচ্ছিক)") },
                            placeholder = { Text("খালি রাখলে সব শিরোনাম অটো স্ক্রল হবে") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reel_ticker_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { musicPickerLauncher.launch("audio/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reel_pick_custom_music_button")
                            ) {
                                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(customMusicName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (customMusicUri != null) {
                                IconButton(
                                    onClick = { viewModel.resetToDefaultMusic() },
                                    modifier = Modifier.testTag("reel_reset_music_button")
                                ) {
                                    Icon(Icons.Filled.RestartAlt, contentDescription = "ডিফল্ট মিউজিক")
                                }
                            }
                        }
                    }
                }
            }

            // Restored AI Voice-Over Script & Bengali TTS Preview Card
            item {
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("AI ভয়েস-ওভার স্ক্রিপ্ট", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.generateReelVoiceScriptAi() },
                                enabled = !isGeneratingVoiceScript,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("generate_reel_voice_script_button")
                            ) {
                                if (isGeneratingVoiceScript) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI স্ক্রিপ্ট", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        OutlinedTextField(
                            value = reelVoiceScript,
                            onValueChange = { viewModel.updateReelVoiceScript(it) },
                            label = { Text("সংবাদ পাঠের স্ক্রিপ্ট (পত্রিকা/চ্যানেলের নাম ছাড়া)") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reel_voice_script_input")
                        )

                        if (reelVoiceScript.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        if (isSpeakingTts) {
                                            ttsEngine.stop()
                                            isSpeakingTts = false
                                        } else if (ttsReady) {
                                            isSpeakingTts = true
                                            ttsEngine.speak(reelVoiceScript, TextToSpeech.QUEUE_FLUSH, null, "reel_voice")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reel_tts_preview_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakingTts) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSpeakingTts) "ভয়েস থামান" else "ভয়েস শুনুন")
                                }
                                OutlinedButton(
                                    onClick = {
                                        MediaExportHelper.copyToClipboard(context, "Reel Voice Script", reelVoiceScript)
                                    },
                                    modifier = Modifier.testTag("reel_copy_script_button")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("কপি")
                                }
                            }
                        }
                    }
                }
            }

            // Export Progress & Action Card
            item {
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val totalSec = slides.sumOf { it.durationSec }
                        Text(
                            text = "মোট ${slides.size}টি স্লাইড • আনুমানিক ${totalSec} সেকেন্ড (720×1280 HD MP4)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        when (val st = exportState) {
                            is ReelExportState.Exporting -> {
                                LinearProgressIndicator(
                                    progress = { st.percent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(st.statusText, style = MaterialTheme.typography.bodySmall)
                            }
                            is ReelExportState.Success -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "রিলস গ্যালারিতে সেভ হয়েছে: ${st.fileName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.shareExportedReel(st.uriString) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ফেসবুক রিলসে শেয়ার করুন")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            is ReelExportState.Error -> {
                                Text(
                                    text = st.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            is ReelExportState.Idle -> Unit
                        }

                        Button(
                            onClick = {
                                isPlaying = false
                                onRequestNotificationPermission()
                                viewModel.exportReelMp4()
                            },
                            enabled = exportState !is ReelExportState.Exporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_reel_mp4_button")
                        ) {
                            Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_export_reel))
                        }
                    }
                }
            }

            // Slide Queue List with Up/Down Reorder
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("রিলস স্লাইড তালিকা", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = { viewModel.clearReelBasket() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reels_clear_basket_button")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সব মুছুন", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            itemsIndexed(slides, key = { _, s -> s.id }) { index, slide ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = slide.headline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${slide.sourceName} • ${slide.durationSec} সেকেন্ড",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.removeReelSlide(slide.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "স্লাইড মুছুন")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.moveReelSlide(index, -1) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "উপরে নিন", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveReelSlide(index, 1) },
                                    enabled = index < slides.lastIndex
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "নিচে নিন", modifier = Modifier.size(18.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.updateReelSlideDuration(slide.id, slide.durationSec - 1) }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "সময় কমান", modifier = Modifier.size(18.dp))
                                }
                                Text("${slide.durationSec}s", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = { viewModel.updateReelSlideDuration(slide.id, slide.durationSec + 1) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "সময় বাড়ান", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
