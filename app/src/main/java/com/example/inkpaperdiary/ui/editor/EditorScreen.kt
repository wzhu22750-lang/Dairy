package com.example.inkpaperdiary.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.inkpaperdiary.core.designsystem.InkType
import com.example.inkpaperdiary.core.designsystem.components.IosDateTimePickerSheet
import com.example.inkpaperdiary.core.designsystem.components.IosDialogAction
import com.example.inkpaperdiary.core.designsystem.components.IosDialogTextField
import com.example.inkpaperdiary.core.designsystem.components.IosModalDialog
import com.example.inkpaperdiary.core.designsystem.components.IosActionItem
import com.example.inkpaperdiary.core.designsystem.components.IosActionSheet
import com.example.inkpaperdiary.core.designsystem.components.TagChip
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavBackButton
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.domain.model.Attachment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dairy 2.0 — 数字纸张 (Digital Paper)
 *
 * 进入即写作：一页纸、一个日期、一个标题、一段正文。
 * - 无工具栏、无心情/天气选择器、无元数据行；
 * - 落笔即自动保存，顶栏右侧只有一行安静的保存状态；
 * - 图片与"附加信息"（地点/标签/时间/置顶）收进底部信息面板；
 * - 阅读交给阅读页，这里只负责写。
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 附加信息面板（地点 / 标签 / 日期 / 置顶）
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDateTimePickerSheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf("") }
    var locationInputText by remember { mutableStateOf("") }

    // 系统返回键：保存草稿后离开（草稿已由自动保存持续落盘，这里只是兜底 flush）
    BackHandler {
        viewModel.flushDraftNow()
        onNavigateBack()
    }

    // 进入后台（Home 键/切应用/深色模式切换前的 onStop）：立即落盘，不等防抖
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.flushDraftNow()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        AppLockManager.isPickerActive = false
        if (uri != null) {
            viewModel.addImage(uri)
        }
    }

    // Markdown 正文输入控制
    var contentValue by remember { mutableStateOf(TextFieldValue(uiState.contentMarkdown)) }
    LaunchedEffect(uiState.contentMarkdown) {
        if (contentValue.text != uiState.contentMarkdown) {
            contentValue = TextFieldValue(uiState.contentMarkdown)
        }
    }

    val dateHeaderFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    val dateHeaderText = remember(uiState.entryDate) {
        dateHeaderFormatter.format(Date(uiState.entryDate))
    }
    val savedTimeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // ---- 顶栏：返回 / 日期 / 保存状态 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosNavBackButton(
                onNavigateBack = {
                    viewModel.flushDraftNow()
                    onNavigateBack()
                },
                tint = MaterialTheme.colorScheme.onBackground
            )

            // 日期：点按可修改（低频操作，安静地居中）
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateHeaderText,
                    style = InkType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.iosClick { showInfoSheet = true }
                )
            }

            // 完成：把当前草稿转正为正式日记
            Text(
                text = "完成",
                style = InkType.meta,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .iosClick { viewModel.finishDiary(onNavigateBack) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            // 保存状态：写作流中唯一的"状态指示"
            Text(
                text = when {
                    uiState.isSaving -> "保存中…"
                    uiState.lastSavedAt > 0 -> "已保存 ${savedTimeFormatter.format(Date(uiState.lastSavedAt))}"
                    else -> ""
                },
                style = InkType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        // ---- 纸面 ----
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // 页首：日期（章回体）
            Text(
                text = dateHeaderText,
                style = InkType.chapterLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 标题
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                textStyle = InkType.readerTitle().copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    if (uiState.title.isEmpty()) {
                        Text(
                            text = "标题",
                            style = InkType.readerTitle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 正文：整张纸都是它
            BasicTextField(
                value = contentValue,
                onValueChange = {
                    contentValue = it
                    viewModel.updateContent(it.text)
                },
                textStyle = InkType.body().copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    if (contentValue.text.isEmpty()) {
                        Text(
                            text = "落笔…",
                            style = InkType.body(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 320.dp)
            )

            // 配图：纸面底部的插图行（删除在此处，阅读时成为书页插图）
            if (uiState.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                EditorAttachmentStrip(
                    attachments = uiState.attachments,
                    onRemove = { viewModel.removeAttachment(it) }
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        // ---- 底部：仅两个动作 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 插入图片
            Box(
                modifier = Modifier.iosIconClick {
                    AppLockManager.isPickerActive = true
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "插入图片",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
            // 附加信息（地点 / 标签 / 时间 / 置顶）
            Box(
                modifier = Modifier.iosIconClick { showInfoSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "附加信息",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${wordCount(uiState.contentMarkdown)} 字",
                style = InkType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ---- 附加信息面板 ----
    if (showInfoSheet) {
        IosActionSheet(
            visible = true,
            title = "附加信息",
            actions = listOf(
                IosActionItem(
                    title = if (uiState.isPinned) "取消置顶" else "置顶这一页",
                    isChecked = false,
                    onClick = { viewModel.togglePinned() }
                ),
                IosActionItem(
                    title = "修改日期与时间",
                    onClick = {
                        showInfoSheet = false
                        showDateTimePickerSheet = true
                    }
                ),
                IosActionItem(
                    title = if (uiState.locationName.isNullOrBlank()) "添加地点" else "修改地点（${uiState.locationName}）",
                    onClick = {
                        showInfoSheet = false
                        locationInputText = uiState.locationName ?: ""
                        showLocationDialog = true
                    }
                ),
                IosActionItem(
                    title = if (uiState.tags.isEmpty()) "添加标签" else "管理标签（${uiState.tags.size}）",
                    onClick = {
                        showInfoSheet = false
                        showTagDialog = true
                    }
                )
            ),
            onDismissRequest = { showInfoSheet = false }
        )
    }

    // ---- 日期时间选择 ----
    IosDateTimePickerSheet(
        visible = showDateTimePickerSheet,
        initialTimestamp = uiState.entryDate,
        onConfirm = { newTimestamp ->
            viewModel.updateEntryDate(newTimestamp)
            showDateTimePickerSheet = false
        },
        onDismissRequest = { showDateTimePickerSheet = false }
    )

    // ---- 标签管理（复用添加对话框；已有标签点按标签本身移除） ----
    IosModalDialog(
        visible = showTagDialog,
        title = "标签",
        message = "输入以添加标签",
        confirmText = "添加",
        cancelText = "完成",
        onConfirm = {
            if (tagInputText.isNotBlank()) {
                viewModel.addTag(tagInputText.trim())
                tagInputText = ""
            } else {
                showTagDialog = false
            }
        },
        onDismissRequest = {
            tagInputText = ""
            showTagDialog = false
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (uiState.tags.isNotEmpty()) {
                FlowTagRow(
                    tags = uiState.tags.map { it.name },
                    onRemove = { name ->
                        uiState.tags.firstOrNull { it.name == name }?.let { viewModel.removeTag(it.id) }
                    }
                )
            }
            IosDialogTextField(
                value = tagInputText,
                onValueChange = { tagInputText = it },
                placeholder = "如：随笔、生活、阅读",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // ---- 草稿恢复提示：检测到未完成的草稿，交还用户决定，绝不强制覆盖 ----
    val pendingDraft = uiState.pendingDraft
    if (pendingDraft != null) {
        IosActionSheet(
            visible = true,
            title = if (pendingDraft.diaryId == null) "发现未完成的日记" else "上次编辑未完成",
            message = "检测到 ${savedTimeFormatter.format(Date(pendingDraft.updatedTime))} 自动保存的草稿，是否继续编辑？",
            actions = listOf(
                IosActionItem(
                    title = "继续编辑",
                    onClick = { viewModel.continuePendingDraft() }
                ),
                IosActionItem(
                    title = if (pendingDraft.diaryId == null) "删除草稿" else "还原为已保存版本",
                    isDestructive = true,
                    onClick = {
                        if (pendingDraft.diaryId == null) {
                            viewModel.discardPendingDraft()
                        } else {
                            viewModel.revertPendingDraftToDiary()
                        }
                    }
                )
            ),
            cancelText = "暂不处理",
            onDismissRequest = { viewModel.dismissPendingDraftPrompt() }
        )
    }

    // ---- 地点输入 ----
    IosModalDialog(
        visible = showLocationDialog,
        title = "地点",
        message = "记录此刻书写的位置",
        actions = listOf(
            IosDialogAction(title = "清除", onClick = {
                viewModel.updateLocation(null)
                showLocationDialog = false
            }),
            IosDialogAction(
                title = "保存",
                isDefault = true,
                onClick = {
                    viewModel.updateLocation(locationInputText.ifBlank { null })
                    showLocationDialog = false
                }
            )
        ),
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

/** 编辑器内字数：中文按字，西文按词。 */
internal fun wordCount(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    val cjk = trimmed.count { it.code in 0x3400..0x4DBF || it.code in 0x4E00..0x9FFF ||
        it.code in 0xF900..0xFAFF || it.code in 0x3040..0x30FF || it.code in 0xAC00..0xD7AF }
    val latin = Regex("[A-Za-z0-9]+(?:['’-][A-Za-z0-9]+)*")
        .findAll(trimmed)
        .count()
    return cjk + latin
}

/** 编辑器插图行：小缩略图 + 删除角标。 */
@Composable
fun EditorAttachmentStrip(
    attachments: List<Attachment>,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    Column {
        Text(
            text = "插图",
            style = InkType.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            attachments.forEach { att ->
                val imageModel = remember(att) {
                    if (File(att.localPath).exists()) File(att.localPath) else att.remoteUrl
                }
                Box(modifier = Modifier.size(84.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "插图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .background(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                CircleShape
                            )
                            .iosIconClick { onRemove(att.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "删除插图",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 标签流式排列（用于标签管理对话框）。 */
@Composable
private fun FlowTagRow(
    tags: List<String>,
    onRemove: (String) -> Unit
) {
    // 简易流式布局：单行放不下则换行
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.chunked(4).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowTags.forEach { name ->
                    TagChip(text = name, onRemove = { onRemove(name) })
                }
            }
        }
    }
}
