package com.example.inkpaperdiary.ui.timeline

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.inkpaperdiary.core.security.AppLockManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.SerifFontFamily
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.components.PaperCard
import com.example.inkpaperdiary.core.designsystem.components.ScrollGlyph
import com.example.inkpaperdiary.core.designsystem.components.StampBadge
import com.example.inkpaperdiary.core.designsystem.components.TagChip
import com.example.inkpaperdiary.core.designsystem.components.WeatherIcon
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.Mood
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.tooling.preview.Preview
import com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val todayFormatter = remember { SimpleDateFormat("yyyy年 M月d日 EEEE", Locale.CHINESE) }
    val currentDateStr = remember { todayFormatter.format(Date()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "日记",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = SansFontFamily,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "日历", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onNavigateToOnThisDay) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "那年今日", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入 TXT 日记", fontFamily = SansFontFamily) },
                                onClick = {
                                    showMoreMenu = false
                                    onImportTxt()
                                },
                                leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("数据与统计", fontFamily = SansFontFamily) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToStats()
                                },
                                leadingIcon = { Icon(Icons.Outlined.BarChart, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("设置", fontFamily = SansFontFamily) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "写日记",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 心情与置顶筛选横条（iOS 极简胶囊风格）
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.onlyPinned,
                        onClick = onTogglePinnedFilter,
                        shape = CapsuleShape,
                        label = { Text("置顶", fontFamily = SansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.onlyPinned,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.5.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                items(Mood.entries) { mood ->
                    val isSelected = uiState.selectedMoodFilter == mood
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetMoodFilter(mood) },
                        shape = CapsuleShape,
                        label = { Text(mood.displayName, fontFamily = SansFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            MoodIcon(
                                mood = mood,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else mood.tintColor
                            )
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.5.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            if (uiState.filteredDiaries.isEmpty()) {
                // 空状态（iOS 极简留白）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScrollGlyph(
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (uiState.diaries.isEmpty()) "暂无日记" else "无匹配日记",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "记录生活中的每一个灵感与瞬间",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = SansFontFamily,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (uiState.diaries.isEmpty()) {
                            Button(
                                onClick = { onNavigateToEditor(null) },
                                shape = CapsuleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("新建日记", fontFamily = SansFontFamily, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // 日记列表 (16dp 间距)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredDiaries, key = { it.id }) { diary ->
                        DiaryCardItem(
                            diary = diary,
                            onClick = { onNavigateToEditor(diary.id) },
                            onTogglePin = { onTogglePin(diary) },
                            onDelete = { onDeleteDiary(diary.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryCardItem(
    diary: Diary,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dayNumFormatter = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val monthYearFormatter = remember { SimpleDateFormat("yyyy.MM", Locale.getDefault()) }

    val dayNum = remember(diary.entryDate) { dayNumFormatter.format(Date(diary.entryDate)) }
    val monthYear = remember(diary.entryDate) { monthYearFormatter.format(Date(diary.entryDate)) }

    PaperCard(
        onClick = onClick,
        hasCeladonAccent = diary.isPinned,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 顶栏：日期微块、心情/天气胶囊徽章、置顶与更多操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 现代 iOS 迷你日历微块 (Date Marker)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = SansFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = monthYear,
                                fontFamily = SansFontFamily,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // 心情与天气胶囊徽章
                    StampBadge(
                        text = diary.mood.displayName,
                        icon = { MoodIcon(mood = diary.mood, modifier = Modifier.size(12.dp)) },
                        tintColor = diary.mood.tintColor
                    )
                    StampBadge(
                        text = diary.weather.displayName,
                        icon = { WeatherIcon(weather = diary.weather, modifier = Modifier.size(12.dp)) },
                        tintColor = diary.weather.tintColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (diary.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "已置顶",
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.secondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (diary.isPinned) "取消置顶" else "置顶此篇", fontFamily = SansFontFamily) },
                                onClick = {
                                    showMenu = false
                                    onTogglePin()
                                },
                                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("移入回收站", fontFamily = SansFontFamily, color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            // 标题
            if (diary.title.isNotBlank()) {
                Text(
                    text = diary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 正文摘要
            Text(
                text = diary.previewText.ifBlank { "（无正文）" },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = SansFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp
            )

            // 图片缩略图展示 (最多 3 张，现代 10dp 圆角)
            if (diary.attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    items(diary.attachments.take(3)) { attachment ->
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
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            // 底部：标签、地点与字数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (diary.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(diary.tags) { tag ->
                            TagChip(text = tag.name)
                        }
                    }
                } else if (!diary.locationName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "位置",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = diary.locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 字数统计
                Text(
                    text = "${diary.wordCount} 字",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = SansFontFamily,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "复古纸质日记时间轴 - 浅色模式")
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
        ),
        Diary(
            id = "2",
            title = "细雨中的漫步",
            contentMarkdown = "初秋的小雨带着丝丝凉意，撑着伞走在青石板路上，空气中满是泥土与桂花的香气。",
            mood = Mood.HAPPY,
            weather = Weather.RAINY,
            locationName = "锦里古街",
            tags = listOf(Tag("3", "生活"))
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

@Preview(showBackground = true, name = "复古纸质日记时间轴 - 深色墨夜")
@Composable
private fun TimelineScreenPreviewDark() {
    val sampleDiaries = listOf(
        Diary(
            id = "1",
            title = "夜读随想",
            contentMarkdown = "夜深人静，桌前一盏微光。记录下今日所得与思考...",
            mood = Mood.FULFILLED,
            weather = Weather.CLOUDY,
            isPinned = true,
            tags = listOf(Tag("1", "思考"))
        )
    )

    PaperDiaryTheme(darkTheme = true) {
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
