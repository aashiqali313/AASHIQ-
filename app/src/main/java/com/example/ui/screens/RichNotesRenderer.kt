package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject

// Theme colors to align beautifully with the Dark Cinematic theme
val PremiumGold = Color(0xFFD4AF37)
val SublimeTeal = Color(0xFF008080)
val SoftCyan = Color(0xFFE0FFFF)
val DarkBackground = Color(0xFF0F0F0F)
val CardBackground = Color(0xFF161616)
val SubduedGray = Color(0xFF9E9E9E)
val ErrorRed = Color(0xFFCF6679)

/**
 * Root Premium Notes Rendering System.
 * Detects format: HTML, JSON, or custom Aashiq Markdown
 */
@Composable
fun RichNotesRenderer(
    noteContent: String,
    onOpenPdf: (String) -> Unit = {},
    onOpenLesson: (String) -> Unit = {}
) {
    val trimmed = noteContent.trim()
    
    when {
        (trimmed.startsWith("<html") || trimmed.startsWith("<!DOCTYPE html")) -> {
            PremiumHtmlWebView(htmlContent = trimmed)
        }
        (trimmed.startsWith("{") && trimmed.endsWith("}")) -> {
            JsonLessonBlockRenderer(jsonString = trimmed, onOpenPdf, onOpenLesson)
        }
        else -> {
            AashiqMarkdownRenderer(rawContent = trimmed, onOpenPdf, onOpenLesson)
        }
    }
}

// ==========================================
// 1. ADVANCED HTML/CSS SYSTEM (.html)
// ==========================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PremiumHtmlWebView(htmlContent: String) {
    val isSystemDark = isSystemInDarkTheme()
    val backgroundHex = if (isSystemDark) "#0F0F0F" else "#FAF9F6"
    val textHex = if (isSystemDark) "#E0E0E0" else "#212121"
    
    // Inject premium styles to guarantee glassmorphism & responsive visuals inside the Web container
    val styledHtml = remember(htmlContent, isSystemDark) {
        val styleHeader = """
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    background-color: $backgroundHex;
                    color: $textHex;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                    padding: 16px;
                    margin: 0;
                    word-wrap: break-word;
                }
                h1, h2, h3 {
                    color: #D4AF37;
                    font-weight: 700;
                    letter-spacing: 0.5px;
                }
                h1 { border-bottom: 1px solid rgba(212, 175, 55, 0.3); padding-bottom: 8px; margin-top: 24px; }
                a { color: #D4AF37; text-decoration: none; font-weight: bold; }
                a:hover { text-decoration: underline; }
                .grid-container {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(240px, 1f));
                    gap: 16px;
                    margin: 16px 0;
                }
                .card {
                    background: rgba(40, 40, 40, 0.4);
                    border: 1px solid rgba(212, 175, 55, 0.15);
                    border-radius: 12px;
                    padding: 16px;
                    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
                    backdrop-filter: blur(8px);
                }
                .column-2 {
                    display: flex;
                    flex-flow: row wrap;
                    gap: 16px;
                }
                .column {
                    flex: 1 1 200px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 16px 0;
                    border-radius: 8px;
                    overflow: hidden;
                }
                th {
                    background-color: rgba(212, 175, 55, 0.2);
                    color: #D4AF37;
                    text-align: left;
                    padding: 12px;
                    font-weight: bold;
                }
                td {
                    padding: 12px;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
                }
                tr:hover {
                    background-color: rgba(255, 255, 255, 0.02);
                }
                code {
                    background-color: rgba(0, 0, 0, 0.3);
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 0.9em;
                }
                pre {
                    background-color: rgba(0, 0, 0, 0.4);
                    padding: 16px;
                    border-radius: 8px;
                    border-left: 3px solid #D4AF37;
                    overflow-x: auto;
                }
                .quote-block {
                    border-left: 4px solid #D4AF37;
                    background: rgba(212, 175, 55, 0.05);
                    padding: 12px 16px;
                    margin: 16px 0;
                    border-radius: 0 8px 8px 0;
                    font-style: italic;
                }
                .info { border-left-color: #008080; background: rgba(0, 128, 128, 0.05); }
                .warning { border-left-color: #CF6679; background: rgba(207, 102, 121, 0.05); }
            </style>
            </head>
        """.trimIndent()
        
        if (htmlContent.contains("<head>")) {
            htmlContent.replace("<head>", styleHeader)
        } else {
            "<html>$styleHeader<body>$htmlContent</body></html>"
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                setBackgroundColor(0x00000000) // Transparent to support background matching
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null)
        }
    )
}

