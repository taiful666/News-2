package com.taiful.tfnewscraft.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.AiOutputLanguage
import com.taiful.tfnewscraft.data.model.AiStudioMode
import com.taiful.tfnewscraft.data.model.AiTextUiState
import com.taiful.tfnewscraft.data.model.CardHookKind
import com.taiful.tfnewscraft.data.model.CardOptionsUiState
import com.taiful.tfnewscraft.data.model.ImageGenUiState
import com.taiful.tfnewscraft.export.MediaExportHelper
import com.taiful.tfnewscraft.viewmodel.StudioViewModel

private val IMAGE_RATIOS = listOf("1:1" to "১:১ পোস্ট", "4:5" to "৪:৫ ফিড", "16:9" to "১৬:৯ ইউটিউব")

@Composable
fun AiStudioScreen(
    viewModel: StudioViewModel,
    onGoToNews: () -> Unit,
    onGoToPhotoCard: () -> Unit,
    onGoToFacebook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeArticle by viewModel.activeArticle.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedAiMode.collectAsStateWithLifecycle()
    val selectedSubOption by viewModel.selectedAiSubOption.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedAiLanguage.collectAsStateWithLifecycle()
    val aiTextState by viewModel.aiTextState.collectAsStateWithLifecycle()
    val selectedHookKind by viewModel.selectedHookKind.collectAsStateWithLifecycle()
    val cardOptionsState by viewModel.cardOptionsState.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val isSuggestingPrompt by viewModel.isSuggestingPrompt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var extraInstruction by remember { mutableStateOf("") }
    var imagePrompt by remember { mutableStateOf("") }
    var selectedImageRatio by remember { mutableStateOf("1:1") }

    LaunchedEffect(activeArticle?.id) {
        activeArticle?.title?.let { title ->
            if (imagePrompt.isBlank()) imagePrompt = title
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active article context card with Original Article Reader trigger
        item {
            if (activeArticle == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("এখনো কোনো খবর নির্বাচন করা হয়নি", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "হোম ট্যাব থেকে যেকোনো একটি সংবাদ বেছে নিন, তারপর এক চাপে ফেসবুক পোস্ট, ফটো কার্ড হেডলাইন ও AI ছবি তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onGoToNews, modifier = Modifier.testTag("ai_pick_news_button")) {
                            Text("খবর বেছে নিন")
                        }
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বর্তমান সংবাদ • ${activeArticle!!.sourceName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { viewModel.openArticleReader() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("ai_open_reader_button")
                            ) {
                                Icon(Icons.Filled.Article, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("মূল খবর পড়ুন", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeArticle!!.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Section 1: Full 8-Mode AI News Studio (Gemini Streaming + Sub-options + Language + Undo)
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI নিউজ স্টুডিও (৮টি মোড)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                selectedMode.subtitleBn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (viewModel.canUndoAiText) {
                            OutlinedButton(
                                onClick = { viewModel.undoAiText() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("ai_undo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "পূর্বের লেখা",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("আগেরটি", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // All 8 AI Modes
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AiStudioMode.entries) { mode ->
                            FilterChip(
                                selected = mode == selectedMode,
                                onClick = { viewModel.setAiMode(mode) },
                                label = { Text(mode.labelBn) },
                                modifier = Modifier.testTag("ai_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Dynamic Sub-Options for the selected mode
                    if (selectedMode.subOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(selectedMode.subOptions, key = { it.id }) { sub ->
                                FilterChip(
                                    selected = selectedSubOption == sub.id,
                                    onClick = { viewModel.setAiSubOption(sub.id) },
                                    label = { Text(sub.labelBn, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("ai_sub_${sub.id}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Output Language selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ভাষা:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AiOutputLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { viewModel.setAiOutputLanguage(lang) },
                                label = { Text(lang.labelBn, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("ai_lang_${lang.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = extraInstruction,
                        onValueChange = { extraInstruction = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_extra_instruction_input"),
                        label = {
                            Text(
                                if (selectedMode == AiStudioMode.CUSTOM) {
                                    "আপনার নিজের নির্দেশনা লিখুন (যেমন: ১ মিনিটের ভিডিও স্ক্রিপ্ট)"
                                } else {
                                    "অতিরিক্ত নির্দেশনা (ঐচ্ছিক, যেমন: আরও ছোট বা নাটকীয় করুন)"
                                }
                            )
                        },
                        singleLine = selectedMode != AiStudioMode.CUSTOM,
                        minLines = if (selectedMode == AiStudioMode.CUSTOM) 2 else 1,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateAiPost(extraInstruction) },
                        enabled = activeArticle != null && aiTextState !is AiTextUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_ai_post_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${selectedMode.labelBn} তৈরি করুন")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = aiTextState) {
                        is AiTextUiState.Idle -> Unit
                        is AiTextUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("AI লেখা তৈরি করছে…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiTextUiState.Streaming, is AiTextUiState.Success -> {
                            val contentText = when (state) {
                                is AiTextUiState.Streaming -> state.text
                                is AiTextUiState.Success -> state.text
                                else -> ""
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = contentText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            if (state is AiTextUiState.Success) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            MediaExportHelper.copyToClipboard(context, "AI Post", state.text)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_copy_output_button")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(stringResource(R.string.action_copy_text), maxLines = 1)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.updateFacebookCaption(state.text)
                                            onGoToFacebook()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_send_facebook_button")
                                    ) {
                                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("ফেসবুকে নিন", maxLines = 1)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.applyTextToCardHeadline(state.text)
                                            onGoToPhotoCard()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_to_card_headline_button")
                                    ) {
                                        Icon(Icons.Filled.Title, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("কার্ড শিরোনামে", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.applyTextToCardComment(state.text)
                                            onGoToPhotoCard()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_to_card_comment_button")
                                    ) {
                                        Icon(Icons.Filled.Comment, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("কার্ড মন্তব্যে", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }
                        }
                        is AiTextUiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: 3 Structured AI Photo Card Headlines (News vs Quote mode)
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Title, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("৩টি স্মার্ট ফটো কার্ড হেডলাইন", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "সংবাদ কার্ড বা উক্তি/বক্তব্য কার্ড — মনিটাইজেশন-সেফ শিরোনাম",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CardHookKind.entries.forEachIndexed { idx, kind ->
                            SegmentedButton(
                                selected = selectedHookKind == kind,
                                onClick = { viewModel.setCardHookKind(kind) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = CardHookKind.entries.size),
                                modifier = Modifier.testTag("hook_kind_${kind.name.lowercase()}")
                            ) {
                                Text(kind.labelBn, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = { viewModel.generateCardHeadlineOptions() },
                        enabled = activeArticle != null && cardOptionsState !is CardOptionsUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_card_options_button")
                    ) {
                        Text(
                            if (selectedHookKind == CardHookKind.QUOTE) {
                                "৩টি উক্তি/বক্তব্য কার্ড সাজেস্ট করুন"
                            } else {
                                "৩টি কার্ড হেডলাইন সাজেস্ট করুন"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val optState = cardOptionsState) {
                        is CardOptionsUiState.Idle -> Unit
                        is CardOptionsUiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("সেরা ৩টি অপশন বাছাই হচ্ছে…")
                            }
                        }
                        is CardOptionsUiState.Error -> {
                            Text(
                                text = optState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is CardOptionsUiState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                optState.options.forEachIndexed { idx, opt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "অপশন ${idx + 1} • ${opt.categoryBadge}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(opt.headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            if (opt.speakerName.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "— ${opt.speakerName}" + if (opt.speakerRole.isNotBlank()) " (${opt.speakerRole})" else "",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            if (opt.subCaption.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    opt.subCaption,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.applyCardOption(opt)
                                                    onGoToPhotoCard()
                                                },
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("কার্ডে বসান")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: AI Editorial Image Studio (Prompt Suggest + Aspect Ratio + Download)
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI ছবি তৈরি (কপিরাইট-মুক্ত)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "সংবাদের জন্য নিজস্ব এডিটোরিয়াল ছবি তৈরি করুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.suggestAiImagePrompt { suggested ->
                                    imagePrompt = suggested
                                }
                            },
                            enabled = activeArticle != null && !isSuggestingPrompt,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_suggest_prompt_button")
                        ) {
                            if (isSuggestingPrompt) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সাজেস্ট", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_image_prompt_input"),
                        label = { Text("ছবির বিবরণ (বাংলা বা ইংরেজি)") },
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IMAGE_RATIOS.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedImageRatio == key,
                                onClick = { selectedImageRatio = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("ai_img_ratio_$key")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.generateAiImage(imagePrompt, selectedImageRatio) },
                        enabled = imageGenState !is ImageGenUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_ai_image_button")
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI ছবি তৈরি করুন")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val imgState = imageGenState) {
                        is ImageGenUiState.Idle -> Unit
                        is ImageGenUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "AI ছবি আঁকছে (৩০-৬০ সেকেন্ড লাগতে পারে)…",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        is ImageGenUiState.Success -> {
                            Image(
                                bitmap = imgState.bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_ai_generated_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.applyAiImageToCard(imgState.bitmap)
                                        onGoToPhotoCard()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("apply_ai_image_to_card_button")
                                ) {
                                    Text("ফটো কার্ডে নিন", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.saveAiImageToGallery(imgState.bitmap) },
                                    modifier = Modifier.testTag("download_ai_image_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("সেভ")
                                }
                            }
                        }
                        is ImageGenUiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = imgState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
