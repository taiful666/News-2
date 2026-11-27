package com.taiful.tfnewscraft.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.CardAspectRatio
import com.taiful.tfnewscraft.data.model.CardFontFamily
import com.taiful.tfnewscraft.data.model.CardImageSource
import com.taiful.tfnewscraft.data.model.CardTextAlign
import com.taiful.tfnewscraft.data.model.LogoPosition
import com.taiful.tfnewscraft.viewmodel.StudioViewModel

private val AccentSwatches: List<Long> = listOf(
    0xFFB91C1CL,
    0xFFD71920L,
    0xFF065F46L,
    0xFF1E3A8AL,
    0xFF0B4F6CL,
    0xFF6B21A8L,
    0xFFC2410CL,
    0xFF111827L
)

@Composable
fun PhotoCardScreen(
    viewModel: StudioViewModel,
    onGoToReels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.cardConfig.collectAsStateWithLifecycle()
    val activePresetId by viewModel.activePresetId.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.renderedCardPreview.collectAsStateWithLifecycle()
    val speakerPhotoBitmap by viewModel.speakerPhotoBitmap.collectAsStateWithLifecycle()

    var showFullscreenPreview by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomCardPhotoFromUri(uri)
        }
    }

    val speakerPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setSpeakerPhotoFromUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live High-Res Photo Card Preview + Next Design / Reset / Fullscreen Toolbar
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Quick Design Toolbar (Next Design, Reset, Fullscreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.cycleNextCardDesign() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("next_card_design_button")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পরবর্তী ডিজাইন", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetCardDesign() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reset_card_design_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("রিসেট", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(
                            onClick = { showFullscreenPreview = true },
                            modifier = Modifier.testTag("fullscreen_card_preview_button")
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "ফুল প্রিভিউ")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val ratio = config.aspectRatio.widthPx.toFloat() / config.aspectRatio.heightPx.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio.coerceIn(0.62f, 1f))
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showFullscreenPreview = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_photo_card_preview),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("প্রিভিউ তৈরি হচ্ছে…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Export Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.savePhotoCardToGallery() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_card_gallery_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সেভ করুন")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.sharePhotoCard() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_card_facebook_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার করুন")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_card_photo_button")
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ছবি বদলান")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.addCurrentCardToReelBasket()
                                onGoToReels()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_to_reel_button")
                        ) {
                            Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("রিলসে নিন")
                        }
                    }
                }
            }
        }

        // Restored 14 Favorite Design Presets Strip
        item {
            Text("ডিজাইন প্রিসেট (${StudioViewModel.CARD_DESIGN_PRESETS.size}টি)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StudioViewModel.CARD_DESIGN_PRESETS, key = { it.id }) { preset ->
                    val selected = activePresetId == preset.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.applyDesignPreset(preset) }
                            .testTag("preset_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.accentArgb))
                                    .border(2.dp, Color(preset.paperBgArgb), CircleShape)
                            )
                            Column {
                                Text(
                                    text = preset.labelBn,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = preset.template.taglineBn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Image Source Switcher & Zoom Slider
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ছবির উৎস ও জুম", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CardImageSource.entries.forEachIndexed { idx, src ->
                            SegmentedButton(
                                selected = config.imageSource == src,
                                onClick = {
                                    if (src == CardImageSource.UPLOAD) {
                                        viewModel.setCardImageSource(src)
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } else {
                                        viewModel.setCardImageSource(src)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = CardImageSource.entries.size),
                                modifier = Modifier.testTag("img_source_${src.name.lowercase()}")
                            ) {
                                Text(src.labelBn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                    if (config.imageSource != CardImageSource.NONE) {
                        Column {
                            Text(
                                text = "ছবির জুম (${(config.photoZoom * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = config.photoZoom,
                                onValueChange = { z -> viewModel.updateCardConfig { it.copy(photoZoom = z) } },
                                valueRange = 1.0f..2.2f
                            )
                        }
                    }
                }
            }
        }

        // Restored Speaker Photo & Attribution Controls (for Quote / Grid Paper Cards)
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("বক্তার ছবি ও পরিচয় (উক্তি কার্ড)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (speakerPhotoBitmap != null) {
                            Image(
                                bitmap = speakerPhotoBitmap!!.asImageBitmap(),
                                contentDescription = "বক্তার ছবি",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(config.accentColorArgb), CircleShape)
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                speakerPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_speaker_photo_button")
                        ) {
                            Icon(Icons.Filled.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (speakerPhotoBitmap != null) "বক্তার ছবি বদলান" else "বক্তার ছবি যোগ", maxLines = 1)
                        }
                        if (speakerPhotoBitmap != null) {
                            IconButton(
                                onClick = { viewModel.toggleFlipSpeakerPhoto() },
                                modifier = Modifier.testTag("flip_speaker_photo_button")
                            ) {
                                Icon(Icons.Filled.Flip, contentDescription = "ছবি উল্টান")
                            }
                            IconButton(
                                onClick = { viewModel.removeSpeakerPhoto() },
                                modifier = Modifier.testTag("remove_speaker_photo_button")
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "বক্তার ছবি সরান")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = config.speakerName,
                            onValueChange = { v -> viewModel.updateCardConfig { it.copy(speakerName = v) } },
                            label = { Text("বক্তার নাম") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speaker_name_input")
                        )
                        OutlinedTextField(
                            value = config.speakerRole,
                            onValueChange = { v -> viewModel.updateCardConfig { it.copy(speakerRole = v) } },
                            label = { Text("পদবি / পরিচয়") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speaker_role_input")
                        )
                    }
                }
            }
        }

        // Aspect Ratio + Accent Swatches + Font + Alignment + Logo Position & Scale
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("কার্ডের সাইজ, রঙ ও স্টাইল", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardAspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = config.aspectRatio == ratio,
                                onClick = { viewModel.updateCardConfig { it.copy(aspectRatio = ratio) } },
                                label = { Text(ratio.labelBn) },
                                modifier = Modifier.testTag("ratio_${ratio.name.lowercase()}")
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(AccentSwatches) { argb ->
                            val isSelected = config.accentColorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateCardConfig { it.copy(accentColorArgb = argb) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "নির্বাচিত রঙ",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Font Family
                    Text("ফন্ট স্টাইল", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CardFontFamily.entries) { font ->
                            FilterChip(
                                selected = config.fontFamily == font,
                                onClick = { viewModel.updateCardConfig { it.copy(fontFamily = font) } },
                                label = { Text(font.labelBn) },
                                modifier = Modifier.testTag("font_${font.name.lowercase()}")
                            )
                        }
                    }

                    // Text Align
                    Text("লেখার অ্যালাইনমেন্ট", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CardTextAlign.entries.forEachIndexed { idx, align ->
                            SegmentedButton(
                                selected = config.textAlign == align,
                                onClick = { viewModel.updateCardConfig { it.copy(textAlign = align) } },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = CardTextAlign.entries.size),
                                modifier = Modifier.testTag("align_${align.name.lowercase()}")
                            ) {
                                Text(align.labelBn)
                            }
                        }
                    }

                    // Logo Position & Size
                    Text("লোগোর অবস্থান", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LogoPosition.entries) { pos ->
                            FilterChip(
                                selected = config.logoPosition == pos,
                                onClick = { viewModel.updateCardConfig { it.copy(logoPosition = pos) } },
                                label = { Text(pos.labelBn, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("logo_pos_${pos.name.lowercase()}")
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "লোগোর আকার (${(config.logoScale * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = config.logoScale,
                            onValueChange = { scale -> viewModel.updateCardConfig { it.copy(logoScale = scale) } },
                            valueRange = 0.6f..1.6f
                        )
                    }
                }
            }
        }

        // Text & Layout Controls
        item {
            ElevatedCard(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("লেখা ও তথ্য সম্পাদনা", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = config.headline,
                        onValueChange = { v -> viewModel.updateCardConfig { it.copy(headline = v) } },
                        label = { Text("ফটো কার্ড শিরোনাম") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_headline_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = config.highlightWord,
                            onValueChange = { v -> viewModel.updateCardConfig { it.copy(highlightWord = v) } },
                            label = { Text("হাইলাইট শব্দ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = config.badgeText,
                            onValueChange = { v -> viewModel.updateCardConfig { it.copy(badgeText = v) } },
                            label = { Text("ব্যাজ / ক্যাটাগরি") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = config.sourceCredit,
                        onValueChange = { v -> viewModel.updateCardConfig { it.copy(sourceCredit = v) } },
                        label = { Text("সংবাদের সূত্র") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CardToggleRow("হাইলাইট রঙ দেখান", config.showHighlight) { checked ->
                        viewModel.updateCardConfig { it.copy(showHighlight = checked) }
                    }

                    CardToggleRow("নিজস্ব মন্তব্য দেখান", config.showComment) { checked ->
                        viewModel.updateCardConfig { it.copy(showComment = checked) }
                    }

                    if (config.showComment) {
                        OutlinedTextField(
                            value = config.ownComment,
                            onValueChange = { v -> viewModel.updateCardConfig { it.copy(ownComment = v) } },
                            label = { Text("নিজস্ব মন্তব্য / সাবটাইটেল") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    CardToggleRow("সূত্র ক্রেডিট দেখান", config.showSourceCredit) { checked ->
                        viewModel.updateCardConfig { it.copy(showSourceCredit = checked) }
                    }

                    CardToggleRow("তারিখ ব্যাজ দেখান", config.showDateBadge) { checked ->
                        viewModel.updateCardConfig { it.copy(showDateBadge = checked) }
                    }

                    CardToggleRow("কার্ডে সংবেদনশীল শব্দ মাস্ক করুন", config.maskSensitiveWords) { checked ->
                        viewModel.updateCardConfig { it.copy(maskSensitiveWords = checked) }
                    }

                    Column {
                        Text(
                            text = "ফন্টের আকার (${(config.headlineScale * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = config.headlineScale,
                            onValueChange = { scale ->
                                viewModel.updateCardConfig { it.copy(headlineScale = scale) }
                            },
                            valueRange = 0.75f..1.25f
                        )
                    }
                }
            }
        }
    }

    // Restored Fullscreen Preview Modal Dialog
    if (showFullscreenPreview && previewBitmap != null) {
        Dialog(
            onDismissRequest = { showFullscreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ফুলস্ক্রিন প্রিভিউ (${config.aspectRatio.labelBn})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(onClick = { showFullscreenPreview = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
                        }
                    }

                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_photo_card_preview),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.savePhotoCardToGallery() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গ্যালারিতে সেভ")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.sharePhotoCard() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