// ==========================================
// 2. STRUCTURED JSON LESSON BLOCKS
// ==========================================

@Composable
fun JsonLessonBlockRenderer(
    jsonString: String,
    onOpenPdf: (String) -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val items = remember(jsonString) {
        val list = mutableListOf<JsonBlock>()
        try {
            val root = JSONObject(jsonString)
            if (root.has("title")) {
                list.add(JsonBlock.Header(
                    title = root.getString("title"),
                    category = root.optString("category", "Premium Lecture")
                ))
            }
            val sections = root.optJSONArray("sections")
            if (sections != null) {
                for (i in 0 until sections.length()) {
                    val sec = sections.getJSONObject(i)
                    val type = sec.optString("type")
                    val block = when (type) {
                        "text" -> JsonBlock.TextMark(sec.getString("content"))
                        "callout" -> JsonBlock.Callout(
                            style = sec.optString("style", "info"),
                            text = sec.getString("content")
                        )
                        "columns" -> {
                            val array = sec.getJSONArray("columns")
                            val colList = mutableListOf<String>()
                            for (c in 0 until array.length()) {
                                colList.add(array.getString(c))
                            }
                            JsonBlock.Columns(colList)
                        }
                        "comparison" -> JsonBlock.Comparison(
                            beforeHeader = sec.optString("left_header", "BEFORE"),
                            afterHeader = sec.optString("right_header", "AFTER"),
                            beforeText = sec.getString("left_text"),
                            afterText = sec.getString("right_text")
                        )
                        "quiz" -> JsonBlock.Quiz(
                            question = sec.getString("question"),
                            options = parseStringArray(sec.getJSONArray("options")),
                            correctAnswerIndex = sec.getInt("answer_index"),
                            explanation = sec.optString("explanation", "")
                        )
                        "flashcard" -> JsonBlock.Flashcard(
                            front = sec.getString("front"),
                            back = sec.getString("back")
                        )
                        "resource" -> JsonBlock.Resource(
                            resType = sec.optString("resource_type", "PDF"),
                            title = sec.getString("title"),
                            uri = sec.getString("uri")
                        )
                        "checklist" -> {
                            val array = sec.getJSONArray("items")
                            val checkList = mutableListOf<String>()
                            for (k in 0 until array.length()) {
                                checkList.add(array.getString(k))
                            }
                            JsonBlock.Checklists(checkList)
                        }
                        "expandable" -> JsonBlock.Expandable(
                            title = sec.getString("title"),
                            content = sec.getString("content")
                        )
                        else -> null
                    }
                    if (block != null) {
                        list.add(block)
                    }
                }
            }
        } catch (e: Exception) {
            list.add(JsonBlock.Callout("warning", "JSON Note Compile Exception: ${e.message}"))
            list.add(JsonBlock.TextMark(jsonString))
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items) { block ->
            when (block) {
                is JsonBlock.Header -> {
                    LessonHeroCard(title = block.title, category = block.category)
                }
                is JsonBlock.TextMark -> {
                    TextWithAnnotation(text = block.text)
                }
                is JsonBlock.Callout -> {
                    CalloutPanel(style = block.style, text = block.text)
                }
                is JsonBlock.Columns -> {
                    MultiColumnView(columns = block.cols)
                }
                is JsonBlock.Comparison -> {
                    ComparisonCard(
                        beforeHeader = block.beforeHeader,
                        afterHeader = block.afterHeader,
                        beforeText = block.beforeText,
                        afterText = block.afterText
                    )
                }
                is JsonBlock.Quiz -> {
                    QuizCard(
                        question = block.question,
                        options = block.options,
                        correctAnswerIndex = block.correctAnswerIndex,
                        explanation = block.explanation
                    )
                }
                is JsonBlock.Flashcard -> {
                    FlashcardItem(front = block.front, back = block.back)
                }
                is JsonBlock.Resource -> {
                    ResourceBox(
                        type = block.resType,
                        title = block.title,
                        uri = block.uri,
                        onOpen = { if (block.resType.lowercase().contains("pdf")) onOpenPdf(block.uri) else onOpenLesson(block.uri) }
                    )
                }
                is JsonBlock.Checklists -> {
                    ChecklistLayout(items = block.items)
                }
                is JsonBlock.Expandable -> {
                    ExpandableCard(title = block.title, content = block.content)
                }
            }
        }
    }
}

