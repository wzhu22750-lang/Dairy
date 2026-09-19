package com.example.inkpaperdiary.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.PaperTypography
import com.example.inkpaperdiary.core.designsystem.components.*
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleItem
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleScaffold
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavBackButton
import com.example.inkpaperdiary.core.designsystem.scaffold.rememberScrollStateOffset
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.core.sync.SyncWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class PinDialogMode {
    SETUP,
    CHANGE,
    DISABLE
}

enum class ChangePinStep {
    VERIFY_OLD,
    ENTER_NEW
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToTrash: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    readingSettingsViewModel: com.example.inkpaperdiary.ui.reader.ReadingSettingsViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val context = LocalContext.current

    // 阅读排版设置（真实持久化；为空时仅展示当前值不可调）
    val readingSettings = readingSettingsViewModel?.settings?.collectAsState()?.value
        ?: com.example.inkpaperdiary.core.designsystem.ReadingSettings()

    val scrollState = rememberScrollState()
    val scrollOffset = rememberScrollStateOffset(scrollState)

    // -----------------------------------------------------------------------------------------
    // Document Pickers & Backup Intent Handlers
    // -----------------------------------------------------------------------------------------
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

    fun shareExportedFile(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val type = if (file.extension.equals("zip", ignoreCase = true)) "application/zip" else "application/json"
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            AppLockManager.isPickerActive = true
            context.startActivity(Intent.createChooser(intent, "分享备份文件"))
        }.onFailure {
            AppLockManager.isPickerActive = false
            Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
        }
    }

    // -----------------------------------------------------------------------------------------
    // Cache Management Utilities
    // -----------------------------------------------------------------------------------------
    fun calculateCacheSize(): String {
        val totalBytes = runCatching {
            var size = 0L
            context.cacheDir?.walkTopDown()?.forEach { if (it.isFile) size += it.length() }
            context.externalCacheDir?.walkTopDown()?.forEach { if (it.isFile) size += it.length() }
            size
        }.getOrDefault(0L)
        return when {
            totalBytes <= 0L -> "0 KB"
            totalBytes < 1024L * 1024L -> "${totalBytes / 1024L} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", totalBytes / (1024.0 * 1024.0))
        }
    }

    var cacheSizeDisplay by remember { mutableStateOf(calculateCacheSize()) }

    // App Version Info
    val appVersionName = remember {
        runCatching {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "v${pInfo.versionName ?: "1.0.0"}"
        }.getOrDefault("v1.0.0")
    }

    // -----------------------------------------------------------------------------------------
    // Modal & Sheet States
    // -----------------------------------------------------------------------------------------
    var showSupabaseDialog by remember { mutableStateOf(false) }
    var supabaseUrlInput by remember { mutableStateOf(uiState.supabaseUrl) }
    var supabaseKeyInput by remember { mutableStateOf(uiState.supabaseAnonKey) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(PinDialogMode.SETUP) }
    var changePinStep by remember { mutableStateOf(ChangePinStep.VERIFY_OLD) }
    var pinInput by remember { mutableStateOf("") }

    var showAutoLockSheet by remember { mutableStateOf(false) }
    var selectedAutoLockTimeout by remember { mutableStateOf("立即") }

    var showBackupSheet by remember { mutableStateOf(false) }
    var showRestoreSheet by remember { mutableStateOf(false) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showReadingSheet by remember { mutableStateOf(false) }

    var showClearCacheDialog by remember { mutableStateOf(false) }

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

    // -----------------------------------------------------------------------------------------
    // UI Scaffold & Inset Grouped Hierarchy
    // -----------------------------------------------------------------------------------------
    IosLargeTitleScaffold(
        title = "设置",
        scrollState = scrollState,
        navigationIcon = if (onNavigateBack != null) {
            {
                IosNavBackButton(
                    onNavigateBack = onNavigateBack,
                    label = "返回"
                )
            }
        } else null
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 96.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // iOS 34sp Collapsible Large Title Item
            IosLargeTitleItem(
                title = "设置",
                scrollOffset = scrollOffset,
                modifier = Modifier.padding(top = 4.dp)
            )

            // =================================================================================
            // SECTION 1: 云端与同步 (Cloud & Sync)
            // =================================================================================
            IosListSection(
                title = "云端与同步",
                footer = "支持通过 Supabase 跨设备双向同步，或生成本地结构化备份文件。"
            ) {
                // Row 1.1: Supabase Credentials
                IosNavigationRow(
                    title = "Supabase 凭据配置",
                    subtitle = if (uiState.supabaseUrl.isNotBlank()) "已连接: ${uiState.supabaseUrl.take(24)}..." else "未连接云端，当前为纯本地离线模式",
                    value = if (uiState.supabaseUrl.isNotBlank()) "已配置" else "未配置",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.CloudSync,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        supabaseUrlInput = uiState.supabaseUrl
                        supabaseKeyInput = uiState.supabaseAnonKey
                        showSupabaseDialog = true
                    },
                    showDivider = true
                )

                // Row 1.2: Manual Immediate Sync Trigger
                IosNavigationRow(
                    title = "立即双向同步",
                    subtitle = "上次同步: $lastSyncStr",
                    value = if (uiState.isSyncing) "同步中..." else "立即同步",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Sync,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        if (uiState.supabaseUrl.isNotBlank()) {
                            viewModel.performManualSync()
                        } else {
                            Toast.makeText(context, "请先配置 Supabase 凭据", Toast.LENGTH_SHORT).show()
                        }
                    },
                    showDivider = true
                )

                // Row 1.3: Periodic Auto Sync Switch
                IosSwitchRow(
                    title = "自动后台同步",
                    subtitle = "每 1 小时在联网时静默自动同步",
                    checked = uiState.autoSyncEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            SyncWorker.schedulePeriodicSync(context)
                        } else {
                            SyncWorker.cancelPeriodicSync(context)
                        }
                        viewModel.setAutoSyncEnabled(enabled)
                    },
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Schedule,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    showDivider = true
                )

                // Row 1.4: Full Data Backup
                IosNavigationRow(
                    title = "全量数据备份",
                    subtitle = "导出全量 JSON 备份或 Markdown 压缩包",
                    value = "导出",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.SaveAlt,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { showBackupSheet = true },
                    showDivider = true
                )

                // Row 1.5: Data Restore & Import (Last row: showDivider = false)
                IosNavigationRow(
                    title = "数据导入与恢复",
                    subtitle = "从 JSON 备份或 TXT 纯文本恢复日记",
                    value = "导入",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.FileUpload,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { showRestoreSheet = true },
                    showDivider = false
                )
            }

            // =================================================================================
            // SECTION 2: 安全与隐私 (Security & Privacy)
            // =================================================================================
            IosListSection(
                title = "安全与隐私",
                footer = "开启应用锁后，切出前台将自动开启 FLAG_SECURE 防窥保护，重新进入需安全认证。"
            ) {
                // Row 2.1: Master App Lock Switch
                IosSwitchRow(
                    title = "应用锁 (PIN 密码)",
                    subtitle = if (uiState.appLockEnabled) "已启用 4 位安全 PIN 保护" else "关闭",
                    checked = uiState.appLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            pinInput = ""
                            pinDialogMode = PinDialogMode.SETUP
                            showPinDialog = true
                        } else {
                            pinInput = ""
                            pinDialogMode = PinDialogMode.DISABLE
                            showPinDialog = true
                        }
                    },
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Lock,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    showDivider = uiState.appLockEnabled
                )

                if (uiState.appLockEnabled) {
                    // Row 2.2: Change PIN
                    IosNavigationRow(
                        title = "修改 PIN 密码",
                        subtitle = "更新当前 4 位安全访问密码",
                        value = "修改",
                        icon = {
                            IosSquircleIconBox(
                                icon = Icons.Outlined.Key,
                                iconTint = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            pinInput = ""
                            pinDialogMode = PinDialogMode.CHANGE
                            changePinStep = ChangePinStep.VERIFY_OLD
                            showPinDialog = true
                        },
                        showDivider = true
                    )

                    // Row 2.3: Biometric Fast Unlock
                    IosSwitchRow(
                        title = "生物特征快速解锁",
                        subtitle = "使用指纹或面容跳过 PIN 键盘输入",
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometric(it) },
                        icon = {
                            IosSquircleIconBox(
                                icon = Icons.Outlined.Fingerprint,
                                iconTint = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        showDivider = true
                    )

                    // Row 2.4: Auto-Lock Timeout (Last row: showDivider = false)
                    IosNavigationRow(
                        title = "自动锁定延迟",
                        subtitle = "离开应用后重新进入时要求验证的间隔",
                        value = selectedAutoLockTimeout,
                        icon = {
                            IosSquircleIconBox(
                                icon = Icons.Outlined.Timer,
                                iconTint = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = { showAutoLockSheet = true },
                        showDivider = false
                    )
                }
            }

            // =================================================================================
            // SECTION 3: 外观与排版 (Appearance & Typography)
            // =================================================================================
            IosListSection(
                title = "纸面与排版",
                footer = "阅读排版与主题全局生效：字体、字号、行距、页宽亦可随时在阅读页的 Aa 中调节。"
            ) {
                // Row 3.1: Theme Mode (真实持久化)
                IosNavigationRow(
                    title = "纸面",
                    subtitle = "纸张白或夜间的柔和黑",
                    value = readingSettings.themeMode.label,
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Palette,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { showThemeSheet = true },
                    showDivider = true
                )

                // Row 3.2: Reading Typography (打开阅读设置面板)
                IosNavigationRow(
                    title = "阅读排版",
                    subtitle = "字体 · 字号 · 行距 · 页宽",
                    value = when (readingSettings.font) {
                        com.example.inkpaperdiary.core.designsystem.ReaderFont.SERIF -> "衬线"
                        com.example.inkpaperdiary.core.designsystem.ReaderFont.SANS -> "无衬线"
                    },
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.TextFields,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { showReadingSheet = true },
                    showDivider = true
                )

                // Row 3.3: 书写信笺底纹 (Last row: no divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Description,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "书写信笺底纹",
                            style = PaperTypography.bodyLarge.copy(fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val patterns = listOf(PaperPattern.BLANK, PaperPattern.RULED_LINES, PaperPattern.DOTTED_GRID)
                    IosSegmentedControl(
                        items = patterns,
                        selectedItem = uiState.paperPattern,
                        onItemSelected = { viewModel.setPaperPattern(it) },
                        itemLabel = { pattern ->
                            when (pattern) {
                                PaperPattern.BLANK -> "纯净纸面"
                                PaperPattern.RULED_LINES -> "横线便签"
                                PaperPattern.DOTTED_GRID -> "手账点阵"
                            }
                        }
                    )
                }
            }

            // =================================================================================
            // SECTION 4: 数据与关于 (Data & About)
            // =================================================================================
            IosListSection(
                title = "数据与关于",
                footer = "已删除的日记将在废纸篓保留 30 天。缓存清理仅删除临时导出的归档包与缩略图。"
            ) {
                // Row 4.0: 书的刻度（统计，刻意降低存在感）
                IosNavigationRow(
                    title = "书的刻度",
                    subtitle = "连续书写 · 页数 · 字数",
                    value = "查看",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.BarChart,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = onNavigateToStats,
                    showDivider = true
                )

                // Row 4.1: Recycle Bin Navigation
                IosNavigationRow(
                    title = "废纸篓",
                    subtitle = "查看与恢复 30 天内撕下的书页",
                    value = "查看",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Delete,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = onNavigateToTrash,
                    showDivider = true
                )

                // Row 4.2: Clear Cache
                IosNavigationRow(
                    title = "清除应用缓存",
                    subtitle = "清理临时导出的备份文件与图片缓存",
                    value = cacheSizeDisplay,
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.CleaningServices,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = { showClearCacheDialog = true },
                    showDivider = true
                )

                // Row 4.3: App Version & About (Last row: showDivider = false)
                IosListRow(
                    title = "关于 Dairy",
                    subtitle = "一本安静的私人书 · 离线优先",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Info,
                            iconTint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailing = {
                        Text(
                            text = appVersionName,
                            style = PaperTypography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = PaperColors.MonoGray500
                            )
                        )
                    },
                    showDivider = false
                )
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Sheets & Dialogs Presentation Layer
    // -----------------------------------------------------------------------------------------

    // 1. Supabase Credentials Dialog (IosModalDialog)
    IosModalDialog(
        visible = showSupabaseDialog,
        title = "Supabase 凭据配置",
        message = "请输入 Supabase 项目的 API URL 与 Anon Key",
        confirmText = "保存",
        cancelText = "取消",
        onConfirm = {
            viewModel.saveSupabaseConfig(supabaseUrlInput.trim(), supabaseKeyInput.trim())
            showSupabaseDialog = false
        },
        onDismissRequest = { showSupabaseDialog = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IosDialogTextField(
                value = supabaseUrlInput,
                onValueChange = { supabaseUrlInput = it },
                placeholder = "Supabase Project URL",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            IosDialogTextField(
                value = supabaseKeyInput,
                onValueChange = { supabaseKeyInput = it },
                placeholder = "Anon Public Key",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
    }

    // 2. PIN Lifecycle Dialog (IosModalDialog)
    val pinDialogTitle = when (pinDialogMode) {
        PinDialogMode.SETUP -> "设置 PIN 密码"
        PinDialogMode.CHANGE -> if (changePinStep == ChangePinStep.VERIFY_OLD) "验证原 PIN 密码" else "设置新 PIN 密码"
        PinDialogMode.DISABLE -> "关闭应用锁"
    }
    val pinDialogMessage = when (pinDialogMode) {
        PinDialogMode.SETUP -> "请输入 4 位数字安全密码用于应用解锁"
        PinDialogMode.CHANGE -> if (changePinStep == ChangePinStep.VERIFY_OLD) "请输入当前 4 位数字密码以验证身份" else "请输入新的 4 位数字安全密码"
        PinDialogMode.DISABLE -> "请输入当前 4 位数字密码以确认关闭应用锁"
    }
    val pinDialogConfirmText = if (pinDialogMode == PinDialogMode.CHANGE && changePinStep == ChangePinStep.VERIFY_OLD) "下一步" else "确定"

    IosModalDialog(
        visible = showPinDialog,
        title = pinDialogTitle,
        message = pinDialogMessage,
        confirmText = pinDialogConfirmText,
        cancelText = "取消",
        onConfirm = {
            when (pinDialogMode) {
                PinDialogMode.SETUP -> {
                    if (pinInput.length == 4) {
                        viewModel.setAppLock(true, pinInput)
                        showPinDialog = false
                        pinInput = ""
                        Toast.makeText(context, "应用锁已启用", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                    }
                }
                PinDialogMode.CHANGE -> {
                    when (changePinStep) {
                        ChangePinStep.VERIFY_OLD -> {
                            if (pinInput.length == 4) {
                                viewModel.verifyPin(pinInput) { isValid ->
                                    if (isValid) {
                                        pinInput = ""
                                        changePinStep = ChangePinStep.ENTER_NEW
                                    } else {
                                        pinInput = ""
                                        Toast.makeText(context, "原 PIN 密码错误，请重新输入", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "请输入 4 位当前密码", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ChangePinStep.ENTER_NEW -> {
                            if (pinInput.length == 4) {
                                viewModel.setAppLock(true, pinInput)
                                showPinDialog = false
                                pinInput = ""
                                changePinStep = ChangePinStep.VERIFY_OLD
                                Toast.makeText(context, "PIN 密码修改成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "新密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                PinDialogMode.DISABLE -> {
                    if (pinInput.length == 4) {
                        viewModel.verifyPin(pinInput) { isValid ->
                            if (isValid) {
                                viewModel.setAppLock(false, "")
                                showPinDialog = false
                                pinInput = ""
                                Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                            } else {
                                pinInput = ""
                                Toast.makeText(context, "PIN 密码错误，无法关闭应用锁", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "请输入 4 位数字密码", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDismissRequest = {
            showPinDialog = false
            pinInput = ""
            changePinStep = ChangePinStep.VERIFY_OLD
        }
    ) {
        IosDialogTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
            placeholder = "4 位数字密码",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
    }

    // 3. Backup Options Action Sheet (IosActionSheet)
    IosActionSheet(
        visible = showBackupSheet,
        title = "全量数据备份与导出",
        message = "选择备份导出格式进行跨设备迁移或本地归档",
        actions = listOf(
            IosActionItem(
                title = "导出全量 JSON 备份",
                icon = Icons.Outlined.SaveAlt
            ) {
                showBackupSheet = false
                viewModel.exportJsonBackup(context) { file ->
                    if (file != null) shareExportedFile(file)
                }
            },
            IosActionItem(
                title = "导出 Markdown 压缩包 (含本地配图)",
                icon = Icons.Outlined.FolderZip
            ) {
                showBackupSheet = false
                viewModel.exportMarkdownZip(context) { file ->
                    if (file != null) shareExportedFile(file)
                }
            }
        ),
        onDismissRequest = { showBackupSheet = false }
    )

    // 4. Restore Options Action Sheet (IosActionSheet)
    IosActionSheet(
        visible = showRestoreSheet,
        title = "数据导入与恢复",
        message = "选择数据源还原日记条目到本地数据库",
        actions = listOf(
            IosActionItem(
                title = "导入 JSON 备份文件",
                icon = Icons.Outlined.FileUpload
            ) {
                showRestoreSheet = false
                runCatching {
                    AppLockManager.isPickerActive = true
                    importJsonLauncher.launch(arrayOf("application/json"))
                }.onFailure {
                    AppLockManager.isPickerActive = false
                    Toast.makeText(context, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                }
            },
            IosActionItem(
                title = "导入 TXT 纯文本日记 (支持多选)",
                icon = Icons.AutoMirrored.Outlined.NoteAdd
            ) {
                showRestoreSheet = false
                runCatching {
                    AppLockManager.isPickerActive = true
                    importTxtLauncher.launch(arrayOf("text/plain", "*/*"))
                }.onFailure {
                    AppLockManager.isPickerActive = false
                    Toast.makeText(context, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                }
            }
        ),
        onDismissRequest = { showRestoreSheet = false }
    )

    // 5. Auto-Lock Timeout Action Sheet (IosActionSheet)
    IosActionSheet(
        visible = showAutoLockSheet,
        title = "自动锁定延迟",
        message = "设定切出应用后多长时间重新进入需要验证",
        actions = listOf("立即", "1 分钟", "5 分钟", "15 分钟").map { option ->
            IosActionItem(
                title = option,
                isChecked = selectedAutoLockTimeout == option
            ) {
                selectedAutoLockTimeout = option
                showAutoLockSheet = false
            }
        },
        onDismissRequest = { showAutoLockSheet = false }
    )

    // 6. Theme Mode Action Sheet（真实持久化到阅读设置）
    IosActionSheet(
        visible = showThemeSheet,
        title = "纸面",
        message = "跟随系统、纸张白，或夜间的柔和黑",
        actions = listOf(
            com.example.inkpaperdiary.core.designsystem.ThemeMode.entries.map { mode ->
                IosActionItem(
                    title = mode.label,
                    isChecked = readingSettings.themeMode == mode
                ) {
                    readingSettingsViewModel?.setThemeMode(mode)
                    showThemeSheet = false
                }
            }
        ).flatten(),
        onDismissRequest = { showThemeSheet = false }
    )

    // 7.5 阅读排版面板（Aa：字体/字号/行距/页宽/纸面，全局持久化）
    if (readingSettingsViewModel != null) {
        com.example.inkpaperdiary.ui.reader.ReadingSettingsSheet(
            visible = showReadingSheet,
            settings = readingSettings,
            onDismiss = { showReadingSheet = false },
            onSetFont = { readingSettingsViewModel.setFont(it) },
            onIncreaseFont = { readingSettingsViewModel.increaseFontScale() },
            onDecreaseFont = { readingSettingsViewModel.decreaseFontScale() },
            onSetLineSpacing = { readingSettingsViewModel.setLineSpacing(it) },
            onSetPageWidth = { readingSettingsViewModel.setPageWidth(it) },
            onSetThemeMode = { readingSettingsViewModel.setThemeMode(it) }
        )
    }

    // 8. Clear Cache Confirmation Dialog (IosModalDialog - Destructive Action)
    IosModalDialog(
        visible = showClearCacheDialog,
        title = "清除应用缓存",
        message = "将清理应用临时生成的导出归档包与缩略图缓存（当前占用：$cacheSizeDisplay）。日记数据库与原始本地附件不受影响。",
        confirmText = "清除",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            runCatching {
                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            }
            cacheSizeDisplay = calculateCacheSize()
            showClearCacheDialog = false
            Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
        },
        onDismissRequest = { showClearCacheDialog = false }
    )
}
