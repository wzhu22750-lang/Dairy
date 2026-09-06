package com.example.inkpaperdiary.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.components.PaperCard
import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.core.sync.SyncWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val context = LocalContext.current

    // 导入 JSON 备份：通过系统文件选择器选取备份文件
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        AppLockManager.isPickerActive = false
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (content.isNullOrBlank()) {
                Toast.makeText(context, "读取备份文件失败", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.importJsonBackup(content) { count ->
                    Toast.makeText(context, "已导入 $count 篇日记", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 导入 TXT 纯文本日记：支持多选及自动解析旧日期
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

    // 把导出文件通过系统分享/保存选择器交给用户
    fun shareExportedFile(file: File) {
        AppLockManager.isPickerActive = true
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val type = if (file.extension.equals("zip", ignoreCase = true)) "application/zip" else "application/json"
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = type
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "分享备份文件"))
        }.onFailure {
            Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
        }
    }

    var showSupabaseDialog by remember { mutableStateOf(false) }
    var supabaseUrlInput by remember { mutableStateOf(uiState.supabaseUrl) }
    var supabaseKeyInput by remember { mutableStateOf(uiState.supabaseAnonKey) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            Toast.makeText(context, syncMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    val lastSyncStr = remember(uiState.lastSyncTime) {
        if (uiState.lastSyncTime > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(uiState.lastSyncTime))
        } else {
            "从未同步"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 云端同步与 Supabase
            Text(
                text = "云端同步",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Supabase 凭据配置",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.supabaseUrl.isNotBlank()) "已配置: ${uiState.supabaseUrl.take(24)}..." else "未连接云端，当前为纯本地模式",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Button(
                            onClick = {
                                supabaseUrlInput = uiState.supabaseUrl
                                supabaseKeyInput = uiState.supabaseAnonKey
                                showSupabaseDialog = true
                            },
                            shape = com.example.inkpaperdiary.core.designsystem.CapsuleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("配置", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "立即双向同步",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "上次同步: $lastSyncStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.performManualSync() },
                            enabled = uiState.supabaseUrl.isNotBlank()
                        ) {
                            Icon(Icons.Outlined.Sync, contentDescription = "同步", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "自动后台同步",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "每 1 小时在联网时自动同步",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = uiState.autoSyncEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    SyncWorker.schedulePeriodicSync(context)
                                } else {
                                    SyncWorker.cancelPeriodicSync(context)
                                }
                                viewModel.setAutoSyncEnabled(enabled)
                            }
                        )
                    }
                }
            }

            // 安全与隐私
            Text(
                text = "安全与隐私保护",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "应用锁 (PIN 密码)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.appLockEnabled) "已启用 PIN 密码保护" else "关闭",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = uiState.appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinDialog = true
                                } else {
                                    viewModel.setAppLock(false, "")
                                }
                            }
                        )
                    }

                    if (uiState.appLockEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "指纹 / 面容快速解锁",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { viewModel.setBiometric(it) }
                            )
                        }
                    }
                }
            }

            // 信笺底纹样式
            Text(
                text = "书写信笺底纹",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.paperPattern == PaperPattern.BLANK,
                        onClick = { viewModel.setPaperPattern(PaperPattern.BLANK) },
                        shape = CapsuleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        label = { Text("纯净纸面", fontFamily = SansFontFamily) }
                    )
                    FilterChip(
                        selected = uiState.paperPattern == PaperPattern.RULED_LINES,
                        onClick = { viewModel.setPaperPattern(PaperPattern.RULED_LINES) },
                        shape = CapsuleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        label = { Text("横线便签", fontFamily = SansFontFamily) }
                    )
                    FilterChip(
                        selected = uiState.paperPattern == PaperPattern.DOTTED_GRID,
                        onClick = { viewModel.setPaperPattern(PaperPattern.DOTTED_GRID) },
                        shape = CapsuleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        label = { Text("手账点阵", fontFamily = SansFontFamily) }
                    )
                }
            }

            // 数据备份与归档
            Text(
                text = "数据管理与归档",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 导出 Markdown Zip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.exportMarkdownZip(context) { file ->
                                    if (file != null) {
                                        shareExportedFile(file)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导出 Markdown 压缩包", style = MaterialTheme.typography.bodyLarge)
                            Text("包含所有日记 .md 文件及本地配图", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // 导出 JSON 备份
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.exportJsonBackup(context) { file ->
                                    if (file != null) {
                                        shareExportedFile(file)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导出全量 JSON 备份", style = MaterialTheme.typography.bodyLarge)
                            Text("用于跨设备无损迁移或还原", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Outlined.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // 导入 JSON 备份
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppLockManager.isPickerActive = true
                                importJsonLauncher.launch(arrayOf("application/json"))
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导入 JSON 备份", style = MaterialTheme.typography.bodyLarge)
                            Text("从其他设备迁移日记数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // 导入 TXT 纯文本日记
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppLockManager.isPickerActive = true
                                importTxtLauncher.launch(arrayOf("text/plain", "*/*"))
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("导入 TXT 纯文本日记", style = MaterialTheme.typography.bodyLarge)
                            Text("支持批量导入单个或多篇日记，自动识别旧日期", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.Outlined.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // 回收站入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTrash() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("日记回收站", style = MaterialTheme.typography.bodyLarge)
                            Text("支持 30 天内恢复被误删的日记", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    // Supabase 凭据弹窗
    if (showSupabaseDialog) {
        AlertDialog(
            onDismissRequest = { showSupabaseDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("配置 Supabase 凭据", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = supabaseUrlInput,
                        onValueChange = { supabaseUrlInput = it },
                        label = { Text("Project URL", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily) },
                        placeholder = { Text("https://xxx.supabase.co", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supabaseKeyInput,
                        onValueChange = { supabaseKeyInput = it },
                        label = { Text("Anon Key (公钥)", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily) },
                        placeholder = { Text("eyJhbGciOi...", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveSupabaseConfig(supabaseUrlInput, supabaseKeyInput)
                        showSupabaseDialog = false
                    }
                ) {
                    Text("保存", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.testSupabaseConnection(supabaseUrlInput, supabaseKeyInput) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("测试连接", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    // PIN 设置弹窗
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("设置 4~6 位 PIN 密码", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pinInput = it },
                    placeholder = { Text("输入数字密码", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length >= 4) {
                            viewModel.setAppLock(true, pinInput)
                            showPinDialog = false
                            pinInput = ""
                        } else {
                            Toast.makeText(context, "密码至少需要 4 位数字", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("确认启用", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("取消", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}