private fun parseStringArray(jsonArray: JSONArray): List<String> {
    val list = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getString(i))
    }
    return list
}

sealed class JsonBlock {
    data class Header(val title: String, val category: String) : JsonBlock()
    data class TextMark(val text: String) : JsonBlock()
    data class Callout(val style: String, val text: String) : JsonBlock()
    data class Columns(val cols: List<String>) : JsonBlock()
    data class Comparison(val beforeHeader: String, val afterHeader: String, val beforeText: String, val afterText: String) : JsonBlock()
    data class Quiz(val question: String, val options: List<String>, val correctAnswerIndex: Int, val explanation: String) : JsonBlock()
    data class Flashcard(val front: String, val back: String) : JsonBlock()
    data class Resource(val resType: String, val title: String, val uri: String) : JsonBlock()
    data class Checklists(val items: List<String>) : JsonBlock()
    data class Expandable(val title: String, val content: String) : JsonBlock()
}


// ==========================================
// 3. ADVANCED AASHIQ MARKDOWN RENDERER
// ==========================================

@Composable
fun AashiqMarkdownRenderer(
    rawContent: String,
    onOpenPdf: (String) -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val blocks = remember(rawContent) {
        parseAashiqMarkdown(rawContent)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(blocks) { block ->
            when (block) {
                is MarkdownBlock.HeaderH1 -> {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = PremiumGold,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    HorizontalDivider(color = PremiumGold.copy(alpha = 0.2f), thickness = 1.dp)
                }
                is MarkdownBlock.HeaderH2 -> {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.RegularText -> {
                    TextWithAnnotation(text = block.text)
                }
                is MarkdownBlock.MarkdownTable -> {
                    MarkdownTableLayout(headers = block.headers, rows = block.rows)
                }
                is MarkdownBlock.AashiqCallout -> {
                    CalloutPanel(style = block.style, text = block.content)
                }
                is MarkdownBlock.AashiqComparison -> {
                    ComparisonCard(
                        beforeHeader = block.leftHeader,
                        afterHeader = block.rightHeader,
                        beforeText = block.leftText,
                        afterText = block.rightText
                    )
                }
                is MarkdownBlock.AashiqQuiz -> {
                    QuizCard(
                        question = block.question,
                        options = block.options,
                        correctAnswerIndex = block.correctIndex,
                        explanation = block.explanation
                    )
                }
                is MarkdownBlock.AashiqFlashcard -> {
                    FlashcardItem(front = block.front, back = block.recto)
                }
                is MarkdownBlock.AashiqColumns -> {
                    MultiColumnView(columns = block.columns)
                }
                is MarkdownBlock.InlineChecklist -> {
                    ChecklistLayout(items = block.items)
                }
                is MarkdownBlock.AashiqEmbedResource -> {
                    ResourceBox(
                        type = block.resType,
                        title = block.title,
                        uri = block.uri,
                        onOpen = { if (block.resType.lowercase().contains("pdf")) onOpenPdf(block.uri) else onOpenLesson(block.uri) }
                    )
                }
                is MarkdownBlock.AashiqExpandable -> {
                    ExpandableCard(title = block.title, content = block.content)
                }
                is MarkdownBlock.InlineRenderImage -> {
                    InlineImageView(url = block.imageUrl, caption = block.caption)
                }
                is MarkdownBlock.Quote -> {
                    QuoteBox(text = block.text)
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class HeaderH1(val title: String) : MarkdownBlock()
    data class HeaderH2(val title: String) : MarkdownBlock()
    data class RegularText(val text: String) : MarkdownBlock()
    data class MarkdownTable(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class AashiqCallout(val style: String, val content: String) : MarkdownBlock()
    data class AashiqComparison(val leftHeader: String, val rightHeader: String, val leftText: String, val rightText: String) : MarkdownBlock()
    data class AashiqQuiz(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String) : MarkdownBlock()
    data class AashiqFlashcard(val front: String, val recto: String) : MarkdownBlock()
    data class AashiqColumns(val columns: List<String>) : MarkdownBlock()
    data class InlineChecklist(val items: List<String>) : MarkdownBlock()
    data class AashiqEmbedResource(val resType: String, val title: String, val uri: String) : MarkdownBlock()
    data class AashiqExpandable(val title: String, val content: String) : MarkdownBlock()
    data class InlineRenderImage(val imageUrl: String, val caption: String) : MarkdownBlock()
}

/**
 * Parses the raw .md / .aashiqnote file content into rich render segments.
 */
fun parseAashiqMarkdown(rawContent: String): List<MarkdownBlock> {
    val lines = rawContent.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    
    var index = 0
    val max = lines.size
    
    while (index < max) {
        val line = lines[index]
        val trimmed = line.trim()
        
        when {
            trimmed.startsWith("### ") || trimmed.startsWith("## ") -> {
                val clean = trimmed.substringAfter("#").trim()
                blocks.add(MarkdownBlock.HeaderH2(clean))
                index++
            }
            trimmed.startsWith("# ") -> {
                val clean = trimmed.removePrefix("# ").trim()
                blocks.add(MarkdownBlock.HeaderH1(clean))
                index++
            }
            trimmed.startsWith("> ") -> {
                val clean = trimmed.removePrefix("> ").trim()
                blocks.add(MarkdownBlock.Quote(clean))
                index++
            }
            // Parse custom syntax tags (e.g., :::comparison)
            trimmed.startsWith(":::comparison") -> {
                index++
                var leftHeader = "BEFORE"
                var rightHeader = "AFTER"
                var leftText = ""
                var rightText = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    val part = lines[index].trim()
                    if (part.contains("|")) {
                        val split = part.split("|")
                        if (split.size == 2) {
                            if (split[0].trim().startsWith("[") || leftText.isEmpty()) {
                                leftHeader = split[0].trim().replace("[", "").replace("]", "")
                                rightHeader = split[1].trim().replace("[", "").replace("]", "")
                            } else {
                                leftText += (if (leftText.isEmpty()) "" else "\n") + split[0].trim()
                                rightText += (if (rightText.isEmpty()) "" else "\n") + split[1].trim()
                            }
                        }
                    } else if (part.isNotBlank()) {
                        leftText += (if (leftText.isEmpty()) "" else "\n") + part
                    }
                    index++
                }
                blocks.add(MarkdownBlock.AashiqComparison(leftHeader, rightHeader, leftText, rightText))
                index++ // skip closing :::
            }
            trimmed.startsWith(":::quiz") -> {
                index++
                var question = ""
                val options = mutableListOf<String>()
                var answerIndex = 0
                var explanation = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    val part = lines[index].trim()
                    when {
                        part.startsWith("Question:") -> question = part.substringAfter("Question:").trim()
                        part.startsWith("Explanation:") -> explanation = part.substringAfter("Explanation:").trim()
                        part.startsWith("Answer:") -> {
                            val rawArg = part.substringAfter("Answer:").trim()
                            answerIndex = rawArg.lowercase().firstOrNull()?.minus('a') ?: 0
                            if (answerIndex < 0 || answerIndex > 3) {
                                answerIndex = rawArg.toIntOrNull() ?: 0
                            }
                        }
                        part.isNotBlank() && (part.startsWith("A)") || part.startsWith("B)") || part.startsWith("C)") || part.startsWith("D)") || part.startsWith("- ")) -> {
                            val cl = part.replace(Regex("^[A-D]\\)\\s*"), "").replace(Regex("^-\\s*"), "").trim()
                            options.add(cl)
                        }
                    }
                    index++
                }
                blocks.add(MarkdownBlock.AashiqQuiz(question, options, answerIndex, explanation))
                index++
            }
            trimmed.startsWith(":::flashcard") -> {
                index++
                var front = ""
                var back = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    val part = lines[index].trim()
                    if (part.startsWith("Front:")) front = part.substringAfter("Front:").trim()
                    if (part.startsWith("Back:")) back = part.substringAfter("Back:").trim()
                    index++
                }
                blocks.add(MarkdownBlock.AashiqFlashcard(front, back))
                index++
            }
            trimmed.startsWith(":::columns") -> {
                index++
                val colContents = mutableListOf<String>()
                var currentText = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    val part = lines[index].trim()
                    if (part.startsWith("[") && part.endsWith("]")) {
                        if (currentText.isNotBlank()) {
                            colContents.add(currentText.trim())
                        }
                        currentText = ""
                    } else {
                        currentText += (if (currentText.isEmpty()) "" else "\n") + part
                    }
                    index++
                }
                if (currentText.isNotBlank()) {
                    colContents.add(currentText.trim())
                }
                blocks.add(MarkdownBlock.AashiqColumns(colContents))
                index++
            }
            trimmed.startsWith(":::resource") -> {
                index++
                var type = "PDF"
                var title = "Attachment"
                var uri = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    val part = lines[index].trim()
                    if (part.startsWith("Type:")) type = part.substringAfter("Type:").trim()
                    if (part.startsWith("Title:")) title = part.substringAfter("Title:").trim()
                    if (part.startsWith("Uri:")) uri = part.substringAfter("Uri:").trim()
                    index++
                }
                blocks.add(MarkdownBlock.AashiqEmbedResource(type, title, uri))
                index++
            }
            trimmed.startsWith(":::expandable") -> {
                val header = trimmed.removePrefix(":::expandable").trim()
                index++
                var innerText = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    innerText += (if (innerText.isEmpty()) "" else "\n") + lines[index]
                    index++
                }
                blocks.add(MarkdownBlock.AashiqExpandable(header, innerText))
                index++
            }
            trimmed.startsWith(":::info") || trimmed.startsWith(":::warning") || trimmed.startsWith(":::tip") -> {
                val style = trimmed.removePrefix(":::").trim()
                index++
                var calloutText = ""
                while (index < max && !lines[index].trim().startsWith(":::")) {
                    calloutText += (if (calloutText.isEmpty()) "" else "\n") + lines[index]
                    index++
                }
                blocks.add(MarkdownBlock.AashiqCallout(style, calloutText))
                index++
            }
            // Embed Render Inline Images (e.g. ![Caption](url))
            trimmed.startsWith("![") -> {
                val caption = trimmed.substringAfter("[").substringBefore("]")
                val url = trimmed.substringAfter("(").substringBefore(")")
                blocks.add(MarkdownBlock.InlineRenderImage(url, caption))
                index++
            }
            // Checklists
            (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) -> {
                val chkList = mutableListOf<String>()
                while (index < max && (lines[index].trim().startsWith("- [ ]") || lines[index].trim().startsWith("- [x]") || lines[index].trim().startsWith("- [X]"))) {
                    val item = lines[index].trim().substring(5).trim()
                    chkList.add(item)
                    index++
                }
                blocks.add(MarkdownBlock.InlineChecklist(chkList))
            }
            // Markdown tables parsing
            trimmed.startsWith("|") -> {
                val headers = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                index++
                // Pass dash lines
                if (index < max && lines[index].trim().startsWith("|--") || lines[index].trim().startsWith("|-")) {
                    index++
                }
                val rows = mutableListOf<List<String>>()
                while (index < max && lines[index].trim().startsWith("|")) {
                    val cells = lines[index].trim().split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (cells.size >= headers.size) {
                        rows.add(cells.take(headers.size))
                    }
                    index++
                }
                blocks.add(MarkdownBlock.MarkdownTable(headers, rows))
            }
            else -> {
                if (trimmed.isNotBlank()) {
                    blocks.add(MarkdownBlock.RegularText(trimmed))
                }
                index++
            }
        }
    }
    return blocks
}


