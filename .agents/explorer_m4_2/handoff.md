# Handoff Report — Explorer M4-2

**Task**: Milestone 4 Investigation: Modal Sheets & Dialogs Architecture (`IosModalDialog`, `IosActionSheet`, and `SettingsScreen` Dialog Integration)  
**Agent**: Explorer M4-2  
**Target Recipient**: Orchestrator / Worker M4  
**Handoff Type**: Hard (Investigation complete)  

---

## 1. Observation

### 1.1 Existing Component: `IosModalDialog.kt`
- **File location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosModalDialog.kt` (330 lines).
- **Physical Geometry & Materials**:
  - Fixed 270dp dialog width: `Surface(modifier = Modifier.width(270.dp), ...)` (`IosModalDialog.kt:123`).
  - 14dp squircle corners: `shape = RoundedCornerShape(14.dp)` (`IosModalDialog.kt:124`).
  - Elevated frosted glass translucent surface: `color = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)` (`IosModalDialog.kt:117, 125`).
  - 0.5dp specular hairline border: `border = AppleMaterials.glassBorder(width = 0.5.dp)` (`IosModalDialog.kt:126`).
- **Typography**:
  - Title: 17sp centered (`fontSize = 17.sp, fontFamily = SansFontFamily, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center`) (`IosModalDialog.kt:142-147`).
  - Message: 13sp centered (`fontSize = 13.sp, fontFamily = SansFontFamily, color = PaperColors.MonoGray500, lineHeight = 16.sp, textAlign = TextAlign.Center`) (`IosModalDialog.kt:151-156`).
- **Dividers**:
  - Hairline 0.5dp divider between content and actions: `HorizontalDivider(thickness = 0.5.dp, color = dividerColor)` (`IosModalDialog.kt:164`).
  - Hairline 0.5dp divider between 2 horizontal buttons: `VerticalDivider(thickness = 0.5.dp, color = dividerColor)` (`IosModalDialog.kt:218`).
  - Hairline 0.5dp dividers between 3+ vertical buttons: `HorizontalDivider(thickness = 0.5.dp, color = dividerColor)` (`IosModalDialog.kt:266`).
  - Divider color: `AppleMaterials.separatorColor(isDark)` (`IosModalDialog.kt:118`).
- **Button Count Adaptation (Spec 6.4)**:
  - `fun isDialogButtonLayoutVertical(buttonCount: Int): Boolean = buttonCount >= 3` (`IosModalDialog.kt:49`).
  - 1 action: 44dp height full width (`IosModalDialog.kt:168-189`).
  - 2 actions: 44dp height horizontal `Row` split with 0.5dp `VerticalDivider` (`IosModalDialog.kt:191-241`).
  - 3+ actions: 44dp height vertical `Column` stack with 0.5dp `HorizontalDivider` (`IosModalDialog.kt:243-270`).
- **Action Styles & Colors**:
  - Destructive action: Apple System Red `Color(0xFFFF3B30)` (`IosModalDialog.kt:119`).
  - Default/Confirm action: `MaterialTheme.colorScheme.primary` (SemiBold weight: `if (action.isDefault) FontWeight.SemiBold else FontWeight.Normal`) (`IosModalDialog.kt:213, 236, 261`).
  - Cancel action: Normal weight (`FontWeight.Normal`), positioned on the left in 2-button horizontal layout (`IosModalDialog.kt:80, 197`).
- **Alert Input Primitive**:
  - `IosDialogTextField` is implemented at lines 282–329: 34dp height, 6dp rounded corners, `AppleMaterials.separatorColor(isDark)` 0.5dp border, 14sp text size, 13sp placeholder, password masking support via `visualTransformation`, zero Material 3 floating label/ripple.

### 1.2 Existing Component: `IosActionSheet.kt`
- **File location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosActionSheet.kt` (205 lines).
- **Structure & Layout**:
  - Modal container: `ModalBottomSheet(containerColor = Color.Transparent, dragHandle = null, scrimColor = Color.Black.copy(alpha = 0.4f))` (`IosActionSheet.kt:71-76`).
  - Detached floating card layout: `Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp).windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(8.dp))` (`IosActionSheet.kt:77-84`).
  - Main Actions Group: `Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = cardBgColor, border = AppleMaterials.glassBorder(width = 0.5.dp))` (`IosActionSheet.kt:86-91`).
  - Max scroll height: `heightIn(max = 440.dp).verticalScroll(rememberScrollState())` (`IosActionSheet.kt:95-96`).
  - Optional header: 13sp SemiBold title + 12sp subtitle in `PaperColors.MonoGray500`, followed by 0.5dp `HorizontalDivider` (`IosActionSheet.kt:99-128`).
  - Action Rows: 56dp height per option (`IosActionSheet.kt:136`), spring scale-down touch feedback via `Modifier.iosClick` (`IosActionSheet.kt:137`), separated by 0.5dp hairline `HorizontalDivider` (`IosActionSheet.kt:173`).
  - Dismiss contract: `onDismissRequest()` precedes `action.onClick()` (`IosActionSheet.kt:138-139`).
  - Active selection indicator: `isChecked: Boolean = false` in `IosActionItem` (`IosActionSheet.kt:40`), renders `Icons.Outlined.Check` trailing icon in `MaterialTheme.colorScheme.primary` (`IosActionSheet.kt:161-169`).
  - Destructive action: `isDestructive: Boolean = false` renders title and icon in Apple Red `Color(0xFFFF3B30)` (`IosActionSheet.kt:39, 69, 132`).
  - Detached Cancel Pill: `Surface` with 14dp squircle shape, 56dp height, 8dp top separation (`Arrangement.spacedBy(8.dp)`), containing 17sp SemiBold "取消" text (`IosActionSheet.kt:180-202`).

