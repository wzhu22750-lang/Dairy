# Comprehensive Investigation Report: SettingsScreen ViewModel Bindings, Domain Preservation, and Test Invariants

**Agent:** Explorer M4-3 (Gen 2)  
**Milestone:** Milestone 4 (Settings Screen & Modal Sheets/Dialogs)  
**Date:** 2026-09-06  
**Status:** Completed & Verified  

---

## 1. Executive Summary

This report establishes the authoritative architectural blueprint for Milestone 4 regarding:
1. **ViewModel State Bindings:** Mapping all interactive elements in `SettingsScreen` to `SettingsViewModel`, preserving reactive unidirectional data flow (UDF).
2. **Security Lifecycle Safeguarding (`isPickerActive`):** Guaranteeing that system document pickers (`OpenDocument`, `OpenMultipleDocuments`) and system share sheets (`Intent.createChooser`) never falsely trigger `AppLockManager` background lockout.
3. **100% Non-UI Domain Preservation:** Absolute preservation of Room database entities, DAOs, repositories, `PinCipher` (AES-GCM Keystore), and Supabase synchronization pipelines.
4. **Test Suite Invariants & Compliance:** Full alignment with contracts defined in `TEST_INFRA.md`, `tier1_features` (F10, F15, F16, F17), `tier2_boundaries` (B3, B4), `tier3_combinations` (Pair 3, Pair 6), and `tier4_scenarios` (Scenario 3, Scenario 4).

Current baseline compilation and tests (`./gradlew test`) pass with **100% success (0 errors, 0 failures)**.

---

## 2. ViewModel State Flow Architecture & UI State Bindings

### 2.1 State Model (`SettingsUiState`)

`SettingsViewModel` manages settings through a unified, immutable UI state:

```kotlin
data class SettingsUiState(
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricEnabled: Boolean = false,
    val paperPattern: PaperPattern = PaperPattern.BLANK,
    val autoSyncEnabled: Boolean = false,
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val exportFile: File? = null
)
```

### 2.2 Unidirectional Data Flow (UDF) Combination

`SettingsViewModel` merges two persistent reactive streams from `SettingsRepository`:
1. `supabaseFlow`: Combines `supabaseUrl`, `supabaseAnonKey`, `autoSyncEnabled`, and `lastSyncTime`.
2. `securityFlow`: Combines `appLockEnabled`, `appLockPin`, `biometricEnabled`, and `paperPattern`.

These are merged into `val uiState: StateFlow<SettingsUiState>` via:
```kotlin
combine(supabaseFlow, securityFlow) { supa, sec ->
    SettingsUiState(...)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())
```

### 2.3 Ephemeral Messaging Channel

In addition to `uiState`, asynchronous sync progress and feedback messages are exposed via a dedicated StateFlow:
```kotlin
private val _syncMessage = MutableStateFlow<String?>(null)
val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

fun clearSyncMessage() {
    _syncMessage.value = null
}
```

In `SettingsScreen`, this must be observed with a `LaunchedEffect(syncMessage)`:
```kotlin
val syncMessage by viewModel.syncMessage.collectAsState()
LaunchedEffect(syncMessage) {
    if (syncMessage != null) {
        Toast.makeText(context, syncMessage, Toast.LENGTH_SHORT).show()
        viewModel.clearSyncMessage()
    }
}
```

---

## 3. Comprehensive Action-to-ViewModel Mapping Matrix

Every user action in `SettingsScreen` is catalogued below, along with its UI presentation, ViewModel call, side-effects, and required lifecycle flags:

