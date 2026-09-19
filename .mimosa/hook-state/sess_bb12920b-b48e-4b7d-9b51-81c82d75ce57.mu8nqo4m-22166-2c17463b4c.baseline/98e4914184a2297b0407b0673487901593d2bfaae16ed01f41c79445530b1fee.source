package com.example.inkpaperdiary.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.SerifFontFamily
import com.example.inkpaperdiary.core.designsystem.components.*
import com.example.inkpaperdiary.core.designsystem.interaction.SuppressMaterialRipples
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavIconButton
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavTextButton
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Weather
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // 脏数据检查：初次加载完成时拍摄快照
    var initialSnapshot by remember { mutableStateOf<EditorUiState?>(null) }
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded && initialSnapshot == null) {
            initialSnapshot = uiState
        }
    }

    val isDirty by remember(uiState, initialSnapshot) {
        derivedStateOf {
            val snap = initialSnapshot ?: return@derivedStateOf false
            if (snap.title.isEmpty() && snap.contentMarkdown.isEmpty() && snap.attachments.isEmpty()) {
                uiState.title.isNotBlank() ||
                    uiState.contentMarkdown.isNotBlank() ||
                    uiState.attachments.isNotEmpty() ||
                    uiState.tags.isNotEmpty() ||
                    uiState.locationName != null
            } else {
                uiState.title != snap.title ||
                    uiState.contentMarkdown != snap.contentMarkdown ||
                    uiState.mood != snap.mood ||
                    uiState.weather != snap.weather ||
                    uiState.locationName != snap.locationName ||
                    uiState.entryDate != snap.entryDate ||
                    uiState.tags != snap.tags ||
                    uiState.attachments != snap.attachments ||
                    uiState.isPinned != snap.isPinned
            }
        }
    }

    var showDiscardSheet by remember { mutableStateOf(false) }
    var showDateTimePickerSheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    var locationInputText by remember { mutableStateOf("") }

    // 系统返回键处理：有脏数据时弹窗确认，无脏数据时直接退出
    BackHandler {
        if (isDirty) {
            showDiscardSheet = true
        } else {
            onNavigateBack()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        AppLockManager.isPickerActive = false
        if (uri != null) {
            viewModel.addImage(uri)
        }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()) }
    val formattedDateTime = remember(uiState.entryDate) { dateTimeFormatter.format(Date(uiState.entryDate)) }

    // Markdown 正文输入控制
    var contentValue by remember { mutableStateOf(TextFieldValue(uiState.contentMarkdown)) }
    LaunchedEffect(uiState.contentMarkdown) {
        if (contentValue.text != uiState.contentMarkdown) {
            contentValue = TextFieldValue(uiState.contentMarkdown)
        }
    }

    val blockPrefixes = setOf("# ", "## ", "- ", "1. ", "- [ ] ", "> ")
    fun insertMarkdown(prefix: String, suffix: String = "") {
        val current = contentValue
        val text = current.text
        val start = current.selection.start.coerceIn(0, text.length)
        val end = current.selection.end.coerceIn(start, text.length)

        val newText: String
        val newCursor: Int
        if (suffix.isNotEmpty()) {
            val selected = text.substring(start, end)
            newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            newCursor = start + prefix.length + selected.length
        } else if (prefix in blockPrefixes) {
            val lineStart = text.lastIndexOf('\n', start - 1) + 1
            val atLineStart = lineStart == start
            val sep = if (atLineStart) "" else "\n"
            val insertPos = if (atLineStart) lineStart else start
            newText = text.substring(0, insertPos) + sep + prefix + text.substring(insertPos)
            newCursor = insertPos + sep.length + prefix.length
        } else {
            val sep = if (text.isEmpty() || text.endsWith("\n")) "" else "\n"
            newText = text + sep + prefix
            newCursor = newText.length
        }
        contentValue = TextFieldValue(newText, selection = TextRange(newCursor))
        viewModel.updateContent(newText)
    }

    SuppressMaterialRipples {
        Scaffold(
            topBar = {
                // iOS 模态顶部导航栏 (44dp + status bar insets)
                val barBgColor = AppleMaterials.barBackgroundColor(isDark)
                val separatorColor = AppleMaterials.separatorColor(isDark)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(barBgColor)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 左侧：取消文字按钮
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            IosNavTextButton(
                                text = "取消",
                                onClick = {
                                    if (isDirty) {
                                        showDiscardSheet = true
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                            )
                        }

                        // 中间：日期时间胶囊药丸按钮
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            EditorDateTimeCapsulePill(
                                formattedDateTime = formattedDateTime,
                                onClick = { showDateTimePickerSheet = true }
                            )
                        }

                        // 右侧：置顶图标 + 完成按钮
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IosNavIconButton(
                                icon = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (uiState.isPinned) "取消置顶" else "置顶",
                                tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                onClick = { viewModel.togglePinned() }
                            )
                            IosNavTextButton(
                                text = "完成",
                                isPrimary = true,
                                onClick = { viewModel.saveDiary(onNavigateBack) }
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = separatorColor)
                }
            },
            bottomBar = {
                // Markdown 快捷排版工具栏（跟随键盘升起，0.5dp 镜面高光顶边）
                val isImeVisible = WindowInsets.isImeVisible
                val barBgColor = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark)
                val borderBrush = if (isDark) {
                    Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x14FFFFFF)))
                } else {
                    Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x1F000000)))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .then(
                            if (!isImeVisible) Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            else Modifier
                        )
                        .background(barBgColor)
                ) {
                    // 0.5dp 镜面高光发丝顶边
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(borderBrush)
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            IosEditorToolButton(
                                onClick = {
                                    AppLockManager.isPickerActive = true
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentDescription = "插入照片"
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = "插入照片",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(0.5.dp)
                                    .height(20.dp)
                                    .background(AppleMaterials.separatorColor(isDark))
                            )
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("# ") }, contentDescription = "一级标题") {
                                Text("H1", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("## ") }, contentDescription = "二级标题") {
                                Text("H2", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("**", "**") }, contentDescription = "粗体") {
                                Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("*", "*") }, contentDescription = "斜体") {
                                Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SerifFontFamily)
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("- ") }, contentDescription = "无序列表") {
                                Icon(Icons.Outlined.FormatListBulleted, contentDescription = "无序列表", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("1. ") }, contentDescription = "有序列表") {
                                Icon(Icons.Outlined.FormatListNumbered, contentDescription = "有序列表", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("- [ ] ") }, contentDescription = "待办事项") {
                                Icon(Icons.Outlined.CheckBox, contentDescription = "待办事项", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("> ") }, contentDescription = "引用") {
                                Icon(Icons.Outlined.FormatQuote, contentDescription = "引用", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        item {
                            IosEditorToolButton(onClick = { insertMarkdown("---\n") }, contentDescription = "分割线") {
                                Icon(Icons.Outlined.HorizontalRule, contentDescription = "分割线", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 今日心情选择行
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "今日心情",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = SansFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Mood.entries) { mood ->
                            val isSelected = uiState.mood == mood
                            StampBadge(
                                text = mood.displayName,
                                icon = {
                                    MoodIcon(
                                        mood = mood,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                },
                                tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.iosClick { viewModel.updateMood(mood) }
                            )
                        }
                    }
                }

                // 今日天气选择行
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "今日天气",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = SansFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Weather.entries) { weather ->
                            val isSelected = uiState.weather == weather
                            StampBadge(
                                text = weather.displayName,
                                icon = {
                                    WeatherIcon(
                                        weather = weather,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                },
                                tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.iosClick { viewModel.updateWeather(weather) }
                            )
                        }
                    }
                }

                // 地点与标签行 (iOS 胶囊药丸)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.iosClick {
                            locationInputText = uiState.locationName ?: ""
                            showLocationDialog = true
                        },
                        shape = CapsuleShape,
                        color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                        border = AppleMaterials.glassBorder(width = 0.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = "地点",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.locationName.takeIf { !it.isNullOrBlank() } ?: "添加地点",
                                fontFamily = SansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.iosClick { showTagDialog = true },
                        shape = CapsuleShape,
                        color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                        border = AppleMaterials.glassBorder(width = 0.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "添加标签",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "添加标签",
                                fontFamily = SansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(uiState.tags) { tag ->
                            TagChip(
                                text = tag.name,
                                onRemove = { viewModel.removeTag(tag.id) }
                            )
                        }
                    }
                }

                // 图片预览网格 (12dp 圆角 + 弹簧触感删除按钮)
                if (uiState.attachments.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(uiState.attachments) { att ->
                            val imageModel = remember(att) {
                                if (File(att.localPath).exists()) File(att.localPath) else att.remoteUrl
                            }
                            Box(modifier = Modifier.size(90.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "配图",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(AppleMaterials.glassBorder(width = 0.5.dp), RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .iosIconClick { viewModel.removeAttachment(att.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "删除图片",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 标题输入框
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                        Text(
                            "标题",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 正文编辑区：纯净纸面纹理
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 420.dp)
                        .paperTexture(
                            pattern = PaperPattern.RULED_LINES,
                            lineColor = MaterialTheme.colorScheme.outline,
                            lineSpacing = 28.dp
                        )
                ) {
                    TextField(
                        value = contentValue,
                        onValueChange = {
                            contentValue = it
                            viewModel.updateContent(it.text)
                        },
                        placeholder = {
                            Text(
                                "记录此时此刻…\n支持 Markdown 格式与下栏快捷排版",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = SansFontFamily,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SansFontFamily,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 放弃修改确认 Action Sheet
        IosActionSheet(
            visible = showDiscardSheet,
            title = "这篇日记有未保存的修改",
            message = "如果退出，所有未保存的修改都将丢失",
            actions = listOf(
                IosActionItem(
                    title = "放弃修改",
                    isDestructive = true,
                    onClick = {
                        showDiscardSheet = false
                        onNavigateBack()
                    }
                ),
                IosActionItem(
                    title = "保存并退出",
                    onClick = {
                        showDiscardSheet = false
                        viewModel.saveDiary(onNavigateBack)
                    }
                )
            ),
            cancelText = "继续编辑",
            onDismissRequest = { showDiscardSheet = false }
        )

        // iOS 日期时间选择器模态面板 (Feature 13 / Explorer M5-2)
        IosDateTimePickerSheet(
            visible = showDateTimePickerSheet,
            initialTimestamp = uiState.entryDate,
            onConfirm = { newTimestamp ->
                viewModel.updateEntryDate(newTimestamp)
                showDateTimePickerSheet = false
            },
            onDismissRequest = { showDateTimePickerSheet = false }
        )

        // 标签添加弹窗 (IosModalDialog + IosDialogTextField)
        IosModalDialog(
            visible = showTagDialog,
            title = "添加标签",
            message = "输入标签名称为日记分类",
            confirmText = "添加",
            cancelText = "取消",
            onConfirm = {
                if (tagInputText.isNotBlank()) {
                    viewModel.addTag(tagInputText.trim())
                    tagInputText = ""
                }
                showTagDialog = false
            },
            onDismissRequest = { showTagDialog = false }
        ) {
            IosDialogTextField(
                value = tagInputText,
                onValueChange = { tagInputText = it },
                placeholder = "如：随笔、生活、阅读",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 地点输入弹窗 (IosModalDialog + IosDialogTextField)
        IosModalDialog(
            visible = showLocationDialog,
            title = "记录地点",
            message = "记录此刻书写的位置",
            confirmText = "保存",
            cancelText = "取消",
            onConfirm = {
                viewModel.updateLocation(locationInputText.ifBlank { null })
                showLocationDialog = false
            },
            onDismissRequest = { showLocationDialog = false }
        ) {
            IosDialogTextField(
                value = locationInputText,
                onValueChange = { locationInputText = it },
                placeholder = "如：咖啡馆、书房、街角",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * iOS 导航栏内嵌紧凑日期时间胶囊组件
 */
@Composable
fun EditorDateTimeCapsulePill(
    formattedDateTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(28.dp)
            .iosClick(
                pressedScale = 0.96f,
                pressedAlpha = 0.85f,
                onClick = onClick
            ),
        shape = CapsuleShape,
        color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
        border = AppleMaterials.glassBorder(width = 0.5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "修改时间",
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formattedDateTime,
                fontSize = 13.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = PaperColors.MonoGray400
            )
        }
    }
}

/**
 * iOS Markdown 排版工具栏紧凑按钮组件（带触觉反馈与按压弹性缩放）
 */
@Composable
fun IosEditorToolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .iosClick(
                pressedScale = 0.94f,
                pressedAlpha = 0.85f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
