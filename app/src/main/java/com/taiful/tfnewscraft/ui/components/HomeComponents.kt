package com.taiful.tfnewscraft.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.taiful.tfnewscraft.R
import com.taiful.tfnewscraft.data.model.HomeMode
import com.taiful.tfnewscraft.data.model.LiveFeedMetadata
import com.taiful.tfnewscraft.data.model.LiveNewsTab
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.data.model.NewspaperSource

fun isLikelyNewsUrl(input: String): Boolean {
    val t = input.trim().lowercase()
    return t.startsWith("http://") ||
        t.startsWith("https://") ||
        t.startsWith("www.") ||
        t.contains(".com") ||
        t.contains(".tv") ||
        t.contains(".net") ||
        t.contains(".bd") ||
        t.contains("youtu.be") ||
        t.contains("fb.watch")
}

/**
 * Clean, spacious hero card used on the minimal Home Hub (3 cards only).
 */
@Composable
fun HomeLauncherCard(
    title: String,
    icon: ImageVector,
    accentStart: Color,
    accentEnd: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(accentStart, accentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "$title খুলুন",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Compact back-navigation header bar shown inside each of the 3 workspaces.
 */
@Composable
fun SectionBackHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("section_back_to_home_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "হোম পেজে ফিরুন"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

@Composable
fun QuickLinkAndSearchCard(
    input: String,
    homeMode: HomeMode,
    isFetchingUrl: Boolean,
    onInputChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onClearInput: () -> Unit,
    onSubmitPrimary: () -> Unit,
    onOpenManualDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrl = isLikelyNewsUrl(input)
    val placeholderText = when (homeMode) {
        HomeMode.PAPER_AND_VIDEO -> "পত্রিকা বা YouTube/Facebook লিংক পেস্ট করুন…"
        HomeMode.MULTI_REELS -> "খবরের লিংক পেস্ট করুন বা বিষয় খুঁজুন…"
        HomeMode.LIVE_NEWS -> "খবর খুঁজুন বা যেকোনো লিংক পেস্ট করুন…"
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("news_url_or_search_input"),
                placeholder = {
                    Text(
                        text = placeholderText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(
                        imageVector = if (isUrl) Icons.Filled.Link else Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(
                            onClick = onClearInput,
                            modifier = Modifier.testTag("clear_url_input_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "ইনপুট মুছুন")
                        }
                    } else {
                        IconButton(
                            onClick = onPasteFromClipboard,
                            modifier = Modifier.testTag("paste_clipboard_button")
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "ক্লিপবোর্ড থেকে পেস্ট করুন")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitPrimary() })
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSubmitPrimary,
                    enabled = !isFetchingUrl && input.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fetch_news_button")
                ) {
                    if (isFetchingUrl) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            isUrl && homeMode == HomeMode.MULTI_REELS -> "লিংক রিলসে যোগ"
                            isUrl -> "লিংক থেকে আনুন"
                            else -> "খবর খুঁজুন"
                        },
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onOpenManualDialog,
                    modifier = Modifier.testTag("manual_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (homeMode == HomeMode.MULTI_REELS) "কপি-পেস্ট" else "নিজে লিখুন",
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun GitHubLiveStatusStrip(
    meta: LiveFeedMetadata,
    isLoading: Boolean,
    hasWorkspaceContent: Boolean,
    onRefresh: () -> Unit,
    onRequestClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (meta.isFromGitHubFeed) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (meta.isFromGitHubFeed) {
                        "গিটহাব লাইভ ফিড • প্রতি ১০ মি. আপডেট"
                    } else {
                        "বাংলাদেশ লাইভ আরএসএস ফিড"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.updatedLabelBn.isNotBlank() || meta.totalArticles > 0) {
                    Text(
                        text = buildString {
                            if (meta.updatedLabelBn.isNotBlank()) append("আপডেট: ${meta.updatedLabelBn}")
                            if (meta.totalArticles > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("${meta.totalArticles}টি খবর")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasWorkspaceContent) {
                Surface(
                    onClick = onRequestClean,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("home_inline_clean_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CleaningServices,
                            contentDescription = "কাজ পরিষ্কার করুন",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "ক্লিয়ার",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.testTag("refresh_feed_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.action_refresh)
                )
            }
        }
    }
}

@Composable
fun MultiReelsBasketBanner(
    reelCount: Int,
    onCreateReels: () -> Unit,
    onClearBasket: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "রিলস বাস্কেট • $reelCount টি খবর নির্বাচিত",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "লাইভ নিউজ, পত্রিকা অথবা কপি-পেস্ট থেকে একাধিক খবর যোগ করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                    )
                }
                if (reelCount > 0) {
                    IconButton(
                        onClick = onClearBasket,
                        modifier = Modifier.testTag("home_clear_reel_basket_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "বাস্কেট পরিষ্কার করুন",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCreateReels,
                enabled = reelCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_open_reels_maker_button")
            ) {
                Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (reelCount > 0) "রিলস ভিডিও তৈরি করুন ($reelCount)" else "নিচ থেকে খবর বাছাই করুন",
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EditorialNewsCard(
    article: NewsArticle,
    isActiveCard: Boolean,
    reelSelectionNumber: Int?,
    multiSelectMode: Boolean,
    onMakeCard: () -> Unit,
    onOpenAi: () -> Unit,
    onToggleReel: () -> Unit
) {
    val isInReel = reelSelectionNumber != null
    val highlightCard = if (multiSelectMode) isInReel else isActiveCard

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (multiSelectMode) onToggleReel else onMakeCard),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (highlightCard) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (highlightCard) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (article.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = stringResource(R.string.cd_news_thumb),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = article.sourceName.ifBlank { "সংবাদ" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (article.publishedLabel.isNotBlank()) {
                                Text(
                                    text = article.publishedLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isInReel) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "রিলসে নির্বাচিত",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "#$reelSelectionNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (article.summary.isNotBlank() && !multiSelectMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (multiSelectMode) {
                FilledTonalButton(
                    onClick = onToggleReel,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isInReel) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInReel) "নির্বাচিত (সরাতে ট্যাপ করুন)" else "রিলসের জন্য সিলেক্ট করুন",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onMakeCard,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("ফটো কার্ড", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenAi,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("AI লেখা", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onToggleReel,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isInReel) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                            contentDescription = if (isInReel) "রিলস থেকে সরান" else "রিলসে যোগ করুন",
                            tint = if (isInReel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewspaperDirectoryCard(
    paper: NewspaperSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(paper.accentHex)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier.testTag("browse_paper_${paper.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = paper.nameEn.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.nameBn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = paper.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "${paper.nameBn} খুলুন",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Minimal single-row toolbar for Live News: just 2 compact pill buttons on one line.
 */
@Composable
fun CompactLiveToolbarRow(
    meta: LiveFeedMetadata,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenManualDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onRefresh,
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier
                .weight(1f)
                .testTag("compact_live_refresh_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 1.8.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (meta.totalArticles > 0) {
                        "লাইভ (${meta.totalArticles}) • রিফ্রেশ"
                    } else {
                        "লাইভ রিফ্রেশ"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            onClick = onOpenManualDialog,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f)
                .testTag("compact_manual_write_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "নিজে লিখুন",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Compact 2-tab switcher for Live News:
 * Tab 1: বাংলাদেশ ও ভাইরাল (Bangladesh Politics, Viral Topics, Catchy Nationwide, Viral-Only Entertainment)
 * Tab 2: আন্তর্জাতিক (Iran / Israel / Palestine & Hot International News)
 */
@Composable
fun CompactLiveCategoryTabs(
    selectedTab: LiveNewsTab,
    bdCount: Int,
    intlCount: Int,
    onSelectTab: (LiveNewsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LiveTabPill(
                label = "বাংলাদেশ ও ভাইরাল",
                count = bdCount,
                icon = Icons.Filled.Whatshot,
                selected = selectedTab == LiveNewsTab.BANGLADESH_HOT,
                activeColor = Color(0xFFC81E2B),
                testTag = "live_tab_bangladesh_hot",
                onClick = { onSelectTab(LiveNewsTab.BANGLADESH_HOT) },
                modifier = Modifier.weight(1f)
            )
            LiveTabPill(
                label = "আন্তর্জাতিক",
                count = intlCount,
                icon = Icons.Filled.Public,
                selected = selectedTab == LiveNewsTab.INTERNATIONAL,
                activeColor = Color(0xFF1D4ED8),
                testTag = "live_tab_international",
                onClick = { onSelectTab(LiveNewsTab.INTERNATIONAL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LiveTabPill(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) activeColor else Color.Transparent,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (count > 0) "$label ($count)" else label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Slim single-line search & link paste bar for mobile Live News screen.
 */
@Composable
fun CompactSearchLinkBar(
    input: String,
    isFetchingUrl: Boolean,
    onInputChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onClearInput: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrl = isLikelyNewsUrl(input)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(21.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isUrl) Icons.Filled.Link else Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (input.isEmpty()) {
                    Text(
                        text = "খবর খুঁজুন বা লিংক পেস্ট…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compact_live_search_input")
                )
            }

            if (input.isBlank()) {
                IconButton(
                    onClick = onPasteFromClipboard,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("compact_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "পেস্ট",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onClearInput,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("compact_clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "মুছুন",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    onClick = onSubmit,
                    enabled = !isFetchingUrl,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("compact_search_submit_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isFetchingUrl) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "খুঁজুন",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-compact, scannable mobile Live News list row:
 * - Tapping selects the article in-place and reveals tiny 1-line quick action chips.
 * - Right badge lets you 1-tap toggle into Multi-News Reels without leaving the list.
 */
@Composable
fun CompactLiveNewsRow(
    article: NewsArticle,
    isSelected: Boolean,
    reelSelectionNumber: Int?,
    categoryBadge: String = "",
    isHotBadge: Boolean = false,
    onSelect: () -> Unit,
    onMakeCard: () -> Unit,
    onOpenAi: () -> Unit,
    onReadFull: () -> Unit,
    onToggleReel: () -> Unit
) {
    val isInReel = reelSelectionNumber != null
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 0.7.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("live_news_row_${article.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (article.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = stringResource(R.string.cd_news_thumb),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (categoryBadge.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isHotBadge) {
                                    Color(0xFFC81E2B).copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = categoryBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isHotBadge) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                append(article.sourceName.ifBlank { "সংবাদ" })
                                if (article.publishedLabel.isNotBlank()) {
                                    append(" • ${article.publishedLabel}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick 1-tap toggle for Multi-News Reels selection
                Surface(
                    onClick = onToggleReel,
                    shape = CircleShape,
                    color = if (isInReel) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("live_row_reel_toggle_${article.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isInReel) {
                            Text(
                                text = "$reelSelectionNumber",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "রিলসে যোগ করুন",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniRowActionChip(
                        label = "কার্ড",
                        icon = Icons.Filled.Image,
                        primary = true,
                        onClick = onMakeCard,
                        modifier = Modifier.weight(1f)
                    )
                    MiniRowActionChip(
                        label = "AI",
                        icon = Icons.Filled.AutoAwesome,
                        primary = false,
                        onClick = onOpenAi,
                        modifier = Modifier.weight(1f)
                    )
                    MiniRowActionChip(
                        label = "পড়ুন",
                        icon = Icons.Filled.Article,
                        primary = false,
                        onClick = onReadFull,
                        modifier = Modifier.weight(1f)
                    )
                    MiniRowActionChip(
                        label = if (isInReel) "রিলস ✓" else "রিলস",
                        icon = if (isInReel) Icons.Filled.Check else Icons.Filled.MovieCreation,
                        primary = false,
                        onClick = onToggleReel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniRowActionChip(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
        }
    }
}