| Section | UI Control & Label | UI Component Type | Target ViewModel / System Call | Lifecycle Flag (`isPickerActive`) | Notes / Behavioral Requirements |
|---|---|---|---|---|---|
| **1. 云端与同步** | Supabase 凭据配置 | `IosNavigationRow` | `viewModel.saveSupabaseConfig(url, anonKey)` | Not modified (Internal Compose Dialog) | Opens `IosModalDialog`. Pre-fills inputs. On confirm, trims strings before saving. |
| **1. 云端与同步** | 立即双向同步 | `IosNavigationRow` | `viewModel.performManualSync()` | Not modified | Checks `if (uiState.supabaseUrl.isNotBlank())`. If empty, prompts to configure credentials first. Runs sync on `Dispatchers.IO`. |
| **1. 云端与同步** | 自动后台同步 | `IosSwitchRow` | `viewModel.setAutoSyncEnabled(enabled)` + `SyncWorker.schedulePeriodicSync(context)` / `cancelPeriodicSync(context)` | Not modified | If enabled: schedules 1-hr periodic WorkManager job. If disabled: cancels periodic work. Persists boolean in DataStore. |
| **2. 安全与隐私保护** | 应用锁 (PIN 密码) | `IosSwitchRow` | If enabling: triggers PIN Dialog -> `viewModel.setAppLock(true, pin)`<br>If disabling: `viewModel.setAppLock(false, "")` | Not modified (Internal Dialog) | When user toggles ON, do NOT enable until valid 4-digit PIN is confirmed via `IosModalDialog`. Plaintext PIN is never stored directly. |
| **2. 安全与隐私保护** | 修改 PIN 密码 | `IosNavigationRow` | Opens PIN Dialog -> `viewModel.setAppLock(true, newPin)` | Not modified | Only visible when `uiState.appLockEnabled == true`. Validates that PIN is exactly 4 digits. |
| **2. 安全与隐私保护** | 指纹 / 面容快速解锁 | `IosSwitchRow` | `viewModel.setBiometric(enabled)` | Not modified | Only visible when `uiState.appLockEnabled == true`. Sets preference for biometric prompt on `LockScreen`. |
| **3. 书写信笺底纹** | 纯净纸面 / 横线便签 / 手账点阵 | `IosSegmentedControl` | `viewModel.setPaperPattern(pattern)` | Not modified | Direct selection of `PaperPattern` enum (`BLANK`, `RULED_LINES`, `DOTTED_GRID`). |
| **4. 数据管理与归档** | 日记回收站 | `IosNavigationRow` | `onNavigateToTrash()` callback | Not modified | Navigates to `AppDestination.Trash`. |
| **4. 数据管理与归档** | 导出 Markdown 压缩包 | `IosNavigationRow` | `viewModel.exportMarkdownZip(context) { file -> shareExportedFile(file) }` | **`isPickerActive = true`** before `startActivity(createChooser)` | Generates `.zip` with `.md` files and images in `cacheDir/exports`. External share sheet requires `isPickerActive = true`. |
| **4. 数据管理与归档** | 导出全量 JSON 备份 | `IosNavigationRow` | `viewModel.exportJsonBackup(context) { file -> shareExportedFile(file) }` | **`isPickerActive = true`** before `startActivity(createChooser)` | Generates full `.json` backup. External share sheet requires `isPickerActive = true`. |
| **4. 数据管理与归档** | 导入 JSON 备份 | `IosNavigationRow` | `importJsonLauncher.launch(arrayOf("application/json"))`<br>Callback: `viewModel.importJsonBackup(content)` | **`isPickerActive = true`** before launch<br>**`isPickerActive = false`** in callback | Launches SAF `ActivityResultContracts.OpenDocument()`. Activity transitions to background (`onStop`). |
| **4. 数据管理与归档** | 导入 TXT 纯文本日记 | `IosNavigationRow` | `importTxtLauncher.launch(arrayOf("text/plain", "*/*"))`<br>Callback: `viewModel.importTxtFiles(context, uris)` | **`isPickerActive = true`** before launch<br>**`isPickerActive = false`** in callback | Launches SAF `ActivityResultContracts.OpenMultipleDocuments()`. Batch imports diaries using intelligent date/mood parsing. |
| **5. 关于本应用** | InkPaperDiary 日记本 | `IosListRow` | Static information display | Not modified | Shows version, HIG compliance, offline-first notes. |

