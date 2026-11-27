package com.taiful.tfnewscraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taiful.tfnewscraft.data.model.NewsArticle
import com.taiful.tfnewscraft.export.MediaExportHelper

/**
 * Restored Original Article Reader ("মূল খবর") modal sheet:
 * - A− / A+ font sizing & Serif / Sans toggle
 * - Inline Edit mode for article Title & Body
 * - 3-way Copy (শিরোনামসহ, শুধু মূল লেখা, সূত্রসহ)
 * - 1-tap "ফেসবুকের জন্য প্রস্তুত করুন"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderSheet(
    article: NewsArticle,
    isPreparingFacebook: Boolean,
    onSaveEdit: (newTitle: String, newBody: String) -> Unit,
    onPrepareForFacebook: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    var useSerif by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember(article.id, article.title) { mutableStateOf(article.title) }
    var editBody by remember(article.id, article.body, article.summary) {
        mutableStateOf(article.body.ifBlank { article.summary })
    }
    var copyFeedback by remember { mutableStateOf("") }

    val fullBodyText = article.body.ifBlank { article.summary }
    val readerFontFamily = if (useSerif) FontFamily.Serif else FontFamily.SansSerif

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Reader Typography Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "মূল খবর পাঠক",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(article.sourceName.ifBlank { "সংবাদ" })
                            if (article.publishedLabel.isNotBlank()) {
                                append(" • ${article.publishedLabel}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Font size & font family pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(13f) },
                        label = { Text("A−") },
                        modifier = Modifier.testTag("reader_font_minus")
                    )
                    FilterChip(
                        selected = false,
                        onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(24f) },
                        label = { Text("A+") },
                        modifier = Modifier.testTag("reader_font_plus")
                    )
                    FilterChip(
                        selected = useSerif,
                        onClick = { useSerif = !useSerif },
                        label = { Text(if (useSerif) "সেরিফ" else "সাধারণ") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.FormatSize,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("reader_font_toggle")
                    )
                }
            }

            // Primary 1-tap Prepare for Facebook + Edit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPrepareForFacebook,
                    enabled = !isPreparingFacebook,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reader_prepare_fb_button")
                ) {
                    if (isPreparingFacebook) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ফেসবুকের জন্য প্রস্তুত করুন", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = {
                        if (isEditing) {
                            onSaveEdit(editTitle, editBody)
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    },
                    modifier = Modifier.testTag("reader_edit_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditing) "সংরক্ষণ" else "সম্পাদনা")
                }
            }

            // 3-Way Copy Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val txt = "${article.title}\n\n$fullBodyText"
                        MediaExportHelper.copyToClipboard(context, "Article With Title", txt)
                        copyFeedback = "শিরোনামসহ মূল খবর কপি হয়েছে"
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reader_copy_with_title")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শিরোনামসহ", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        MediaExportHelper.copyToClipboard(context, "Article Body", fullBodyText)
                        copyFeedback = "শুধু মূল লেখা কপি হয়েছে"
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reader_copy_body_only")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শুধু লেখা", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        val txt = buildString {
                            appendLine(article.title)
                            appendLine()
                            appendLine(fullBodyText)
                            appendLine()
                            append("সূত্র: ${article.sourceName}")
                            if (article.sourceUrl.isNotBlank()) {
                                append(" (${article.sourceUrl})")
                            }
                        }
                        MediaExportHelper.copyToClipboard(context, "Article With Source", txt)
                        copyFeedback = "সূত্রসহ সম্পূর্ণ খবর কপি হয়েছে"
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reader_copy_with_source")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সূত্রসহ", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }

            if (copyFeedback.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copyFeedback,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider()

            // Scrollable Article Content or Inline Editor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("সংবাদের শিরোনাম") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_edit_title_input")
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("মূল সংবাদ বিবরণ") },
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_edit_body_input")
                    )
                } else {
                    Text(
                        text = article.title,
                        fontSize = (fontSizeSp + 4f).sp,
                        lineHeight = (fontSizeSp + 11f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = readerFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fullBodyText.ifBlank { "মূল বিবরণ পাওয়া যায়নি। 'সম্পাদনা' বাটনে ট্যাপ করে লেখা যুক্ত করতে পারেন।" },
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.55f).sp,
                        fontFamily = readerFontFamily,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
