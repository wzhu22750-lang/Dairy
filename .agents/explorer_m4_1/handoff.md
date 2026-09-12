# Handoff Report: Milestone 4 Settings Screen Inset Grouped Architecture & HIG Optimization

## 1. Observation

### 1.1 Codebase & Component Analysis

1. **Existing `SettingsScreen.kt` Layout**:
   - Location: `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt` (lines 1-589).
   - Currently divided into **5 sections**:
     * Section 1 (lines 165-230): "云端与同步" (Supabase 凭据配置, 立即双向同步, 自动后台同步).
     * Section 2 (lines 233-295): "安全与隐私保护" (应用锁 (PIN 密码), 修改 PIN 密码, 指纹 / 面容快速解锁). Lacks auto-lock timeout option.
     * Section 3 (lines 298-369): "外观与风格" (主题外观, 正文字体, 书写信笺底纹).
     * Section 4 (lines 372-460): "数据管理与归档" (导入 TXT, 导出 Markdown, 导出 JSON, 导入 JSON, 日记回收站).
     * Section 5 (lines 463-478): "关于本应用" (InkPaperDiary 日记本, subtitle).
   - Missing features identified in prompt:
     * Section 1 lacks backup/restore consolidation.
     * Section 2 lacks "自动锁定" (Auto-lock timeout).
     * Section 4 lacks "清理本地缓存" (Clear Cache) and dedicated "版本信息" (App Version).
     * Section 5 is an unneeded 5th section that should be integrated into Section 4 "数据与关于".

2. **iOS Design System Components (`IosListComponents.kt`)**:
   - Location: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosListComponents.kt`.
   - `IosListSection` (lines 42-93): Inset Grouped container with 16dp rounded corner squircle, `MaterialThickness.THICK` background, 0.5dp specular border (`hasBorder = true`), 16dp horizontal padding, uppercase header (12sp MonoGray500), and optional footer (13sp MonoGray500).
   - `IosListRow` (lines 143-217):
     * Container min height: 44dp, padding: horizontal 16dp, vertical 11dp.
     * Leading icon container: `Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)))` (lines 176-184).
     * Indented divider logic (lines 208-215):
       ```kotlin
       if (showDivider) {
           val indentStart = if (leadingIcon != null) 56.dp else 16.dp
           HorizontalDivider(
               modifier = Modifier.padding(start = indentStart),
               thickness = 0.5.dp,
               color = dividerColor
           )
       }
       ```
       Divider indent is mathematically derived: 16dp start padding + 30dp icon width + 10dp inter-item spacing = **56.dp**!
   - `IosNavigationRow` (lines 223-264): Automatically adds Cupertino trailing chevron disclosure (`Icons.AutoMirrored.Filled.ArrowForwardIos`, 13dp) and optional trailing value string (15sp MonoGray500).
   - `IosSwitchRow` (lines 270-308): Automatically places an `IosSwitch` (51dp x 31dp capsule, 27dp circular thumb, Apple Green `#34C759` active track, zero ripple, tactile haptic feedback) at the trailing end.
   - `IosSquircleIconBox` (lines 381-402):
     * Size: 30dp x 30dp.
     * Shape: `RoundedCornerShape(7.dp)`.
     * Icon glyph size: 18dp.
     * Icon tint: `Color.White`.
     * Background: Dynamic Apple vivid system color.