---

## 4. `AppLockManager.isPickerActive` Lifecycle Hazard & Invariants

### 4.1 The Lifecycle Hazard

In `MainActivity.kt`:
```kotlin
override fun onStop() {
    super.onStop()
    // 只要退出了界面（按 Home、切换应用、息屏），且不是启动系统级图片/文件选择器，下一次进入应用即刻重新触发锁屏
    if (isLockEnabled && !AppLockManager.isPickerActive) {
        AppLockManager.lock()
    }
}

override fun onResume() {
    super.onResume()
    // 恢复前台后重置外部选择器状态
    AppLockManager.isPickerActive = false
}
```

When an external activity or system intent is launched:
1. SAF document picker (`OpenDocument`, `OpenMultipleDocuments`)
2. Android Sharesheet / Intent chooser (`Intent.ACTION_SEND` via `Intent.createChooser`)

The host activity (`MainActivity`) enters the background: `onPause()` -> `onStop()` is invoked.
- **If `isPickerActive == false`:** `MainActivity.onStop()` immediately triggers `AppLockManager.lock()`. When the user selects a file or dismisses the share sheet, the app resumes with `isLocked == true`, throwing up `LockScreen`! The user is locked out, and the import completion callback is obstructed.
- **If `isPickerActive == true`:** `MainActivity.onStop()` bypasses `AppLockManager.lock()`. The app remains unlocked. Once the user returns to the app, `MainActivity.onResume()` resets `isPickerActive = false`. If the user now presses the Home button, `isPickerActive` is `false`, and the app locks securely.

### 4.2 Exact Implementation Code for Worker M4

Worker M4 **must** implement the picker launchers and share helper with the exact invariant pattern:

```kotlin
// 1. JSON Backup Import Launcher
val importJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    // INVARIANT: Reset flag immediately upon callback receipt
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

// 2. TXT Plain Text Diary Import Launcher
val importTxtLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    // INVARIANT: Reset flag immediately upon callback receipt
    AppLockManager.isPickerActive = false
    if (uris.isNotEmpty()) {
        viewModel.importTxtFiles(context, uris) { count, failed ->
            val msg = if (failed == 0) "已成功导入 $count 篇日记" else "已导入 $count 篇日记 ($failed 个文件读取失败)"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}

// 3. System Share Sheet Helper
fun shareExportedFile(file: File) {
    // INVARIANT: Flag must be set to true BEFORE triggering external intent chooser
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
```

And in the UI rows:
```kotlin
// Import JSON Row Click
onClick = {
    AppLockManager.isPickerActive = true
    importJsonLauncher.launch(arrayOf("application/json"))
}

// Import TXT Row Click
onClick = {
    AppLockManager.isPickerActive = true
    importTxtLauncher.launch(arrayOf("text/plain", "*/*"))
}
```

### 4.3 Test Suite Verification of `isPickerActive`

This contract is verified across multiple tiers:
- **Tier 1 (`R4BusinessLogicFeatureTest`):**
  - `testF16_IsPickerActiveFlagContract`: Asserts flag transitions.
  - `testF16_PickerActivePreventsAppLockOnStop`: Simulates `onStop()` when `isPickerActive = true` and asserts `AppLockManager.isLocked.value == false`.
- **Tier 3 (`CrossFeaturePairwiseTest`):**
  - `testPair6_MediaPickerActivationPreventsAppLockLockoutDuringImport`: Validates interaction between import initiation, activity onStop, and subsequent lock state.
- **Tier 4 (`RealWorldApplicationScenariosTest`):**
  - `testScenario3_PrivacySecurityAndExternalMediaPickerLifecycleFlow`: Step-by-step E2E flow verifying lock suspension during external picker and re-locking upon genuine backgrounding.

---

