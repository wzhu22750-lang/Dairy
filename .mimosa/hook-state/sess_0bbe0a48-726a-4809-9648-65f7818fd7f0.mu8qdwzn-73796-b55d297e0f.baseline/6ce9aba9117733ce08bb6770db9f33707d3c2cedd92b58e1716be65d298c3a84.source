package com.example.inkpaperdiary.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.inkpaperdiary.core.designsystem.InkType
import com.example.inkpaperdiary.core.designsystem.PaperTypography
import com.example.inkpaperdiary.core.designsystem.ReaderFont
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavBackButton
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Weather
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dairy 2.0 — 阅读页 (The Reading Page)
 *
 * 像 Kindle 一样安静地读：
 * - 内容占满纸面，顶栏与底栏随滚动隐现；
 * - 点击纸面即进入沉浸模式，只剩文字；
 * - 顶缘一根发丝线显示阅读进度；
 * - Aa 随时调出排版面板；
 * - 图片是书页插图：全幅、微圆角、克制的间距。
 */
@Composable
fun ReaderRoute(
    diaryId: String,
    diaryRepository: com.example.inkpaperdiary.data.repository.DiaryRepository,
    settingsViewModel: ReadingSettingsViewModel,
    refreshKey: Int = 0,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    var diary by remember(diaryId) { mutableStateOf<Diary?>(null) }
    LaunchedEffect(diaryId, refreshKey) {
        diary = diaryRepository.getDiaryById(diaryId)
    }

    val loaded = diary
    if (loaded == null) {
        // 加载中的一页纸
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else {
        ReaderScreen(
            diary = loaded,
            onBack = onBack,
            onEdit = onEdit,
            settingsViewModel = settingsViewModel
        )
    }
}

@Composable
fun ReaderScreen(
    diary: Diary,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    settingsViewModel: ReadingSettingsViewModel
) {
    val readingSettings by settingsViewModel.settings.collectAsState()
    val listState = rememberLazyListState()

    // 沉浸模式：点击纸面或向下滚动时隐藏栏
    var chromeVisible by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var viewerImage by remember { mutableStateOf<String?>(null) }

    // 滚动方向感知：下滑隐藏、上滑显现
    LaunchedEffect(listState) {
        var lastOffset = 0f
        snapshotFlow {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()
            if (first != null) (first.index * 10000f + first.offset) else 0f
        }.collect { offset ->
            val delta = offset - lastOffset
            if (delta > 40f && chromeVisible) chromeVisible = false
            else if (delta < -40f && !chromeVisible) chromeVisible = true
            lastOffset = offset
        }
    }

    val blocks = remember(diary.contentMarkdown, diary.attachments) {
        MdParser.parse(diary.contentMarkdown)
    }
    val hasLegacyAttachments = diary.attachments.isNotEmpty()

    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total <= 1) 1f
            else listState.firstVisibleItemIndex.toFloat() / (total - 1).coerceAtLeast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { chromeVisible = !chromeVisible }
                )
            }
    ) {
        // ---- 正文 ----
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 88.dp,
                bottom = 120.dp
            )
        ) {
            // 页首：日期章节
            item(key = "header") {
                ReaderHeader(diary = diary)
            }
            // 正文块
            items(blocks.size, key = { "b$it" }) { index ->
                val block = blocks[index]
                MdBlockView(
                    block = block,
                    onImageTap = { viewerImage = it }
                )
            }
            // 旧条目的附件（未写入正文的图片）作为末尾插图
            if (hasLegacyAttachments) {
                item(key = "legacy_images") {
                    LegacyAttachmentFigures(
                        diary = diary,
                        onImageTap = { viewerImage = it }
                    )
                }
            }
            // 页脚：安静的收尾
            item(key = "footer") {
                ReaderFooter(diary = diary)
            }
        }

        // ---- 顶栏（随滚动隐现）----
        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosNavBackButton(
                        onNavigateBack = onBack,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Aa 排版
                    Box(
                        modifier = Modifier.iosIconClick { showSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aa",
                            fontFamily = SansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                    // 编辑
                    Box(
                        modifier = Modifier.iosIconClick { onEdit(diary.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑这一页",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                // 阅读进度：顶缘发丝线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // ---- 底栏（随滚动隐现）：阅读时长 ----
        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(bottom = 24.dp, top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val minutes = (diary.wordCount / 400).coerceAtLeast(1)
                Text(
                    text = "约 $minutes 分钟",
                    style = InkType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- 全屏插图查看器 ----
        viewerImage?.let { src ->
            ImageViewerOverlay(
                imageSrc = src,
                onDismiss = { viewerImage = null }
            )
        }
    }

    // ---- 排版面板 ----
    ReadingSettingsSheet(
        visible = showSettingsSheet,
        settings = readingSettings,
        onDismiss = { showSettingsSheet = false },
        onSetFont = { settingsViewModel.setFont(it) },
        onIncreaseFont = { settingsViewModel.increaseFontScale() },
        onDecreaseFont = { settingsViewModel.decreaseFontScale() },
        onSetLineSpacing = { settingsViewModel.setLineSpacing(it) },
        onSetPageWidth = { settingsViewModel.setPageWidth(it) },
        onSetThemeMode = { settingsViewModel.setThemeMode(it) }
    )
}

// ---------------------------------------------------------------------------
// 页面元素
// ---------------------------------------------------------------------------

/** 页首：日期章节 + 天气/地点元信息 + 标题。 */
@Composable
private fun ReaderHeader(diary: Diary) {
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE) }
    val dateText = remember(diary.entryDate) { dateFormatter.format(Date(diary.entryDate)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = dateText,
            style = InkType.chapterLabel(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val metaParts = buildList {
            // 天气默认值（晴）不展示：只呈现有信息量的元数据
            if (diary.weather != Weather.SUNNY) add(diary.weather.displayName)
            if (!diary.locationName.isNullOrBlank()) add(diary.locationName!!)
        }
        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metaParts.joinToString(" · "),
                style = InkType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (diary.title.isNotBlank()) {
            Text(
                text = diary.title,
                style = InkType.readerTitle(),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/** 单个 Markdown 块 → 书页元素。 */
@Composable
fun MdBlockView(
    block: MdBlock,
    onImageTap: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        when (block) {
            is MdBlock.Heading -> {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = block.text,
                    style = InkType.body().copy(
                        fontSize = InkType.body().fontSize * when (block.level) {
                            1 -> 1.4f
                            2 -> 1.2f
                            else -> 1.08f
                        },
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = InkType.body().lineHeight * 1.15f
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            is MdBlock.Paragraph -> {
                Spacer(modifier = Modifier.height(if (block.text.startsWith("\n")) 0.dp else 2.dp))
                Text(
                    text = renderInline(block.text),
                    style = InkType.body(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            is MdBlock.Quote -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    // 左侧发丝竖线
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(((block.lines.size * 26) + 8).dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        block.lines.forEach { line ->
                            Text(
                                text = renderInline(line),
                                style = InkType.quote(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            is MdBlock.ListItem -> {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = if (block.done == null) block.marker else if (block.done) "■" else "□",
                        style = InkType.body().copy(fontFamily = SansFontFamily, fontSize = InkType.body().fontSize * 0.9f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = renderInline(block.text),
                        style = InkType.body(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            is MdBlock.Divider -> {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            is MdBlock.Image -> {
                Spacer(modifier = Modifier.height(10.dp))
                BookFigure(src = block.src, onImageTap = onImageTap)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

/** 书页插图：全幅、2dp 微圆角、可点击放大。 */
@Composable
fun BookFigure(
    src: String,
    onImageTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model = remember(src) {
        val cleaned = src.trim().removePrefix("file://")
        if (cleaned.startsWith("http") || cleaned.startsWith("content://")) cleaned
        else File(cleaned).takeIf { it.exists() } ?: src
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .build(),
        contentDescription = "书页插图",
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onImageTap(src) }
    )
}

/** 旧条目的附件插图（未内联到正文的图片）。 */
@Composable
private fun LegacyAttachmentFigures(
    diary: Diary,
    onImageTap: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        diary.attachments.forEach { att ->
            val model = remember(att) {
                if (File(att.localPath).exists()) File(att.localPath) else att.remoteUrl
            }
            val src = att.localPath.ifBlank { att.remoteUrl ?: "" }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = "书页插图",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { if (src.isNotBlank()) onImageTap(src) }
            )
        }
    }
}

/** 页脚：字数与收笔时间，安静的落款。 */
@Composable
private fun ReaderFooter(diary: Diary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${diary.wordCount} 字",
            style = InkType.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 行内格式
// ---------------------------------------------------------------------------

private val boldPattern = Regex("""\*\*(.+?)\*\*""")
private val italicPattern = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
private val codePattern = Regex("""`([^`\n]+?)`""")

/** 行内 Markdown 渲染：粗体 / 斜体 / 行内代码。 */
internal fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    data class Span(val start: Int, val end: Int, val style: SpanStyle, val strip: Int)

    val spans = mutableListOf<Span>()
    boldPattern.findAll(text).forEach { spans += Span(it.range.first, it.range.last + 1, SpanStyle(fontWeight = FontWeight.Bold), 2) }
    codePattern.findAll(text).forEach { spans += Span(it.range.first, it.range.last + 1, SpanStyle(fontFamily = FontFamily.Monospace), 1) }
    italicPattern.findAll(text).forEach { spans += Span(it.range.first, it.range.last + 1, SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), 1) }

    // 丢弃互相重叠的样式区间，保留最靠前的
    spans.sortBy { it.start }
    val consumed = mutableListOf<Span>()
    var lastEnd = 0
    for (span in spans) {
        if (span.start >= lastEnd) {
            consumed += span
            lastEnd = span.end
        }
    }

    var cursor = 0
    for (span in consumed) {
        if (span.start > cursor) append(text.substring(cursor, span.start))
        pushStyle(span.style)
        append(text.substring(span.start + span.strip, span.end - span.strip))
        pop()
        cursor = span.end
    }
    if (cursor < text.length) append(text.substring(cursor))
}