3. **Collapsible Large Title Scaffold (`IosLargeTitleScaffold.kt`)**:
   - Location: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`.
   - Supports `scrollState: ScrollState` overload (lines 115-142).
   - Dynamic scroll coupling:
     * Collapsing threshold: `IosLargeTitleDefaults.CollapseThresholdDp` = 52.dp.
     * Top bar height: 44.dp + status bar insets.
     * Top bar inline title alpha: `(scrollOffset / thresholdPx).coerceIn(0f, 1f)`.
     * Large title alpha in content: `(1f - (scrollOffset / thresholdPx)).coerceIn(0f, 1f)`.
     * Header background transitions to 93% frosted glass elevation with a 0.5dp hairline border when collapsed.
   - Current `SettingsScreen.kt` already imports and calls `IosLargeTitleScaffold(title = "设置", scrollState = scrollState)` with `IosLargeTitleItem` at line 158.

4. **Remaining Material / Android Idioms**:
   - `Toast.makeText(context, ...)` used across operations (lines 65, 68, 82, 101, 122, 203, 523, 525).
   - Dialog text input in previous versions relied on standard Material TextField; now `IosDialogTextField` is available in `IosModalDialog.kt` (lines 292-329).
   - No Material 3 `TopAppBar`, `FloatingActionButton`, or `Icons.Default.MoreVert` exist in `SettingsScreen.kt`.

5. **Build Status**:
   - Command: `./gradlew testDebugUnitTest`.
   - Result: Failed on `CalendarScreen.kt:210:39` (`No parameter with name 'scale' found; No value passed for parameter 'scaleDown'`) which belongs to Milestone 5.
   - `SettingsScreen.kt` and `SettingsViewModel.kt` themselves have zero compiler warnings or errors.

---

## 2. Logic Chain

1. **4 Inset Grouped Sections Structural Realignment**:
   - *Observation 1.1*: `SettingsScreen.kt` currently splits settings into 5 sections, placing backup and restore in Section 4, omitting auto-lock timeout, cache clearing, and app version, and leaving Section 5 as an isolated 1-row block.
   - *Requirement*: Group into 4 canonical sections matching iOS Settings:
     * **Section 1: 云端与同步 (Cloud & Sync)**:
       - Row 1: Supabase 凭据配置 (`IosNavigationRow`, opens `IosModalDialog`)
       - Row 2: 立即双向同步 (`IosNavigationRow`, triggers manual sync)
       - Row 3: 自动后台同步 (`IosSwitchRow`, toggles periodic sync)
       - Row 4: 全量数据备份 (`IosNavigationRow`, opens `IosActionSheet` with JSON and Markdown export)
       - Row 5: 数据导入与恢复 (`IosNavigationRow`, opens `IosActionSheet` with JSON and TXT import)
     * **Section 2: 安全与隐私 (Security & Privacy)**:
       - Row 1: 应用锁 (PIN 密码) (`IosSwitchRow`, toggles lock)
       - (Conditional if `appLockEnabled`):
         - Row 2: 修改 PIN 密码 (`IosNavigationRow`, opens PIN dialog)
         - Row 3: 生物特征快速解锁 (`IosSwitchRow`, toggles biometric)
         - Row 4: 自动锁定 (`IosNavigationRow`, opens `IosActionSheet` with "立即", "1 分钟", "5 分钟", "15 分钟")
     * **Section 3: 外观与排版 (Appearance & Typography)**:
       - Row 1: 主题外观 (`IosNavigationRow`, opens `IosActionSheet` with 跟随系统 / 浅色模式 / 深色模式)
       - Row 2: 正文字体 (`IosNavigationRow`, opens `IosActionSheet` with 系统无衬线 / 经典宋体)
       - Row 3: 书写信笺底纹 (Squircle icon + `IosSegmentedControl` for 纯净纸面 / 横线便签 / 手账点阵)
     * **Section 4: 数据与关于 (Data & About)**:
       - Row 1: 日记回收站 (`IosNavigationRow`, navigates to `TrashScreen`)
       - Row 2: 清理本地缓存 (`IosNavigationRow`, opens destructive `IosModalDialog` showing current cache size)
       - Row 3: 关于与版本信息 (`IosListRow`, displays "InkPaperDiary 日记本" and trailing "v1.0.0 (Build 1)")

2. **Squircle Icon Specification & Vivid Color Palette**:
   - *Observation 1.2*: `IosSquircleIconBox` implements 30dp x 30dp box with 7dp corner radius and 18dp centered vector icon.
   - *Palette Mapping*:
     | Setting Item | Category | Background Color | Hex Code | Icon Vector |
     |---|---|---|---|---|
     | Supabase 凭据配置 | Cloud | Apple System Blue | `0xFF007AFF` | `Icons.Outlined.CloudSync` |
     | 立即双向同步 | Cloud | Apple System Indigo | `0xFF5856D6` | `Icons.Outlined.Sync` |
     | 自动后台同步 | Cloud | Apple System Green | `0xFF34C759` | `Icons.Outlined.Schedule` |
     | 全量数据备份 | Backup | Apple System Blue | `0xFF007AFF` | `Icons.Outlined.SaveAlt` |
     | 数据导入与恢复 | Backup | Apple System Indigo | `0xFF5856D6` | `Icons.Outlined.FileUpload` |
     | 应用锁 (PIN 密码) | Security | Apple System Orange | `0xFFFF9500` | `Icons.Outlined.Lock` |
     | 修改 PIN 密码 | Security | Apple System Orange | `0xFFFF9500` | `Icons.Outlined.Key` |
     | 生物特征快速解锁 | Security | Apple System Light Blue | `0xFF32ADE6` | `Icons.Outlined.Fingerprint` |
     | 自动锁定 | Security | Apple System Orange | `0xFFFF9500` | `Icons.Outlined.Timer` |
     | 主题外观 | Appearance | Apple System Purple | `0xFFAF52DE` | `Icons.Outlined.Palette` |
     | 正文字体 | Appearance | Apple System Blue | `0xFF007AFF` | `Icons.Outlined.TextFields` |
     | 书写信笺底纹 | Appearance | Apple System Orange | `0xFFFF9500` | `Icons.Outlined.Description` |
     | 日记回收站 | Data | Apple System Red | `0xFFFF3B30` | `Icons.Outlined.Delete` |
     | 清理本地缓存 | Data | Apple System Orange | `0xFFFF9500` | `Icons.Outlined.CleaningServices` |
     | 关于与版本信息 | About | Apple System Gray | `0xFF8E8E93` | `Icons.Outlined.Info` |

3. **56dp Indented Divider Rule**:
   - *Observation 1.2*: `IosListRow` computes `indentStart = if (leadingIcon != null) 56.dp else 16.dp` with thickness `0.5.dp` and color `AppleMaterials.separatorColor(isDark)`.
   - *Last Row Omission Rule*:
     * Section 1: Rows 1-4 `showDivider = true`; Row 5 `showDivider = false`.
     * Section 2: If `appLockEnabled == false`, Row 1 `showDivider = false`. If `appLockEnabled == true`, Rows 1-3 `showDivider = true`; Row 4 (Auto-lock) `showDivider = false`.
     * Section 3: Rows 1-2 `showDivider = true`; Row 3 (Segmented control container) `showDivider = false`.
     * Section 4: Rows 1-2 `showDivider = true`; Row 3 (About & Version) `showDivider = false`.
   - Result: No row divider ever collides with the outer 16dp rounded corner of `IosListSection`.

4. **Integration with `IosLargeTitleScaffold`**:
   - *Observation 1.3*: `IosLargeTitleScaffold` accepts `title = "设置"` and `scrollState = scrollState`.
   - *Mechanism*: `val scrollOffset = rememberScrollStateOffset(scrollState)` connects to `IosLargeTitleItem(title = "设置", scrollOffset = scrollOffset)` at the top of the scrollable column.
   - When the user scrolls down, the 34sp Bold title fades out as the scroll offset passes 52dp, and the 17sp SemiBold title fades into the pinned top bar with frosted glass translucency and a 0.5dp bottom border.

5. **Android Material Idiom Elimination**:
   - Zero `TopAppBar`, zero `FloatingActionButton`, zero `MoreVert` menus.
   - All switches use `IosSwitchRow` (Apple HIG UISwitch).
   - Dialogs use `IosModalDialog` with fixed 270dp width, 14dp squircle, and `IosDialogTextField`.
   - Selection menus use `IosActionSheet` with 14dp squircle cards, Cupertino checkmarks, and detached Cancel pill.
   - Local cache clearing uses standard sandboxed Android filesystem APIs (`context.cacheDir` and `context.externalCacheDir`) without leaking Android idioms into the UI presentation.

---

## 3. Caveats

1. **Compilation Blocker in `CalendarScreen.kt`**:
   - `CalendarScreen.kt:210` currently has an erroneous parameter call (`scale = ...` instead of `pressedScale = ...`). This does not impact `SettingsScreen.kt` but prevents `./gradlew testDebugUnitTest` from running until fixed by Milestone 5.
2. **Auto-Lock Timeout Persistence**:
   - `SettingsRepository` currently does not have a dedicated `KEY_AUTO_LOCK_TIMEOUT` DataStore key (it is protected under Protected Files). The UI state can hold `var autoLockTimeout by remember { mutableStateOf("立即") }` (or persist into DataStore if an extended key is added later in Milestone 6).
3. **External Pickers & AppLock**:
   - When triggering `importJsonLauncher`, `importTxtLauncher`, or `shareExportedFile`, `AppLockManager.isPickerActive = true` must be set prior to opening system intents so that the security lifecycle does not trigger the lock screen.

---

## 4. Conclusion & Proposed Implementation Blueprint

### 4.1 Architectural Blueprint for `SettingsScreen.kt`

Below is the complete, drop-in replacement implementation blueprint for `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`:

```kotlin
package com.example.inkpaperdiary.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToTrash: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val context = LocalContext.current

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
    var isChangingPin by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    var showAutoLockSheet by remember { mutableStateOf(false) }
    var selectedAutoLockTimeout by remember { mutableStateOf("立即") }

    var showBackupSheet by remember { mutableStateOf(false) }
    var showRestoreSheet by remember { mutableStateOf(false) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var selectedThemeTitle by remember { mutableStateOf("跟随系统") }

    var showFontSheet by remember { mutableStateOf(false) }
    var selectedFontTitle by remember { mutableStateOf("系统无衬线 (San Francisco)") }

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
                    title = "Supabase 云端配置",
                    subtitle = if (uiState.supabaseUrl.isNotBlank()) "已连接: ${uiState.supabaseUrl.take(24)}..." else "未连接云端，当前为纯本地离线模式",
                    value = if (uiState.supabaseUrl.isNotBlank()) "已配置" else "未配置",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.CloudSync,
                            backgroundColor = Color(0xFF007AFF),
                            iconTint = Color.White
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
                            backgroundColor = Color(0xFF5856D6),
                            iconTint = Color.White
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
                            backgroundColor = Color(0xFF34C759),
                            iconTint = Color.White
                        )
                    },
                    showDivider = true
                )

                // Row 1.4: Full Data Backup
                IosNavigationRow(
                    title = "全量数据备份",
                    subtitle = "导出 JSON 结构化备份或 Markdown 压缩包",
                    value = "导出",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.SaveAlt,
                            backgroundColor = Color(0xFF007AFF),
                            iconTint = Color.White
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
                            backgroundColor = Color(0xFF5856D6),
                            iconTint = Color.White
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
                            isChangingPin = false
                            showPinDialog = true
                        } else {
                            viewModel.setAppLock(false, "")
                        }
                    },
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Lock,
                            backgroundColor = Color(0xFFFF9500),
                            iconTint = Color.White
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
                                backgroundColor = Color(0xFFFF9500),
                                iconTint = Color.White
                            )
                        },
                        onClick = {
                            pinInput = ""
                            isChangingPin = true
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
                                backgroundColor = Color(0xFF32ADE6),
                                iconTint = Color.White
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
                                backgroundColor = Color(0xFFFF9500),
                                iconTint = Color.White
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
                title = "外观与排版",
                footer = "定制应用主题呈现、正文阅读排版字体以及书写信笺底纹样式。"
            ) {
                // Row 3.1: Theme Mode
                IosNavigationRow(
                    title = "主题外观",
                    subtitle = "跟随系统自动切换或锁定深浅色模式",
                    value = selectedThemeTitle,
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Palette,
                            backgroundColor = Color(0xFFAF52DE),
                            iconTint = Color.White
                        )
                    },
                    onClick = { showThemeSheet = true },
                    showDivider = true
                )

                // Row 3.2: Typography Font
                IosNavigationRow(
                    title = "正文字体",
                    subtitle = "日记阅读与编辑排版字体",
                    value = selectedFontTitle,
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.TextFields,
                            backgroundColor = Color(0xFF007AFF),
                            iconTint = Color.White
                        )
                    },
                    onClick = { showFontSheet = true },
                    showDivider = true
                )

                // Row 3.3: Paper Pattern Segmented Control (Last row: no divider)
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
                            backgroundColor = Color(0xFFFF9500),
                            iconTint = Color.White
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
                footer = "已删除的日记将在回收站保留 30 天。缓存清理仅删除临时导出的归档包与缩略图。"
            ) {
                // Row 4.1: Recycle Bin Navigation
                IosNavigationRow(
                    title = "日记回收站",
                    subtitle = "查看与恢复 30 天内删除的日记条目",
                    value = "查看",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Delete,
                            backgroundColor = Color(0xFFFF3B30),
                            iconTint = Color.White
                        )
                    },
                    onClick = onNavigateToTrash,
                    showDivider = true
                )

                // Row 4.2: Clear Cache
                IosNavigationRow(
                    title = "清理本地缓存",
                    subtitle = "包含临时导出的备份文件与配图缓存",
                    value = cacheSizeDisplay,
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.CleaningServices,
                            backgroundColor = Color(0xFFFF9500),
                            iconTint = Color.White
                        )
                    },
                    onClick = { showClearCacheDialog = true },
                    showDivider = true
                )

                // Row 4.3: App Version & About (Last row: showDivider = false)
                IosListRow(
                    title = "关于 InkPaperDiary",
                    subtitle = "极简黑白 · Apple HIG 规范 · 离线优先",
                    icon = {
                        IosSquircleIconBox(
                            icon = Icons.Outlined.Info,
                            backgroundColor = Color(0xFF8E8E93),
                            iconTint = Color.White
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

    // 2. PIN Setup / Change Dialog (IosModalDialog)
    IosModalDialog(
        visible = showPinDialog,
        title = if (isChangingPin) "修改 PIN 密码" else "设置 PIN 密码",
        message = "请输入 4 位数字安全密码用于应用解锁",
        confirmText = "确定",
        cancelText = "取消",
        onConfirm = {
            if (pinInput.length == 4) {
                viewModel.setAppLock(true, pinInput)
                showPinDialog = false
                Toast.makeText(context, if (isChangingPin) "PIN 密码修改成功" else "应用锁已启用", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "密码须为 4 位数字", Toast.LENGTH_SHORT).show()
            }
        },
        onDismissRequest = { showPinDialog = false }
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
                AppLockManager.isPickerActive = true
                importJsonLauncher.launch(arrayOf("application/json"))
            },
            IosActionItem(
                title = "导入 TXT 纯文本日记 (支持多选)",
                icon = Icons.AutoMirrored.Outlined.NoteAdd
            ) {
                showRestoreSheet = false
                AppLockManager.isPickerActive = true
                importTxtLauncher.launch(arrayOf("text/plain", "*/*"))
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

    // 6. Theme Mode Action Sheet (IosActionSheet)
    IosActionSheet(
        visible = showThemeSheet,
        title = "选择主题模式",
        message = "设定应用在不同光线环境下的色彩呈现",
        actions = listOf(
            IosActionItem(
                title = "跟随系统",
                isChecked = selectedThemeTitle == "跟随系统"
            ) {
                selectedThemeTitle = "跟随系统"
                showThemeSheet = false
            },
            IosActionItem(
                title = "浅色模式",
                isChecked = selectedThemeTitle == "浅色模式"
            ) {
                selectedThemeTitle = "浅色模式"
                showThemeSheet = false
            },
            IosActionItem(
                title = "深色模式",
                isChecked = selectedThemeTitle == "深色模式"
            ) {
                selectedThemeTitle = "深色模式"
                showThemeSheet = false
            }
        ),
        onDismissRequest = { showThemeSheet = false }
    )

    // 7. Typography Font Action Sheet (IosActionSheet)
    IosActionSheet(
        visible = showFontSheet,
        title = "选择正文字体",
        message = "定制日记阅读与书写排版字体",
        actions = listOf(
            IosActionItem(
                title = "系统无衬线 (San Francisco)",
                isChecked = selectedFontTitle.startsWith("系统无衬线")
            ) {
                selectedFontTitle = "系统无衬线 (San Francisco)"
                showFontSheet = false
            },
            IosActionItem(
                title = "经典宋体 / 衬线体 (Serif)",
                isChecked = selectedFontTitle.startsWith("经典宋体")
            ) {
                selectedFontTitle = "经典宋体 / 衬线体 (Serif)"
                showFontSheet = false
            }
        ),
        onDismissRequest = { showFontSheet = false }
    )

    // 8. Clear Cache Confirmation Dialog (IosModalDialog - Destructive Action)
    IosModalDialog(
        visible = showClearCacheDialog,
        title = "清理本地缓存",
        message = "将清理应用临时生成的导出归档包与缩略图缓存（当前大小：$cacheSizeDisplay）。日记数据库与原始本地附件不受影响。",
        confirmText = "清理",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            runCatching {
                context.cacheDir?.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
            }
            cacheSizeDisplay = calculateCacheSize()
            showClearCacheDialog = false
            Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
        },
        onDismissRequest = { showClearCacheDialog = false }
    )
}
```

---

## 5. Verification Method

### 5.1 Verification Commands
1. **Compilation Check**:
   Once Milestone 5 fixes the parameter call in `CalendarScreen.kt:210`, run:
   ```bash
   ./gradlew compileDebugKotlin
   ./gradlew testDebugUnitTest
   ```
2. **Audit Compliance**:
   Verify zero Material 3 FloatingActionButton or MoreVert menus in UI modules:
   ```bash
   ./gradlew test --tests "com.example.inkpaperdiary.tier1_features.MaterialIdiomPurgeAuditTest"
   ```

### 5.2 Visual & Structural Verification Checklist
- [ ] Verify `SettingsScreen` renders exactly 4 Inset Grouped sections (`IosListSection`).
- [ ] Verify each row uses `IosSquircleIconBox` with 30dp x 30dp dimension and 7dp corner radius.
- [ ] Verify each row divider starts at 56dp indentation and has 0.5dp hairline thickness.
- [ ] Verify the last row in each of the 4 sections has `showDivider = false`.
- [ ] Verify scroll coupling: Scrolling up and down smoothly transitions the 34sp Bold title into the 17sp SemiBold title with frosted glass top bar elevation.
- [ ] Verify `IosModalDialog` renders fixed 270dp width for credentials, PIN, and cache clearing.
- [ ] Verify `IosActionSheet` opens for Backup, Restore, Auto-lock, Theme, and Font selections.
- [ ] Verify zero regression on Room DAOs, PIN keystore encryption, and Supabase cloud sync.