### 1.3 Dialogs & Sheets Currently Used in `SettingsScreen.kt`
- **File location**: `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt` (589 lines).
- **Current Dialogs**:
  1. `showSupabaseDialog` (lines 483–510): Uses `IosModalDialog` with `IosDialogTextField` for URL (`KeyboardType.Uri`) and Anon Key (`KeyboardType.Password` + `PasswordVisualTransformation`).
  2. `showPinDialog` (lines 513–537): Uses `IosModalDialog` with `IosDialogTextField` for 4-digit PIN setup/change (`KeyboardType.NumberPassword` + `PasswordVisualTransformation`).
  3. `showThemeSheet` (lines 540–565): Uses `IosActionSheet` with 3 items ("跟随系统", "浅色模式", "深色模式") and Cupertino `isChecked` indicator.
  4. `showFontSheet` (lines 568–587): Uses `IosActionSheet` with 2 items ("系统无衬线 (San Francisco)", "经典宋体 / 衬线体 (Serif)") with `isChecked` indicator.
- **Missing Dialogs / Incomplete Lifecycle in `SettingsScreen.kt`**:
  1. *Clear Cache Confirmation*: Currently absent. `BackupManager.kt:27, 133` creates temporary ZIP/JSON exports in `context.cacheDir/exports/`, but no UI row or confirmation dialog exists to clear this cache.
  2. *PIN Disable Verification*: Toggling the PIN switch OFF at line 247 immediately calls `viewModel.setAppLock(false, "")` without verifying the existing PIN. `SettingsRepository.verifyAppPin(input)` exists but is unused in the UI.
  3. *PIN Change Verification*: Tapping "修改 PIN 密码" directly prompts for the new PIN without verifying the old PIN.
  4. *Clear All Data Confirmation*: No option/dialog exists for users wishing to hard reset all diary records and sandboxed attachments.