## 5. 100% Domain & Data Preservation Audit

The non-UI domain layers must remain completely untouched. Our inspection confirms:

### 5.1 Room Database & DAOs (`com.example.inkpaperdiary.core.database.*`)
- **Entities:** `DiaryEntity`, `AttachmentEntity`, `TagEntity`, `DiaryTagCrossRef` contain all necessary indices (`entryDate`, `isDeleted`, `isPinned`, `syncStatus`). Default `syncStatus` is `1` (`DIRTY`).
- **DAOs:** `DiaryDao`, `TagDao`, `AttachmentDao` handle soft-deletion (`isDeleted = 1, syncStatus = 2`), restoring (`isDeleted = 0, syncStatus = 1`), and transactions.
- **Repositories:** `DiaryRepository`, `MediaRepository`, `SettingsRepository` operate transparently on `Dispatchers.IO`.
- **Verdict:** **Zero modifications required. 100% preserved.**

### 5.2 Security & PIN Cipher (`PinCipher.kt`)
- Uses Android Keystore AES-GCM 128-bit key (`KEY_ALIAS = "ink_paper_diary_pin_key"`).
- Ciphertext stored in DataStore with prefix `c1:<base64(iv)>:<base64(ciphertext)>`.
- `SettingsRepository.verifyAppPin`: Handles automatic migration of legacy plaintext PINs to AES-GCM ciphertext upon first successful verification.
- `SettingsRepository.setAppLock`: Strictly uses `PinCipher.encrypt(pin)`.
- **Verdict:** **Zero modifications required. 100% preserved.**

### 5.3 Cloud Sync & Backup Pipelines (`SyncManager.kt`, `BackupManager.kt`, `TxtDiaryImporter.kt`)
- `SyncManager.performSync()` executes a 3-stage bidirectional sync:
  1. Uploads dirty attachments via Supabase Storage.
  2. Upserts unsynced/deleted diaries to Supabase REST `diaries` endpoint.
  3. Pulls remote updates with ISO-8601 timestamps and merges with local priority on pending deletions.
- `BackupManager` executes lossless JSON serialization/deserialization and Markdown ZIP archiving with attached images.
- `TxtDiaryImporter` features dual-encoding parsing (UTF-8 with BOM and GB18030/GBK fallback), regex date inference, and multi-entry splitting.
- **Verdict:** **Zero modifications required. 100% preserved.**

---

## 6. Test Suite Invariants & HIG Compliance

### 6.1 Feature F10 Invariants (`tier1_features/R3ScreenLayoutFeatureTest.kt`)

The test suite explicitly codifies the canonical section structure:
```kotlin
@Test
fun testF10_FourCanonicalSettingsSections() {
    val expectedSections = listOf(
        "云端与同步",
        "安全与隐私保护",
        "书写信笺底纹",
        "数据管理与归档"
    )
    assertEquals(4, expectedSections.size)
    assertEquals("云端与同步", expectedSections[0])
    assertEquals("安全与隐私保护", expectedSections[1])
    assertEquals("书写信笺底纹", expectedSections[2])
    assertEquals("数据管理与归档", expectedSections[3])
}
```

Item contracts:
- **Cloud & Sync:** `listOf("Supabase 凭据配置", "立即双向同步", "自动后台同步")`
- **Security:** `listOf("应用锁 (PIN 密码)", "指纹 / 面容快速解锁")`
- **Paper Texture:** `listOf("纯净纸面", "横线便签", "手账点阵")` (or "纯净白纸")
- **Data Management:** `listOf("导出 Markdown 压缩包", "导出全量 JSON 备份", "导入 JSON 备份", "导入 TXT 纯文本日记", "日记回收站")`

### 6.2 Material 3 Idiom Purge Audit (`tier1_features/MaterialIdiomPurgeAuditTest.kt`)