// ==========================================
// 4. BEAUTIFIER COMMONS UI & CUSTOM WIDGETS
// ==========================================

@Composable
fun TextWithAnnotation(text: String) {
    val annotated = remember(text) {
        buildAnnotatedString {
            var i = 0
            val limit = text.length
            while (i < limit) {
                when {
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append("**")
                            i += 2
                        }
                    }
                    text.startsWith("`", i) -> {
                        val end = text.indexOf("`", i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = PremiumGold, background = Color(0xFF222222))) {
                                append("  " + text.substring(i + 1, end) + "  ")
                            }
                            i = end + 1
                        } else {
                            append("`")
                            i += 1
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = SubduedGray,
        lineHeight = 22.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun LessonHeroCard(title: String, category: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumGold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun CalloutPanel(style: String, text: String) {
    val (borderColor, backTint, icon) = when (style.lowercase().trim()) {
        "warning" -> Triple(ErrorRed, ErrorRed.copy(alpha = 0.08f), Icons.Default.Warning)
        "tip" -> Triple(PremiumGold, PremiumGold.copy(alpha = 0.08f), Icons.Default.Lightbulb)
        else -> Triple(SublimeTeal, SublimeTeal.copy(alpha = 0.08f), Icons.Default.Info)
    }

    Surface(
        color = backTint,
        border = BorderStroke(0.5.dp, borderColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun QuoteBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(PremiumGold.copy(alpha = 0.08f), RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
            .border(
                BorderStroke(1.dp, Brush.horizontalGradient(listOf(PremiumGold, Color.Transparent))),
                RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = PremiumGold.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * 2 & 3 Multi-Column Split Screen rendering
 */
@Composable
fun MultiColumnView(columns: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        columns.forEach { col ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                TextWithAnnotation(text = col)
            }
        }
    }
}

/**
 * Premium Metric Comparison: Before vs After cards
 */
@Composable
fun ComparisonCard(
    beforeHeader: String,
    afterHeader: String,
    beforeText: String,
    afterText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "TRANSFORMATION SPEC",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PremiumGold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Before Case
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                border = BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = beforeHeader.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = beforeText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                }
            }
            // After Case
            Card(
                colors = CardDefaults.cardColors(containerColor = SublimeTeal.copy(alpha = 0.08f)),
                border = BorderStroke(0.5.dp, SublimeTeal.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = afterHeader.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = afterText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                }
            }
        }
    }
}

/**
 * Quiz Element supporting interactive responses, states, and instant explanations.
 */
@Composable
fun QuizCard(
    question: String,
    options: List<String>,
    correctAnswerIndex: Int,
    explanation: String
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "INTERACTIVE QUIZ", style = MaterialTheme.typography.labelSmall, color = PremiumGold, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = question, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(14.dp))

            options.forEachIndexed { optIndex, option ->
                val isSelected = selectedIndex == optIndex
                val isCorrect = optIndex == correctAnswerIndex
                val blockColor = when {
                    !submitted -> if (isSelected) PremiumGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                    isCorrect -> Color(0xFF1B391B).copy(alpha = 0.2f) // correct green
                    isSelected -> Color(0xFF3F1B1B).copy(alpha = 0.2f) // incorrect red
                    else -> MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                }
                
                val borderCol = when {
                    !submitted -> if (isSelected) PremiumGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    isCorrect -> Color(0xFF4CAF50)
                    isSelected -> ErrorRed
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(blockColor, RoundedCornerShape(8.dp))
                        .border(0.5.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable(enabled = !submitted) {
                            selectedIndex = optIndex
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(if (isSelected) PremiumGold else Color.DarkGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = if (submitted && isCorrect) Icons.Default.Check else Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = option, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!submitted) {
                Button(
                    onClick = { if (selectedIndex != null) submitted = true },
                    enabled = selectedIndex != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "SUBMIT RESPONSE", color = if (selectedIndex != null) Color.Black else SubduedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val wasRight = selectedIndex == correctAnswerIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (wasRight) Color(0xFF1B391B).copy(alpha = 0.3f) else Color(0xFF3F1B1B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (wasRight) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (wasRight) Color(0xFF4CAF50) else ErrorRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (wasRight) "Excellent Work! Correct Answer." else "Nice Try! Review Solution Below.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wasRight) Color(0xFF81C784) else Color(0xFFE57373)
                        )
                        if (explanation.isNotEmpty()) {
                            Text(text = explanation, fontSize = 11.sp, color = SubduedGray, modifier = Modifier.padding(top = 4.dp), lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Flashcard Item with Tap-To-Flip 3D animated mechanics.
 */
@Composable
fun FlashcardItem(front: String, back: String) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "FlipAnim")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = if (isFlipped) 0.4f else 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(vertical = 4.dp)
            .clickable { isFlipped = !isFlipped }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Indicator Label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFlipped) Icons.Default.FlipToBack else Icons.Default.FlipToFront, 
                        contentDescription = null, 
                        tint = PremiumGold, 
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFlipped) "SOLUTION MEMORY" else "FLASH STUDY DECK", 
                        fontSize = 9.sp, 
                        color = PremiumGold, 
                        fontWeight = FontWeight.Bold, 
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isFlipped) back else front,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Custom table rendering for beautiful Markdown tabular content
 */
@Composable
fun MarkdownTableLayout(headers: List<String>, rows: List<List<String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PremiumGold.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                headers.forEach { hdr ->
                    Text(
                        text = hdr.uppercase(),
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            
            // Content rows
            rows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(8.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Embed Resource Clickable box triggers
 */
@Composable
fun ResourceBox(
    type: String,
    title: String,
    uri: String,
    onOpen: () -> Unit
) {
    val (icon, tint) = if (type.lowercase().contains("pdf")) {
        Pair(Icons.Default.PictureAsPdf, ErrorRed)
    } else {
        Pair(Icons.Default.Link, SublimeTeal)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, tint.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Connect offline resource • $type", fontSize = 10.sp, color = SubduedGray)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SubduedGray, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Checkbox Checklist Layout which state survives locally for the reading process
 */
@Composable
fun ChecklistLayout(items: List<String>) {
    var checkStates = remember(items) {
        mutableStateMapOf<Int, Boolean>().apply {
            items.forEachIndexed { i, _ -> this[i] = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.PlaylistAddCheck, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "MEMORIZATION CHECKLIST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PremiumGold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        items.forEachIndexed { idx, itm ->
            val checked = checkStates[idx] ?: false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checkStates[idx] = !checked }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { checkStates[idx] = !checked },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (checked) PremiumGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = itm,
                    fontSize = 12.sp,
                    color = if (checked) SubduedGray else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }
    }
}

/**
 * Collapsible Accordion Expandable Card
 */
@Composable
fun ExpandableCard(title: String, content: String) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "Collapsible")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SubduedGray,
                    modifier = Modifier
                        .rotate(arrowRotation)
                        .size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))
                    TextWithAnnotation(text = content)
                }
            }
        }
    }
}

/**
 * Click to fullscreen visual inline imagers
 */
@Composable
fun InlineImageView(url: String, caption: String) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = url,
            contentDescription = caption,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .clickable { showDialog = true }
        )
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                fontSize = 11.sp,
                color = SubduedGray,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url,
                        contentDescription = caption,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showDialog = false }
                            .align(Alignment.Center)
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = caption, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(text = "Tap anywhere to exit preview", fontSize = 11.sp, color = SubduedGray)
                    }

                    IconButton(
                        onClick = { showDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                }
            }
        }
    }
}
