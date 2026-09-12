package com.example.inkpaperdiary.ui.timeline

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.PaperTypography
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.components.*
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleItem
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleScaffold
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavIconButton
import com.example.inkpaperdiary.core.designsystem.scaffold.rememberLazyListScrollOffset
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme

/**
 * Apple Journal-style Timeline Screen Composable
 *
 * Implements Apple Human Interface Guidelines (HIG) stream architecture:
 * 1. Pinned translucent large-title navigation bar via IosLargeTitleScaffold.
 * 2. Sliding segmented filter ("全部", "图文", "置顶") and mood capsule selectors.
 * 3. Journal stream cards with 16dp squircles, 0.5dp specular hairline glass borders, left 3dp accent bar, and spring physics.
 * 4. Adaptive multi-photo mosaic grid (1 to 4+ photos) with 12dp squircle corners.
 * 5. Minimalist empty state with 44dp capsule CTA button.
 * 6. Native contextual action sheet on long-press.
 */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToOnThisDay: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val importTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        AppLockManager.isPickerActive = false
        if (uris.isNotEmpty()) {
            viewModel.importTxtFiles(context, uris) { count, failed ->
                val msg = if (failed == 0) "已成功导入 $count 篇日记" else "已导入 $count 篇日记 ($failed 个文件读取失败)"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    TimelineContent(
        uiState = uiState,
        onTogglePinnedFilter = { viewModel.togglePinnedFilter() },
        onSetMoodFilter = { viewModel.setMoodFilter(it) },
        onTogglePin = { viewModel.togglePin(it) },
        onDeleteDiary = { viewModel.deleteDiary(it) },
        onNavigateToEditor = onNavigateToEditor,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToOnThisDay = onNavigateToOnThisDay,
        onNavigateToStats = onNavigateToStats,
        onNavigateToSettings = onNavigateToSettings,
        onImportTxt = {
            AppLockManager.isPickerActive = true
            importTxtLauncher.launch(arrayOf("text/plain", "*/*"))
        }
    )
}

@Composable
fun TimelineContent(
    uiState: TimelineUiState,
    onTogglePinnedFilter: () -> Unit,
    onSetMoodFilter: (Mood?) -> Unit,
    onTogglePin: (Diary) -> Unit,
    onDeleteDiary: (String) -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToOnThisDay: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onImportTxt: () -> Unit = {}
) {
    val todayFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    val currentDateStr = remember { todayFormatter.format(Date()) }

    val listState = rememberLazyListState()
    val scrollOffset = rememberLazyListScrollOffset(listState)

    // 长按卡片呼出的 iOS Action Sheet 目标日记
    var selectedDiaryForAction by remember { mutableStateOf<Diary?>(null) }

    // 分段筛选状态 ("全部", "图文", "置顶")
    val filterSegments = remember { listOf("全部", "图文", "置顶") }
    var selectedSegment by remember { mutableStateOf(if (uiState.onlyPinned) "置顶" else "全部") }

    // 监听外部 uiState.onlyPinned 的变更以保持同步
    LaunchedEffect(uiState.onlyPinned) {
        if (uiState.onlyPinned && selectedSegment != "置顶") {
            selectedSegment = "置顶"
        } else if (!uiState.onlyPinned && selectedSegment == "置顶") {
            selectedSegment = "全部"
        }
    }

    // 响应式数据源过滤
    val displayedDiaries = remember(uiState.diaries, uiState.filteredDiaries, selectedSegment, uiState.selectedMoodFilter) {
        val source = if (uiState.diaries.isNotEmpty()) uiState.diaries else uiState.filteredDiaries
        var list = source
        when (selectedSegment) {
            "全部" -> { /* 无额外附加条件 */ }
            "图文" -> { list = list.filter { it.attachments.isNotEmpty() } }
            "置顶" -> { list = list.filter { it.isPinned } }
        }
        if (uiState.selectedMoodFilter != null) {
            list = list.filter { it.mood == uiState.selectedMoodFilter }
        }
        list
    }

    IosLargeTitleScaffold(
        title = "日记",
        lazyListState = listState,
        actions = {
            // 搜索按钮
            IosNavIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToSearch
            )

            // 顶栏写日记按钮（彻底替代 Android FAB）
            IosNavIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "新建日记",
                tint = MaterialTheme.colorScheme.primary,
                onClick = { onNavigateToEditor(null) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 88.dp // 底部毛玻璃 TabBar 预留安全内边距
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. iOS Large Title 头部（带平滑滚动淡出淡入耦合）
            item(key = "header_large_title") {
                IosLargeTitleItem(
                    title = "日记",
                    subtitle = currentDateStr,
                    scrollOffset = scrollOffset,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 2. iOS 胶囊分段筛选器 ("全部", "图文", "置顶") + 心情横滑胶囊
            item(key = "filter_controls") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IosSegmentedControl(
                        items = filterSegments,
                        selectedItem = selectedSegment,
                        onItemSelected = { selected ->
                            selectedSegment = selected
                            if (selected == "置顶" && !uiState.onlyPinned) {
                                onTogglePinnedFilter()
                            } else if (selected != "置顶" && uiState.onlyPinned) {
                                onTogglePinnedFilter()
                            }
                        },
                        itemLabel = { it }
                    )

                    // 心情筛选横滑胶囊列
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            val isAllMoods = uiState.selectedMoodFilter == null
                            Surface(
                                modifier = Modifier
                                    .height(28.dp)
                                    .iosClick { onSetMoodFilter(null) },
                                shape = CapsuleShape,
                                color = if (isAllMoods) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = AppleMaterials.glassBorder(width = 0.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "全部心情",
                                        fontSize = 11.sp,
                                        fontFamily = SansFontFamily,
                                        fontWeight = if (isAllMoods) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isAllMoods) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        items(Mood.entries) { mood ->
                            val isSelected = uiState.selectedMoodFilter == mood
                            Surface(
                                modifier = Modifier
                                    .height(28.dp)
                                    .iosClick { onSetMoodFilter(mood) },
                                shape = CapsuleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = AppleMaterials.glassBorder(width = 0.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MoodIcon(
                                        mood = mood,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else mood.tintColor
                                    )
                                    Text(
                                        text = mood.displayName,
                                        fontSize = 11.sp,
                                        fontFamily = SansFontFamily,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 列表内容：Apple Journal 风格卡片 或 iOS 极简空状态
            if (displayedDiaries.isEmpty()) {
                item(key = "empty_state") {
                    val isFiltered = uiState.diaries.isNotEmpty() && displayedDiaries.isEmpty()
                    JournalEmptyState(
                        isFiltered = isFiltered,
                        onNewDiaryClick = { onNavigateToEditor(null) },
                        onClearFilterClick = {
                            selectedSegment = "全部"
                            if (uiState.onlyPinned) onTogglePinnedFilter()
                            if (uiState.selectedMoodFilter != null) onSetMoodFilter(null)
                        }
                    )
                }
            } else {
                items(displayedDiaries, key = { it.id }) { diary ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DiaryCardItem(
                            diary = diary,
                            onClick = { onNavigateToEditor(diary.id) },
                            onLongClick = { selectedDiaryForAction = diary }
                        )
                    }
                }
            }
        }

        // 长按日记卡片呼出的 iOS 原生 Action Sheet
        val actionDiary = selectedDiaryForAction
        if (actionDiary != null) {
            val titleText = actionDiary.title.ifBlank {
                actionDiary.previewText.take(28).ifBlank { "日记操作" }
            }
            val dateStr = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE).format(Date(actionDiary.entryDate))
            IosActionSheet(
                visible = true,
                title = titleText,
                message = dateStr,
                actions = listOf(
                    IosActionItem(
                        title = if (actionDiary.isPinned) "取消置顶" else "置顶此篇",
                        icon = if (actionDiary.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        onClick = { onTogglePin(actionDiary) }
                    ),
                    IosActionItem(
                        title = "编辑日记",
                        icon = Icons.Outlined.Edit,
                        onClick = { onNavigateToEditor(actionDiary.id) }
                    ),
                    IosActionItem(
                        title = "移入回收站",
                        icon = Icons.Outlined.Delete,
                        isDestructive = true,
                        onClick = { onDeleteDiary(actionDiary.id) }
                    )
                ),
                cancelText = "取消",
                onDismissRequest = { selectedDiaryForAction = null }
            )
        }
    }
}

/**
 * Apple Journal 风格极简日记流卡片 (Apple Journal Stream Card)
 *
 * 规范：
 * - 16dp 连续平滑圆角卡片 (Squircle) + 0.5dp 细发丝微光玻璃边框
 * - 弹簧交互物理：scale 0.97f, alpha 0.85f, 零水波纹
 * - 清晰文字层级：
 *   - 日期时间：13sp Footnote / Caption (PaperColors.MonoGray500)
 *   - 标题：17sp SemiBold Headline
 *   - 正文：15sp Regular Subheadline
 * - 多图展示：自适应 1~4+ 张图片画廊拼接 (JournalPhotoMosaic)，12dp 连续圆角 + 0.5dp 玻璃边框
 * - 置顶与标签胶囊：JournalCapsulePill, JournalPinnedBadge, 左侧 3dp 药丸指示条
 */
@Composable
fun DiaryCardItem(
    diary: Diary,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formattedDateTime = remember(diary.entryDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = diary.entryDate }
        val nowCal = Calendar.getInstance()
        val pattern = if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            "M月d日 EEEE · HH:mm"
        } else {
            "yyyy年M月d日 EEEE · HH:mm"
        }
        SimpleDateFormat(pattern, Locale.CHINESE).format(Date(diary.entryDate))
    }

    PaperCard(
        onClick = onClick,
        onLongClick = onLongClick,
        hasCeladonAccent = diary.isPinned,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. 顶栏：日期时间 (13sp Footnote) 与状态胶囊 (心情/天气/置顶)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDateTime,
                    style = PaperTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.08).sp,
                        color = PaperColors.MonoGray500
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JournalCapsulePill(
                        text = diary.mood.displayName,
                        icon = { MoodIcon(mood = diary.mood, modifier = Modifier.size(12.dp)) },
                        tintColor = diary.mood.tintColor
                    )
                    JournalCapsulePill(
                        text = diary.weather.displayName,
                        icon = { WeatherIcon(weather = diary.weather, modifier = Modifier.size(12.dp)) },
                        tintColor = diary.weather.tintColor
                    )
                    if (diary.isPinned) {
                        JournalPinnedBadge()
                    }
                }
            }

            // 2. 标题 (17sp Emphasized / Headline)
            if (diary.title.isNotBlank()) {
                Text(
                    text = diary.title,
                    style = PaperTypography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.4).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. 正文摘要 (15sp Subheadline / Secondary)
            Text(
                text = diary.previewText.ifBlank { "（无正文）" },
                style = PaperTypography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.24).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = if (diary.attachments.isNotEmpty()) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )

            // 4. Apple Journal 风格多图画廊拼接 (1 到 4+ 张图片，12dp 圆角 + 0.5dp 玻璃边框)
            if (diary.attachments.isNotEmpty()) {
                JournalPhotoMosaic(
                    attachments = diary.attachments,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 5. 底栏：标签、地点与字数统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (diary.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(diary.tags) { tag ->
                            TagChip(text = tag.name)
                        }
                    }
                } else if (!diary.locationName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "位置",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = diary.locationName,
                            style = PaperTypography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 计数统计 (12sp Caption)
                Text(
                    text = "${diary.wordCount} 字",
                    style = PaperTypography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontFamily = SansFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    }
}

/**
 * Apple Journal 风格多图自适应画廊拼接组件 (1 to 4+ photos)
 *
 * 布局规则：
 * - 1 张：全宽展示 (高 180dp)
 * - 2 张：等分双列并排 (高 130dp, 间距 6dp)
 * - 3 张：左侧 1 张大图 (1.5x 权重) + 右侧上下叠放 2 张小图 (高 160dp)
 * - 4 张：2x2 网格 (每行高 96dp, 间距 6dp)
 * - 5+ 张：2x2 网格，第 4 张覆盖半透明遮罩与 "+N" 提示
 * 所有图片均采用 12dp squircle 连续平滑圆角与 0.5dp 发丝线微光边框。
 */
@Composable
fun JournalPhotoMosaic(
    attachments: List<Attachment>,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    val photoShape = RoundedCornerShape(12.dp)
    val glassBorder = AppleMaterials.glassBorder(width = 0.5.dp)

    when (attachments.size) {
        1 -> {
            // 1 张图片：单幅 Hero 大图
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(photoShape)
                    .border(glassBorder, photoShape)
            ) {
                JournalImageItem(attachment = attachments[0])
            }
        }
        2 -> {
            // 2 张图片：等宽双列并排
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(photoShape)
                        .border(glassBorder, photoShape)
                ) {
                    JournalImageItem(attachment = attachments[0])
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(photoShape)
                        .border(glassBorder, photoShape)
                ) {
                    JournalImageItem(attachment = attachments[1])
                }
            }
        }
        3 -> {
            // 3 张图片：左侧大画幅 (1.5x) + 右侧双小图垂直层叠
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .clip(photoShape)
                        .border(glassBorder, photoShape)
                ) {
                    JournalImageItem(attachment = attachments[0])
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[1])
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[2])
                    }
                }
            }
        }
        4 -> {
            // 4 张图片：经典 2x2 网格
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[0])
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[1])
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[2])
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[3])
                    }
                }
            }
        }
        else -> {
            // 5+ 张图片：2x2 网格，第 4 张带 "+N" 半透明毛玻璃蒙层
            val remainingCount = attachments.size - 3
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[0])
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[1])
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                            .border(glassBorder, photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[2])
                    }
                    // 第 4 张：覆盖暗色半透明 "+N" 提示
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(photoShape)
                    ) {
                        JournalImageItem(attachment = attachments[3])
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$remainingCount",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontFamily = SansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // 细发丝边框置顶
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(glassBorder, photoShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 独立的 Coil 图片加载容器
 */
@Composable
private fun JournalImageItem(
    attachment: Attachment,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel = remember(attachment) {
        if (File(attachment.localPath).exists()) {
            File(attachment.localPath)
        } else {
            attachment.remoteUrl
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .crossfade(true)
            .build(),
        contentDescription = "日记插图",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/**
 * iOS 极简胶囊徽章组件 (Capsule Pill)
 */
@Composable
fun JournalCapsulePill(
    text: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    tintColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier.height(24.dp),
        shape = CapsuleShape,
        color = tintColor.copy(alpha = 0.08f),
        border = AppleMaterials.glassBorder(width = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Text(
                text = text,
                color = tintColor,
                fontFamily = SansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * iOS 置顶指示胶囊 (Journal Pinned Badge)
 */
@Composable
fun JournalPinnedBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(24.dp),
        shape = CapsuleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = AppleMaterials.glassBorder(width = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "已置顶",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "置顶",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = SansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * iOS 风格空状态面板 (Journal Empty State)
 */
@Composable
fun JournalEmptyState(
    isFiltered: Boolean,
    onNewDiaryClick: () -> Unit,
    onClearFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 40.dp, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 72dp 连续圆角毛玻璃图标底座
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = AppleMaterials.glassBorder(width = 0.5.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFiltered) Icons.Outlined.Search else Icons.Outlined.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // iOS 文字排版
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isFiltered) "无匹配日记" else "暂无日记",
                    fontSize = 20.sp,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isFiltered)
                        "当前筛选条件下没有找到日记\n请尝试切换或清除筛选条件"
                    else
                        "记录生活中的每一个灵感与瞬间\n写下当下的所思所想",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontFamily = SansFontFamily,
                    letterSpacing = (-0.24).sp,
                    textAlign = TextAlign.Center,
                    color = PaperColors.MonoGray500
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // iOS 44dp 胶囊 CTA 操作按钮 (带 iOS 弹簧缩放 0.97f 与触感)
            if (!isFiltered) {
                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .iosClick(
                            pressedScale = 0.97f,
                            pressedAlpha = 0.85f,
                            onClick = onNewDiaryClick
                        ),
                    shape = CapsuleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = AppleMaterials.glassBorder(width = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "新建第一篇日记",
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.24).sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .iosClick(
                            pressedScale = 0.97f,
                            pressedAlpha = 0.85f,
                            onClick = onClearFilterClick
                        ),
                    shape = CapsuleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = AppleMaterials.glassBorder(width = 0.5.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "清除筛选条件",
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Apple Journal 风格日记流 - 浅色模式")
@Composable
private fun TimelineScreenPreviewLight() {
    val sampleDiaries = listOf(
        Diary(
            id = "1",
            title = "午后在街角书店",
            contentMarkdown = "今天读完了博尔赫斯的《沙之书》，窗外有微风掠过梧桐树叶，纸页沙沙作响...",
            mood = Mood.CALM,
            weather = Weather.SUNNY,
            locationName = "成都·玉林西路",
            isPinned = true,
            tags = listOf(Tag("1", "阅读"), Tag("2", "随笔"))
        )
    )

    PaperDiaryTheme(darkTheme = false) {
        TimelineContent(
            uiState = TimelineUiState(
                diaries = sampleDiaries,
                filteredDiaries = sampleDiaries
            ),
            onTogglePinnedFilter = {},
            onSetMoodFilter = {},
            onTogglePin = {},
            onDeleteDiary = {},
            onNavigateToEditor = {},
            onNavigateToSearch = {},
            onNavigateToCalendar = {},
            onNavigateToOnThisDay = {},
            onNavigateToStats = {},
            onNavigateToSettings = {}
        )
    }
}