- **Zero FABs:** No `FloatingActionButton` import or invocation anywhere in UI modules.
- **Zero 3-Dot Menus:** No `Icons.Default.MoreVert` in `SettingsScreen.kt`.
- **Touch Physics:** All rows and interactive elements must utilize `Modifier.iosClick` or `IosSwitch` / `IosSegmentedControl` with haptic feedback and spring physics, eliminating Android Material ripple effects.

### 6.3 Boundary & Edge Cases (`tier2_boundaries/R3BoundaryEdgeCasesTest.kt`)

- Subtitles with long URLs (e.g. Supabase Project URL) must enforce:
  `maxLines = 1, overflow = TextOverflow.Ellipsis`.
- Dialog buttons: 1 or 2 buttons render as a horizontal Row; 3+ buttons render as a vertical Column stack.

---

## 7. Drop-In Architecture Guidelines for Worker M4

Worker M4 should structure `SettingsScreen.kt` adhering to the following blueprint:

1. **Scaffold & Large Title Header:**
   - Use `IosLargeTitleTopBar(title = "设置", scrollOffset = scrollOffset, navigationIcon = onNavigateBack)` at the top of the box.
   - Use `IosLargeTitleItem(title = "设置")` at the top of the scrollable column.
2. **Four Inset Grouped Sections:**
   - Section 1: `title = "云端与同步"`, `footer = "支持通过 Supabase 提供跨设备端到端双向数据同步能力。"`
   - Section 2: `title = "安全与隐私保护"`, `footer = "开启应用锁后，离开前台会自动启用 FLAG_SECURE 防窥保护。"`
   - Section 3: `title = "书写信笺底纹"`, `footer = "在编辑日记与阅读时使用的衬底纸张纹理风格。"`
   - Section 4: `title = "数据管理与归档"`, `footer = "日记数据始终以纯本地 Room 数据库为源，所有附件文件存储于沙盒内。"`
   - Optional Section 5: `title = "关于本应用"`
3. **Squircle Category Icons:**
   - Container size: 30dp x 30dp
   - Corner shape: 7dp squircle radius (`RoundedCornerShape(7.dp)`)
   - Standard Apple HIG icon colors:
     - CloudSync: `#007AFF` (Blue)
     - Sync: `#5856D6` (Indigo)
     - Schedule: `#34C759` (Green)
     - Lock / PIN: `#FF9500` (Orange)
     - Fingerprint: `#32ADE6` (Teal)
     - Trash: `#FF3B30` (Red)
     - Markdown: `#AF52DE` (Purple)
     - JSON Export: `#007AFF` (Blue)
     - JSON Import: `#5856D6` (Indigo)
     - TXT Import: `#34C759` (Green)
     - About: `PaperColors.MonoGray600` (Gray)
4. **Dividers:**
   - `showDivider = true` between rows within each section (indented 56dp).
   - `showDivider = false` on the final row of each section.
5. **Modal Dialogs:**
   - Use `IosModalDialog` (270dp fixed width, 14dp squircle corners, hairline dividers) for Supabase credentials and PIN configuration, replacing any legacy Android dialogs.

---

## 8. Verification & Execution Commands

To verify domain and test invariants at any time:

```bash
# 1. Run all unit tests including feature, boundary, pairwise, and scenario tests:
./gradlew test

# 2. Run specific business logic and security tests:
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier1_features.R4BusinessLogicFeatureTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier2_boundaries.R4BoundaryEdgeCasesTest"

# 3. Run settings and layout feature tests:
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier1_features.R3ScreenLayoutFeatureTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier2_boundaries.R3BoundaryEdgeCasesTest"

# 4. Run cross-feature pairwise and E2E scenario tests:
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier3_combinations.CrossFeaturePairwiseTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier4_scenarios.RealWorldApplicationScenariosTest"

# 5. Run Android idiom purge audit:
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier1_features.MaterialIdiomPurgeAuditTest"

# 6. Verify compilation:
./gradlew assembleDebug
```
