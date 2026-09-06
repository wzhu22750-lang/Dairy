package com.example.inkpaperdiary.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.inkpaperdiary.core.security.AppLockManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
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
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.designsystem.components.StampBadge
import com.example.inkpaperdiary.core.designsystem.components.TagChip
import com.example.inkpaperdiary.core.designsystem.components.WeatherIcon
import com.example.inkpaperdiary.core.designsystem.components.paperTexture
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Weather
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 系统返回键 = 保存并返回（与工具栏返回行为一致，避免丢失未保存内容）
    BackHandler { viewModel.saveDiary(onNavigateBack) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        AppLockManager.isPickerActive = false
        if (uri != null) {
            viewModel.addImage(uri)
        }
    }

    var showTagDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    var locationInputText by remember { mutableStateOf("") }

    val dateTimeFormatter = remember { SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()) }
    val formattedDateTime = remember(uiState.entryDate) { dateTimeFormatter.format(Date(uiState.entryDate)) }

    // 日期时间选择弹窗
    val calendar = remember(uiState.entryDate) {
        Calendar.getInstance().apply { timeInMillis = uiState.entryDate }
    }

    fun pickDateTime() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        viewModel.updateEntryDate(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Markdown 正文：本地持有 TextFieldValue 以支持光标位置插入
    var contentValue by remember { mutableStateOf(TextFieldValue(uiState.contentMarkdown)) }
    LaunchedEffect(uiState.contentMarkdown) {
        if (contentValue.text != uiState.contentMarkdown) {
            contentValue = TextFieldValue(uiState.contentMarkdown)
        }
    }

    // Markdown 工具栏插入辅助函数：块级前缀插到行首，行内符号包围选区/光标处
    val blockPrefixes = setOf("# ", "## ", "- ", "1. ", "- [ ] ", "> ")
    fun insertMarkdown(prefix: String, suffix: String = "") {
        val current = contentValue
        val text = current.text
        val start = current.selection.start.coerceIn(0, text.length)
        val end = current.selection.end.coerceIn(start, text.length)

        val newText: String
        val newCursor: Int
        if (suffix.isNotEmpty()) {
            // 行内样式：包围选区，无选区则纯插入
            val selected = text.substring(start, end)
            newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            newCursor = start + prefix.length + selected.length
        } else if (prefix in blockPrefixes) {
            // 块级前缀：插入到当前行行首；不在行首时先换行
            val lineStart = text.lastIndexOf('\n', start - 1) + 1
            val atLineStart = lineStart == start
            val sep = if (atLineStart) "" else "\n"
            val insertPos = if (atLineStart) lineStart else start
            newText = text.substring(0, insertPos) + sep + prefix + text.substring(insertPos)
            newCursor = insertPos + sep.length + prefix.length
        } else {
            // 分割线等整块追加
            val sep = if (text.isEmpty() || text.endsWith("\n")) "" else "\n"
            newText = text + sep + prefix
            newCursor = newText.length
        }
        contentValue = TextFieldValue(newText, selection = TextRange(newCursor))
        viewModel.updateContent(newText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        onClick = { pickDateTime() },
                        shape = CapsuleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = "修改时间",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formattedDateTime,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = SansFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveDiary(onNavigateBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回并保存",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePinned() }) {
                        Icon(
                            imageVector = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "置顶",
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = { viewModel.saveDiary(onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = CapsuleShape,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("完成", fontFamily = SansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Markdown 快捷排版工具栏（iOS 极简通透毛玻璃工具栏）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(AppleMaterials.glassBorder(width = 0.5.dp)),
                color = AppleMaterials.backgroundColor(MaterialThickness.REGULAR),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        IconButton(
                            onClick = {
                                AppLockManager.isPickerActive = true
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "插入照片", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    item { VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outline) }
                    item {
                        IconButton(onClick = { insertMarkdown("# ") }) {
                            Text("H1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("## ") }) {
                            Text("H2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("**", "**") }) {
                            Text("B", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("*", "*") }) {
                            Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface, fontFamily = SansFontFamily)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("- ") }) {
                            Icon(Icons.Outlined.FormatListBulleted, contentDescription = "无序列表", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("1. ") }) {
                            Icon(Icons.Outlined.FormatListNumbered, contentDescription = "有序列表", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("- [ ] ") }) {
                            Icon(Icons.Outlined.CheckBox, contentDescription = "待办事项", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("> ") }) {
                            Icon(Icons.Outlined.FormatQuote, contentDescription = "引用", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item {
                        IconButton(onClick = { insertMarkdown("---\n") }) {
                            Icon(Icons.Outlined.HorizontalRule, contentDescription = "分割线", tint = MaterialTheme.colorScheme.onSurface)
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
            // 心情选择行
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "今日心情",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = SansFontFamily,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            modifier = Modifier.clickable { viewModel.updateMood(mood) }
                        )
                    }
                }
            }

            // 天气选择行
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "今日天气",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = SansFontFamily,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            modifier = Modifier.clickable { viewModel.updateWeather(weather) }
                        )
                    }
                }
            }

            // 地点与标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 地点按钮 (iOS 极简胶囊风格)
                Surface(
                    onClick = {
                        locationInputText = uiState.locationName ?: ""
                        showLocationDialog = true
                    },
                    shape = CapsuleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
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

                // 添加标签按钮
                Surface(
                    onClick = { showTagDialog = true },
                    shape = CapsuleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
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

                // 标签展示
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.tags) { tag ->
                        TagChip(
                            text = tag.name,
                            onRemove = { viewModel.removeTag(tag.id) }
                        )
                    }
                }
            }

            // 图片预览网格 (iOS 12dp 圆角)
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
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            IconButton(
                                onClick = { viewModel.removeAttachment(att.id) },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
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

            // 正文编辑区：纯净通透纸面
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

    // 标签添加弹窗
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("添加标签", fontFamily = SansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tagInputText,
                    onValueChange = { tagInputText = it },
                    placeholder = { Text("输入标签名称，如：随笔、生活", fontFamily = SansFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTag(tagInputText)
                        tagInputText = ""
                        showTagDialog = false
                    }
                ) {
                    Text("确定", fontFamily = SansFontFamily, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("取消", fontFamily = SansFontFamily, color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    // 地点输入弹窗
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("记录地点", fontFamily = SansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = locationInputText,
                    onValueChange = { locationInputText = it },
                    placeholder = { Text("输入地点，如：咖啡馆、书房", fontFamily = SansFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateLocation(locationInputText.ifBlank { null })
                        showLocationDialog = false
                    }
                ) {
                    Text("确定", fontFamily = SansFontFamily, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("取消", fontFamily = SansFontFamily, color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "简洁编辑器")
@Composable
private fun EditorScreenPreview() {
    com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}