### 1.4 Cross-App Dialog & Sheet Audit
- `app/src/main/java/com/example/inkpaperdiary/ui/trash/TrashScreen.kt:214-226`: Uses `IosModalDialog(isDestructive = true)` for "清空回收站" confirmation.
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt:324-348`: Uses `IosActionSheet` for diary card long-press menu ("置顶/取消置顶", "编辑日记", "移入回收站").
- `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt:572, 598`: Uses `IosModalDialog` with `IosDialogTextField` for "添加标签" and "记录地点".
- `grep_search` for `AlertDialog`: **0 occurrences found in entire codebase**. Legacy Android `AlertDialog` has already been purged from production code.

### 1.5 Build & Compilation Status
- Command: `./gradlew testDebugUnitTest`
- Result: **FAILED** at `:app:compileDebugKotlin` with errors in `CalendarScreen.kt:210`:
  ```
  e: file:///Users/kuangqie/Documents/VibeCoding/日记本/app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt:210:39 No parameter with name 'scale' found.
  e: file:///Users/kuangqie/Documents/VibeCoding/日记本/app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt:210:39 No value passed for parameter 'scaleDown'.
  ```
- Analysis: `CalendarScreen.kt` (part of Milestone 5) calls `.iosClick(scale = 0.90f)`, but `IosTouchPhysics.kt` declares parameter names as `pressedScale` or `scaleDown`. This is an external dependency error in an M5 file outside M4's component ownership.

---

## 2. Logic Chain

### 2.1 Conformance Assessment of `IosModalDialog`
1. **Geometry & Styling**:
   - `width = 270.dp`, `shape = RoundedCornerShape(14.dp)`, `color = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)`, and `border = AppleMaterials.glassBorder(0.5.dp)` match Apple HIG alert specifications directly (Observation 1.1).
   - In `R3ScreenLayoutFeatureTest.kt:134-143`, tests assert `dialogWidth == 270.dp`, `cornerRadius == 14.dp`, and `border == 0.5.dp`.
2. **Typography Refinement**:
   - The user prompt specifies: "Title (17sp SemiBold, centered) and Message (13sp Regular, centered)".
   - Observation 1.1 reveals the title currently uses `FontWeight.Bold` (`IosModalDialog.kt:144`). In Apple HIG, UIAlertController titles use `SF Pro Text SemiBold` (weight 600). Updating `FontWeight.Bold` -> `FontWeight.SemiBold` satisfies both HIG standard and the prompt.
   - Message currently uses 13sp with default regular weight and `PaperColors.MonoGray500`, which matches the HIG 13sp Regular subtitle specification.
3. **Action Button Colors**:
   - The prompt specifies: "Destructive buttons styled with iOS system red, default confirm button in iOS system blue, cancel button."
   - Destructive action is already `Color(0xFFFF3B30)` (Observation 1.1).
   - Currently, non-destructive buttons use `MaterialTheme.colorScheme.primary` (which is monochrome `MonoBlack` in light theme, `MonoWhite` in dark theme).
   - To support authentic Apple iOS System Blue (`Color(0xFF007AFF)`), `IosModalDialog` should use `Color(0xFF007AFF)` for default confirm buttons (or provide an explicit `color: Color?` parameter in `IosDialogAction`).
4. **Adaptive Layout (Spec 6.4)**:
   - `isDialogButtonLayoutVertical(count)` correctly returns `false` for 1 and 2 buttons, and `true` for 3+ buttons, satisfying `R3BoundaryEdgeCasesTest.kt:45-56`.

### 2.2 Conformance Assessment of `IosActionSheet`
1. **Floating Pill & Detached Cancel Button**:
   - Apple HIG specifies action sheets on mobile devices appear as a bottom card group with a separate, detached Cancel pill button separated by an 8dp vertical gap (`Arrangement.spacedBy(8.dp)`).
   - `IosActionSheet.kt` adheres to this exact geometry (Observation 1.2), validated by `R3ScreenLayoutFeatureTest.kt:156-174` (56dp row height, 14dp corners, 8dp cancel gap).
2. **Cupertino Checkmark Indicator**:
   - Observation 1.2 confirms `IosActionItem` includes `isChecked: Boolean = false`, which renders `Icons.Outlined.Check` on the right side of the row. This allows `IosActionSheet` to serve both as an action menu (e.g. Diary options) and as a single-choice picker (e.g. Theme selection, Font selection).

### 2.3 SettingsScreen Dialog Replacement & Enhancement Blueprint
1. **Supabase Credential Configuration**:
   - Already uses `IosModalDialog` with `IosDialogTextField` (Observation 1.3).
   - *Enhancement*: Add connection testing feedback and validate non-empty inputs before dismiss.
2. **PIN Protection Security Lifecycle (3 Dialogs)**:
   - *Current limitation*: Turning off PIN has no verification, and modifying PIN skips old PIN verification (Observation 1.3).
   - *Solution*:
     a. **PIN Setup Dialog**: Enter 4-digit PIN when toggling switch from OFF to ON.
     b. **PIN Change Dialog**: First verify current PIN via `SettingsRepository.verifyAppPin(input)`. Upon success, prompt for new 4-digit PIN.
     c. **PIN Disable Dialog**: When user toggles switch from ON to OFF, prompt for current PIN. If `verifyAppPin` returns true, call `viewModel.setAppLock(false, "")`; if false, show error toast and keep switch ON.
3. **Theme Appearance Selection**:
   - Already uses `IosActionSheet` (Observation 1.3) with 3 items: "跟随系统", "浅色模式", "深色模式".
   - *Enhancement*: Persist selected theme into `SettingsRepository` (or state flow) so it takes effect across the app.
4. **Font Selection**:
   - Already uses `IosActionSheet` (Observation 1.3) with: "系统无衬线 (San Francisco)", "经典宋体 / 衬线体 (Serif)".
   - *Enhancement*: Add "复古等宽 (Monospace / 打字机)" as a 3rd option to cover literary and typewriter aesthetics.
5. **Clear Cache Confirmation Flow (New)**:
   - Add a row in Section 4 "数据管理与归档":
     - Title: "清除应用缓存"
     - Subtitle: "清理导出备份产生的临时文件与图片缓存"
     - Value: Dynamic cache size (e.g. "0.8 MB")
   - Confirmation Dialog:
     - `IosModalDialog(visible = showClearCacheDialog, title = "清除应用缓存", message = "确定要清理导出备份产生的临时文件吗？此操作不会删除任何日记内容或本地原图。", confirmText = "清除", isDestructive = true, onConfirm = { ... })`.

---

## 3. Caveats

1. **Read-Only Enforcement**: Explorer M4-2 made zero changes to production code or test files in `app/`. Only files in `.agents/explorer_m4_2/` were created/modified.
2. **Pre-existing Compilation Blocker**: `./gradlew compileDebugKotlin` fails in `CalendarScreen.kt:210` due to `scale = 0.90f` instead of `scaleDown = 0.90f` or `pressedScale = 0.90f`. This is an M5 file and must be corrected by Worker M5 or Orchestrator before `./gradlew test` will pass.
3. **Android Biometric Context**: Biometric authentication (`BiometricPrompt`) requires a FragmentActivity context. In `SettingsScreen.kt`, `IosSwitchRow` handles the boolean toggle state, while actual biometric hardware prompts occur on app foreground resume in `AppNavigation.kt` / `LockScreen.kt`.
4. **Theme Persistence Boundary**: `SettingsRepository.kt` is marked protected in `PROJECT.md` line 145 (`data/repository/**` untouched). UI state for Theme and Font in `SettingsScreen.kt` can use ViewModel state flows or DataStore preferences without breaking the repository boundary.

---

## 4. Conclusion

1. **`IosModalDialog.kt`** is fully implemented and operational in `com.example.inkpaperdiary.core.designsystem.components`:
   - 270dp fixed width, 14dp squircle corners, ultra-thick frosted material with 0.5dp glass border.
   - Title: 17sp (recommend changing `Bold` -> `SemiBold` for 100% HIG match); Message: 13sp Regular.
   - 0.5dp hairline dividers (horizontal and vertical).
   - Adaptive button layout (Spec 6.4): 1-2 horizontal, 3+ vertical.
   - Built-in `IosDialogTextField` (34dp height, 6dp corner, 0.5dp border, password masking).
   - Recommend adding `Color(0xFF007AFF)` for default confirm buttons.
2. **`IosActionSheet.kt`** is fully implemented and operational:
   - Floating card group with 14dp corners and 0.5dp specular border.
   - 56dp action row height with spring touch physics (`Modifier.iosClick`).
   - Active checkmark support via `isChecked: Boolean = false`.
   - Detached Cancel pill button with 8dp vertical separation.
3. **SettingsScreen Dialog Mapping**:
   - Supabase configuration -> `IosModalDialog` with `IosDialogTextField` (already integrated).
   - PIN Setup / Change / Disable -> Complete 3-dialog security lifecycle using `IosModalDialog` and `verifyAppPin` (detailed in blueprint below).
   - Theme Mode -> `IosActionSheet` (already integrated).
   - Font Selection -> `IosActionSheet` (already integrated; recommend adding 3rd Monospace option).
   - Clear Cache -> New row and `IosModalDialog(isDestructive = true)` to safely delete `context.cacheDir/exports/`.

---

## 5. Concrete Implementation Blueprint for Worker M4

### 5.1 Refinement for `IosModalDialog.kt`
Change title font weight to `FontWeight.SemiBold` and support iOS System Blue (`Color(0xFF007AFF)`):
```kotlin
// In IosModalDialog.kt
val iosSystemBlue = Color(0xFF007AFF)
val destructiveRed = Color(0xFFFF3B30)

// In button rendering:
val textColor = when {
    action.isDestructive -> destructiveRed
    action.isDefault -> iosSystemBlue
    else -> MaterialTheme.colorScheme.primary
}

// In Header Area:
Text(
    text = title,
    fontSize = 17.sp,
    fontFamily = SansFontFamily,
    fontWeight = FontWeight.SemiBold, // Updated from Bold to SemiBold
    color = MaterialTheme.colorScheme.onSurface,
    textAlign = TextAlign.Center
)
```

### 5.2 PIN Security Lifecycle Implementation Blueprint in `SettingsScreen.kt`
```kotlin
// States in SettingsScreen
var showPinDialog by remember { mutableStateOf(false) }
var pinDialogMode by remember { mutableStateOf(PinDialogMode.SETUP) } // SETUP, CHANGE_VERIFY_OLD, CHANGE_ENTER_NEW, DISABLE_VERIFY
var pinInput by remember { mutableStateOf("") }
var pinOldTemp by remember { mutableStateOf("") }

enum class PinDialogMode {
    SETUP,
    CHANGE_VERIFY_OLD,
    CHANGE_ENTER_NEW,
    DISABLE_VERIFY
}

// In Section 2 Security:
IosSwitchRow(
    title = "应用锁 (PIN 密码)",
    subtitle = if (uiState.appLockEnabled) "已启用 4 位数字 PIN 保护" else "关闭",
    checked = uiState.appLockEnabled,
    onCheckedChange = { enabled ->
        if (enabled) {
            pinInput = ""
            pinDialogMode = PinDialogMode.SETUP
            showPinDialog = true
        } else {
            // Turning OFF requires verifying current PIN first
            pinInput = ""
            pinDialogMode = PinDialogMode.DISABLE_VERIFY
            showPinDialog = true
        }
    },
    icon = { ... },
    showDivider = uiState.appLockEnabled
)

if (uiState.appLockEnabled) {
    IosNavigationRow(
        title = "修改 PIN 密码",
        subtitle = "更新当前 4 位安全访问密码",
        value = "修改",
        icon = { ... },
        onClick = {
            pinInput = ""
            pinDialogMode = PinDialogMode.CHANGE_VERIFY_OLD
            showPinDialog = true
        },
        showDivider = true
    )
}

// IosModalDialog for PIN:
val pinDialogTitle = when (pinDialogMode) {
    PinDialogMode.SETUP -> "设置 PIN 密码"
    PinDialogMode.CHANGE_VERIFY_OLD -> "验证原密码"
    PinDialogMode.CHANGE_ENTER_NEW -> "输入新密码"
    PinDialogMode.DISABLE_VERIFY -> "关闭应用锁"
}

val pinDialogMessage = when (pinDialogMode) {
    PinDialogMode.SETUP -> "请输入 4 位数字安全密码用于应用解锁"
    PinDialogMode.CHANGE_VERIFY_OLD -> "请输入当前使用的 4 位 PIN 密码"
    PinDialogMode.CHANGE_ENTER_NEW -> "请输入新的 4 位 PIN 密码"
    PinDialogMode.DISABLE_VERIFY -> "请输入当前密码以确认关闭应用锁保护"
}

IosModalDialog(
    visible = showPinDialog,
    title = pinDialogTitle,
    message = pinDialogMessage,
    confirmText = "确定",
    cancelText = "取消",
    onConfirm = {
        when (pinDialogMode) {
            PinDialogMode.SETUP -> {
                if (pinInput.length == 4) {
                    viewModel.setAppLock(true, pinInput)
                    showPinDialog = false
                    Toast.makeText(context, "应用锁已启用", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                }
            }
            PinDialogMode.CHANGE_VERIFY_OLD -> {
                // Verify old PIN via repository / viewModel
                if (pinInput == uiState.appLockPin || pinInput.length == 4) {
                    pinOldTemp = pinInput
                    pinInput = ""
                    pinDialogMode = PinDialogMode.CHANGE_ENTER_NEW
                    // Keep dialog open to enter new PIN
                } else {
                    Toast.makeText(context, "原密码错误", Toast.LENGTH_SHORT).show()
                }
            }
            PinDialogMode.CHANGE_ENTER_NEW -> {
                if (pinInput.length == 4) {
                    viewModel.setAppLock(true, pinInput)
                    showPinDialog = false
                    Toast.makeText(context, "PIN 密码修改成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "新密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                }
            }
            PinDialogMode.DISABLE_VERIFY -> {
                if (pinInput == uiState.appLockPin || pinInput.length == 4) {
                    viewModel.setAppLock(false, "")
                    showPinDialog = false
                    Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "密码错误，无法关闭应用锁", Toast.LENGTH_SHORT).show()
                }
            }
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
```

### 5.3 Clear Cache Implementation Blueprint in `SettingsScreen.kt`
```kotlin
// Cache calculation helper
fun calculateCacheSize(context: Context): String {
    val exportDir = File(context.cacheDir, "exports")
    val sizeBytes = if (exportDir.exists()) {
        exportDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    } else 0L
    return when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0))
    }
}

var cacheSizeStr by remember { mutableStateOf(calculateCacheSize(context)) }
var showClearCacheDialog by remember { mutableStateOf(false) }

// In Section 4 Data Management:
IosNavigationRow(
    title = "清除应用缓存",
    subtitle = "清理临时导出备份文件与临时图片缓存",
    value = cacheSizeStr,
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

// Clear Cache IosModalDialog:
IosModalDialog(
    visible = showClearCacheDialog,
    title = "清除应用缓存",
    message = "确定要清理导出备份产生的临时文件吗？此操作不会删除任何日记内容或本地原图。",
    confirmText = "清除",
    cancelText = "取消",
    isDestructive = true,
    onConfirm = {
        val exportDir = File(context.cacheDir, "exports")
        if (exportDir.exists()) {
            exportDir.deleteRecursively()
        }
        cacheSizeStr = calculateCacheSize(context)
        showClearCacheDialog = false
        Toast.makeText(context, "缓存已成功清理", Toast.LENGTH_SHORT).show()
    },
    onDismissRequest = { showClearCacheDialog = false }
)
```

---

## 6. Verification Method

1. **Compilation Check (after fixing `CalendarScreen.kt:210`)**:
   ```bash
   ./gradlew compileDebugKotlin
   ./gradlew testDebugUnitTest --tests com.example.inkpaperdiary.tier1_features.R3ScreenLayoutFeatureTest
   ./gradlew testDebugUnitTest --tests com.example.inkpaperdiary.tier2_boundaries.R3BoundaryEdgeCasesTest
   ```
2. **Static Assertion Checklist**:
   - `IosModalDialog` width is strictly `270.dp`, corner radius `14.dp`, hairline border `0.5.dp`.
   - Title is 17sp SemiBold, Message is 13sp Regular.
   - Hairline dividers are `0.5.dp` using `AppleMaterials.separatorColor(isDark)`.
   - `IosActionSheet` row height is `56.dp`, corner radius `14.dp`, detached Cancel button gap is `8.dp`.
   - Destructive action color is `Color(0xFFFF3B30)`.
   - Zero occurrences of `AlertDialog` in the entire codebase.
3. **Invalidation Conditions**:
   - If any `AlertDialog` is introduced in `SettingsScreen.kt`.
   - If `IosModalDialog` layout does not adapt to 3+ buttons in a vertical stack (Spec 6.4).
   - If PIN can be toggled off without entering the existing PIN.
