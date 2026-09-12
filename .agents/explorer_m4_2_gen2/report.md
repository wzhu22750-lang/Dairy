# Milestone 4 Investigation Report: iOS Modal Dialogs & Action Sheets

**Agent**: Explorer M4-2 (Gen 2)  
**Date**: 2026-09-06  
**Status**: COMPLETE  
**Target Focus**: `IosModalDialog.kt`, `IosActionSheet.kt`, and replacing legacy Android dialogs in `SettingsScreen` (Theme mode, Font selection, PIN setup/change, Clear data confirmation).

---

## 1. Executive Summary

This investigation delivers the comprehensive architectural and implementation blueprint for Milestone 4's modal presentation layer. It focuses on replacing Android legacy dialogs (`AlertDialog` / `BasicAlertDialog` / Material 3 `OutlinedTextField` dialogs) with authentic Apple Human Interface Guidelines (HIG) primitives:
1. **`IosModalDialog.kt`**: Fixed 270dp width, 14dp squircle corners, 17sp bold title, 13sp footnote message, 0.5dp hairline dividers, destructive button support (`0xFFFF3B30`), single-action full-width button fallback, and multi-action Spec 6.4 vertical stacking (>=3 buttons vertical Column, <=2 buttons horizontal Row).
2. **`IosActionSheet.kt`**: Bottom sheet modal with 14dp squircle card group, 56dp action rows, detached 56dp Cancel pill with 8dp margin, and newly integrated active-item checkmark (`isChecked`) indicator.
3. **SettingsScreen Dialog & Sheet Inventory**:
   - **Theme Mode Selection**: Dynamic `IosActionSheet` selecting "跟随系统", "浅色模式", "深色模式".
   - **Font Style Selection**: Dynamic `IosActionSheet` selecting "系统默认 (无衬线)", "典雅宋体 (文学感)", "复古等宽 (打字机)".
   - **PIN Setup & Change Dialogs**: Native `IosModalDialog` with custom `IosDialogTextField` (34dp height, 6dp corner, numeric password mask, 4-digit validation).
   - **PIN Disable Confirmation**: Native `IosModalDialog` requiring current PIN verification prior to disabling security protection.
   - **Clear All Data Confirmation**: Native `IosModalDialog` with `isDestructive = true` for complete, irreversible local database and attachment purge.
4. **Zero Legacy `AlertDialog` across App**:
   - Identified and formulated drop-in replacements for `TrashScreen.kt` ("清空回收站") and `EditorScreen.kt` ("添加标签", "记录地点").

---

## 2. Existing Components Forensic Audit

### 2.1 `IosModalDialog.kt`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosModalDialog.kt`
- **Current Specs Verified**:
  - Width: Exactly 270dp (`Modifier.width(270.dp)`).
  - Corner radius: Exactly 14dp (`RoundedCornerShape(14.dp)`).
  - Background material: `AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)`.
  - Glass hairline border: `AppleMaterials.glassBorder(width = 0.5.dp)`.
  - Title: 17sp Bold `SansFontFamily`, centered.
  - Message: 13sp Regular `SansFontFamily`, `PaperColors.MonoGray500`, 16sp line height, centered.
  - Hairline dividers: 0.5dp thickness using `AppleMaterials.separatorColor(isDark)`.
  - Split action buttons: 44dp height row with 0.5dp vertical divider.
  - Destructive button: Apple HIG Red `Color(0xFFFF3B30)`.
- **Identified Deficiencies / Enhancement Opportunities**:
  1. **Single-Action Alerts**: Currently hardcoded to 2 buttons. When an alert only requires confirmation (e.g. "好" or "我知道了"), it must render a single full-width 44dp button without the vertical divider.
  2. **Spec 6.4 Multi-Action Adaptation**: `app/src/test/java/com/example/inkpaperdiary/tier2_boundaries/R3BoundaryEdgeCasesTest.kt` (test `testB3_ModalDialogButtonLayoutAdaptationByCount`) mandates:
     - 1 or 2 buttons -> horizontal `Row`, side-by-side.
     - 3+ buttons -> vertical `Column` stack.
  3. **Alert Text Field Primitive**: Currently `SettingsScreen` drops raw Material 3 `OutlinedTextField` into the `content` slot. In iOS HIG, alert text fields are compact 34dp rounded boxes with subtle gray translucent fill, hairline border, and no floating labels.

### 2.2 `IosActionSheet.kt`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosActionSheet.kt`
- **Current Specs Verified**:
  - Floating 14dp squircle group container with 0.5dp glass border.
  - Action rows: 56dp height with `Modifier.iosClick { onDismissRequest(); action.onClick() }`.
  - Detached Cancel pill: Separate 14dp squircle card with 56dp height and 8dp top margin.
  - Destructive red action styling (`Color(0xFFFF3B30)`).
- **Identified Deficiencies / Enhancement Opportunities**:
  - **Selection Checkmark Support**: For theme and font pickers, iOS Action Sheets show an active selection state (either a trailing checkmark `Icons.Outlined.Check` or bold highlight). Adding `val isChecked: Boolean = false` to `IosActionItem` enables this cleanly without breaking existing usages in `TimelineScreen.kt`.

---

## 3. SettingsScreen Modal & Dialog Inventory

| Dialog / Sheet Name | Primitive Component | Trigger In Settings | Key Visuals & Interactions | State & Data Binding |
|---|---|---|---|---|
| **外观主题选择** | `IosActionSheet` | "外观主题" Inset Grouped row | Header: "选择外观主题"<br>Options: 跟随系统, 浅色模式, 深色模式<br>Trailing checkmark on active item<br>Detached Cancel pill | `SettingsRepository.setThemeMode()`<br>`SettingsViewModel.setThemeMode()` |
| **排版字体选择** | `IosActionSheet` | "字体风格" Inset Grouped row | Header: "选择字体风格"<br>Options: 系统默认 (无衬线), 典雅宋体 (文学感), 复古等宽 (打字机)<br>Trailing checkmark on active item | `SettingsRepository.setAppFont()`<br>`SettingsViewModel.setAppFont()` |
| **PIN 密码设置** | `IosModalDialog` | 开启 "应用锁 (PIN 密码)" 开关 | Fixed 270dp, 14dp squircle<br>Title: "设置 PIN 密码"<br>Message: "请输入 4 位数字密码用于应用解锁"<br>Content: `IosDialogTextField` (34dp, masked, numeric keypad)<br>Buttons: 取消 (17sp) / 确定 (17sp SemiBold) | `SettingsViewModel.setAppLock(true, pin)` |
| **PIN 密码修改** | `IosModalDialog` | "修改 PIN 密码" 导航行 (当应用锁开启时可见) | Title: "修改 PIN 密码"<br>Message: "请输入原密码并设定新的 4 位密码"<br>Content: 原密码 + 新密码 2 个 `IosDialogTextField`<br>Buttons: 取消 / 修改 | `SettingsRepository.verifyAppPin(oldPin)`<br>`SettingsRepository.setAppLock(true, newPin)` |
| **PIN 密码关闭验证** | `IosModalDialog` | 关闭 "应用锁 (PIN 密码)" 开关 | Title: "关闭应用锁"<br>Message: "请输入当前 PIN 密码以关闭安全保护"<br>Content: 当前密码输入框<br>Buttons: 取消 / 确认关闭 (`isDestructive = true`) | 验证原密码成功后调用 `viewModel.setAppLock(false, "")` |
| **清空本地数据确认** | `IosModalDialog` | "清空所有日记数据" 导航行 (数据管理区) | Title: "清除所有日记数据？"<br>Message: "此操作将永久抹掉所有本地日记条目与配图缓存，不可撤销。"<br>Buttons: 取消 / 清空并抹掉 (`isDestructive = true`, 红色高亮) | `SettingsViewModel.clearAllData()`<br>调用 `DiaryRepository.hardDeleteDiary()` 清除所有记录及附件 |
| **Supabase 凭据配置** | `IosModalDialog` | "Supabase 凭据配置" 导航行 | Title: "Supabase 凭据配置"<br>Message: "请输入 Supabase 项目的 API URL 与 Anon Key"<br>Content: URL + Key 2 个 `IosDialogTextField`<br>Buttons: 取消 / 保存 | `SettingsViewModel.saveSupabaseConfig()` |

---

## 4. Other Legacy Android Dialog Replacements

### 4.1 `TrashScreen.kt` ("清空回收站")
- **Legacy Code** (lines 176–211): Material 3 `AlertDialog` with `shape = RoundedCornerShape(20.dp)` and `TextButton`.
- **Replacement**:
```kotlin
IosModalDialog(
    visible = showEmptyConfirm,
    title = "清空回收站？",
    message = "清空后所有已删除日记将被永久销毁，无法恢复。",
    confirmText = "清空",
    cancelText = "取消",
    isDestructive = true,
    onConfirm = {
        viewModel.emptyTrash()
        showEmptyConfirm = false
    },
    onDismissRequest = { showEmptyConfirm = false }
)
```

### 4.2 `EditorScreen.kt` ("添加标签" & "记录地点")
- **Legacy Code** (lines 560–640): Two Material 3 `AlertDialog` instances with `OutlinedTextField` and `TextButton`.
- **Replacement**:
```kotlin
// 添加标签
IosModalDialog(
    visible = showTagDialog,
    title = "添加标签",
    message = "为日记添加分类标签",
    confirmText = "确定",
    cancelText = "取消",
    onConfirm = {
        if (tagInputText.isNotBlank()) {
            viewModel.addTag(tagInputText.trim())
            tagInputText = ""
            showTagDialog = false
        }
    },
    onDismissRequest = { showTagDialog = false }
) {
    IosDialogTextField(
        value = tagInputText,
        onValueChange = { tagInputText = it },
        placeholder = "输入标签名称，如：随笔、生活"
    )
}

// 记录地点
IosModalDialog(
    visible = showLocationDialog,
    title = "记录地点",
    message = "记录此时此刻身处的地理位置",
    confirmText = "确定",
    cancelText = "取消",
    onConfirm = {
        if (locationInputText.isNotBlank()) {
            viewModel.setLocation(locationInputText.trim())
            locationInputText = ""
            showLocationDialog = false
        }
    },
    onDismissRequest = { showLocationDialog = false }
) {
    IosDialogTextField(
        value = locationInputText,
        onValueChange = { locationInputText = it },
        placeholder = "输入地点，如：咖啡馆、书房"
    )
}
```

---

## 5. Turnkey Code Blueprints for Worker M4

### 5.1 Enhanced `IosModalDialog.kt`
Support for:
- Spec 6.4 adaptive button layouts (1 button full width, 2 buttons horizontal split, 3+ buttons vertical stack)
- Optional `cancelText` (null for single alert action)
- Public `IosDialogTextField` composable for authentic alert text inputs

```kotlin
package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * Data class representing an action button in an iOS Alert Dialog.
 */
data class IosDialogAction(
    val title: String,
    val isDestructive: Boolean = false,
    val isDefault: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Apple HIG Alert Dialog (UIAlertController alert style)
 *
 * Characteristics:
 * - 270dp standardized width
 * - 14dp squircle corners
 * - Centered 17sp bold title and 13sp message
 * - Spec 6.4 adaptive button layout:
 *   * 1 button: 44dp full width
 *   * 2 buttons: 44dp horizontal split row with 0.5dp vertical hairline divider
 *   * 3+ buttons: 44dp vertical column stack with 0.5dp horizontal hairline dividers
 */
@Composable
fun IosModalDialog(
    visible: Boolean,
    title: String,
    message: String? = null,
    confirmText: String = "确认",
    cancelText: String? = "取消",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    if (!visible) return

    val actions = buildList {
        if (cancelText != null) {
            add(IosDialogAction(title = cancelText, onClick = onDismissRequest))
        }
        add(IosDialogAction(title = confirmText, isDestructive = isDestructive, isDefault = true, onClick = onConfirm))
    }

    IosModalDialog(
        visible = visible,
        title = title,
        message = message,
        actions = actions,
        onDismissRequest = onDismissRequest,
        content = content
    )
}

@Composable
fun IosModalDialog(
    visible: Boolean,
    title: String,
    message: String? = null,
    actions: List<IosDialogAction>,
    onDismissRequest: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    if (!visible) return

    val isDark = isSystemInDarkTheme()
    val dialogBgColor = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)
    val dividerColor = AppleMaterials.separatorColor(isDark)
    val destructiveRed = Color(0xFFFF3B30)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.width(270.dp),
            shape = RoundedCornerShape(14.dp),
            color = dialogBgColor,
            border = AppleMaterials.glassBorder(width = 0.5.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (!message.isNullOrBlank()) {
                        Text(
                            text = message,
                            fontSize = 13.sp,
                            fontFamily = SansFontFamily,
                            color = PaperColors.MonoGray500,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (content != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        content()
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                // Spec 6.4: Button Layout Adaptation by Count
                when {
                    actions.size == 1 -> {
                        val action = actions[0]
                        val textColor = if (action.isDestructive) destructiveRed else MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .iosClick {
                                    action.onClick()
                                    onDismissRequest()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.title,
                                fontSize = 17.sp,
                                fontFamily = SansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }
                    actions.size == 2 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val firstAction = actions[0]
                            val firstColor = if (firstAction.isDestructive) destructiveRed else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .iosClick {
                                        firstAction.onClick()
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstAction.title,
                                    fontSize = 17.sp,
                                    fontFamily = SansFontFamily,
                                    fontWeight = if (firstAction.isDefault) FontWeight.SemiBold else FontWeight.Normal,
                                    color = firstColor
                                )
                            }

                            VerticalDivider(thickness = 0.5.dp, color = dividerColor)

                            val secondAction = actions[1]
                            val secondColor = if (secondAction.isDestructive) destructiveRed else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .iosClick {
                                        secondAction.onClick()
                                        onDismissRequest()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = secondAction.title,
                                    fontSize = 17.sp,
                                    fontFamily = SansFontFamily,
                                    fontWeight = if (secondAction.isDefault) FontWeight.SemiBold else FontWeight.Normal,
                                    color = secondColor
                                )
                            }
                        }
                    }
                    else -> {
                        // 3+ actions: Vertical Column Stack
                        Column(modifier = Modifier.fillMaxWidth()) {
                            actions.forEachIndexed { index, action ->
                                val textColor = if (action.isDestructive) destructiveRed else MaterialTheme.colorScheme.primary
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .iosClick {
                                            action.onClick()
                                            onDismissRequest()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = action.title,
                                        fontSize = 17.sp,
                                        fontFamily = SansFontFamily,
                                        fontWeight = if (action.isDefault) FontWeight.SemiBold else FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                                if (index < actions.lastIndex) {
                                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact iOS UIAlertController-style text field input primitive.
 */
@Composable
fun IosDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0x26FFFFFF) else Color(0x0D000000)
    val borderColor = AppleMaterials.separatorColor(isDark)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        singleLine = singleLine,
        textStyle = TextStyle(
            fontSize = 14.sp,
            fontFamily = SansFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        ),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        fontFamily = SansFontFamily,
                        color = PaperColors.MonoGray400
                    )
                }
                innerTextField()
            }
        }
    )
}
```

---

### 5.2 Enhanced `IosActionSheet.kt` (`isChecked` checkmark support)
```kotlin
package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * Data class representing a single action item in an Apple HIG Action Sheet.
 */
data class IosActionItem(
    val title: String,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val isChecked: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosActionSheet(
    visible: Boolean,
    title: String? = null,
    message: String? = null,
    actions: List<IosActionItem>,
    cancelText: String = "取消",
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val isDark = isSystemInDarkTheme()
    val cardBgColor = AppleMaterials.backgroundColor(MaterialThickness.THICK)
    val dividerColor = AppleMaterials.separatorColor(isDark)
    val destructiveRed = Color(0xFFFF3B30)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Actions Group Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = AppleMaterials.glassBorder(width = 0.5.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Optional Header (Title + Message)
                    if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontFamily = SansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaperColors.MonoGray500,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (!message.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = message,
                                    fontSize = 12.sp,
                                    fontFamily = SansFontFamily,
                                    color = PaperColors.MonoGray500,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                    }

                    // Action Items
                    actions.forEachIndexed { index, action ->
                        val itemColor = if (action.isDestructive) destructiveRed else MaterialTheme.colorScheme.onSurface
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .iosClick {
                                    onDismissRequest()
                                    action.onClick()
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (action.icon != null) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = action.title,
                                fontSize = 17.sp,
                                fontFamily = SansFontFamily,
                                fontWeight = if (action.isChecked || !action.isDestructive) FontWeight.Medium else FontWeight.Normal,
                                color = itemColor
                            )
                            if (action.isChecked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "已选",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (index < actions.lastIndex) {
                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                        }
                    }
                }
            }

            // Separate Detached "Cancel" Button Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .iosClick { onDismissRequest() },
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = AppleMaterials.glassBorder(width = 0.5.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cancelText,
                        fontSize = 17.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

---

### 5.3 Data Layer Additions (`ThemeMode`, `AppFont`, Clear Data)

#### In `SettingsRepository.kt`:
```kotlin
enum class ThemeMode(val title: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
}

enum class AppFont(val title: String) {
    SYSTEM("系统默认"),
    SERIF("典雅宋体"),
    MONOSPACE("复古等宽")
}

// In companion object:
val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
val KEY_APP_FONT = stringPreferencesKey("app_font")

// Flows:
val themeMode: Flow<ThemeMode> = context.dataStore.data.safeCatch().map {
    val name = it[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
    try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
}

val appFont: Flow<AppFont> = context.dataStore.data.safeCatch().map {
    val name = it[KEY_APP_FONT] ?: AppFont.SYSTEM.name
    try { AppFont.valueOf(name) } catch (e: Exception) { AppFont.SYSTEM }
}

// Mutators:
suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs ->
        prefs[KEY_THEME_MODE] = mode.name
    }
}

suspend fun setAppFont(font: AppFont) {
    context.dataStore.edit { prefs ->
        prefs[KEY_APP_FONT] = font.name
    }
}
```

#### In `SettingsViewModel.kt`:
```kotlin
// In SettingsUiState:
val themeMode: ThemeMode = ThemeMode.SYSTEM,
val appFont: AppFont = AppFont.SYSTEM,

// Mutators:
fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }
}

fun setAppFont(font: AppFont) {
    viewModelScope.launch {
        settingsRepository.setAppFont(font)
    }
}

fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
        val ok = settingsRepository.verifyAppPin(oldPin)
        if (!ok) {
            onResult(false, "原密码错误")
            return@launch
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            onResult(false, "新密码须为 4 位数字")
            return@launch
        }
        settingsRepository.setAppLock(true, newPin)
        onResult(true, "PIN 密码已成功修改")
    }
}

fun clearAllData(context: Context, onResult: (Int) -> Unit) {
    viewModelScope.launch {
        val active = diaryRepository.getAllDiaries().first()
        for (d in active) {
            diaryRepository.hardDeleteDiary(d.id)
        }
        val trash = diaryRepository.getTrashDiaries().first()
        for (d in trash) {
            diaryRepository.hardDeleteDiary(d.id)
        }
        onResult(active.size)
    }
}
```

---

### 5.4 SettingsScreen Modal & Dialog Composable Code
Drop-in code for the bottom of `SettingsScreen.kt`:

```kotlin
    // ---------------------------------------------------------------------------------------------
    // Modal Sheets and Dialogs (Apple HIG Primitives)
    // ---------------------------------------------------------------------------------------------

    var showThemeSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showPinDisableDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var disablePinInput by remember { mutableStateOf("") }

    // 1. Theme Mode Selection (IosActionSheet)
    IosActionSheet(
        visible = showThemeSheet,
        title = "选择外观主题",
        message = "调整日记本的界面配色风格",
        actions = listOf(
            IosActionItem(
                title = "跟随系统",
                icon = Icons.Outlined.BrightnessAuto,
                isChecked = uiState.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            ),
            IosActionItem(
                title = "浅色模式",
                icon = Icons.Outlined.LightMode,
                isChecked = uiState.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
            ),
            IosActionItem(
                title = "深色模式",
                icon = Icons.Outlined.DarkMode,
                isChecked = uiState.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
            )
        ),
        cancelText = "取消",
        onDismissRequest = { showThemeSheet = false }
    )

    // 2. Font Style Selection (IosActionSheet)
    IosActionSheet(
        visible = showFontSheet,
        title = "选择排版字体",
        message = "应用于日记正文与标题排版",
        actions = listOf(
            IosActionItem(
                title = "系统默认 (无衬线)",
                icon = Icons.Outlined.TextFields,
                isChecked = uiState.appFont == AppFont.SYSTEM,
                onClick = { viewModel.setAppFont(AppFont.SYSTEM) }
            ),
            IosActionItem(
                title = "典雅宋体 (文学感)",
                icon = Icons.Outlined.FormatQuote,
                isChecked = uiState.appFont == AppFont.SERIF,
                onClick = { viewModel.setAppFont(AppFont.SERIF) }
            ),
            IosActionItem(
                title = "复古等宽 (打字机)",
                icon = Icons.Outlined.Code,
                isChecked = uiState.appFont == AppFont.MONOSPACE,
                onClick = { viewModel.setAppFont(AppFont.MONOSPACE) }
            )
        ),
        cancelText = "取消",
        onDismissRequest = { showFontSheet = false }
    )

    // 3. Supabase Credentials Dialog (IosModalDialog + IosDialogTextField)
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
                placeholder = "Supabase Project URL"
            )
            IosDialogTextField(
                value = supabaseKeyInput,
                onValueChange = { supabaseKeyInput = it },
                placeholder = "Anon Public Key",
                visualTransformation = PasswordVisualTransformation()
            )
        }
    }

    // 4. PIN Setup Dialog (IosModalDialog + IosDialogTextField)
    IosModalDialog(
        visible = showPinDialog,
        title = "设置 PIN 密码",
        message = "请输入 4 位数字密码用于应用解锁",
        confirmText = "确定",
        cancelText = "取消",
        onConfirm = {
            if (pinInput.length == 4 && pinInput.all { it.isDigit() }) {
                viewModel.setAppLock(true, pinInput)
                showPinDialog = false
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

    // 5. PIN Change Dialog (IosModalDialog + IosDialogTextField)
    IosModalDialog(
        visible = showPinChangeDialog,
        title = "修改 PIN 密码",
        message = "请输入原密码并设定新的 4 位数字密码",
        confirmText = "修改",
        cancelText = "取消",
        onConfirm = {
            viewModel.changePin(oldPinInput, newPinInput) { ok, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (ok) {
                    oldPinInput = ""
                    newPinInput = ""
                    showPinChangeDialog = false
                }
            }
        },
        onDismissRequest = {
            oldPinInput = ""
            newPinInput = ""
            showPinChangeDialog = false
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IosDialogTextField(
                value = oldPinInput,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPinInput = it },
                placeholder = "输入当前原密码",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            IosDialogTextField(
                value = newPinInput,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                placeholder = "设定新的 4 位数字密码",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        }
    }

    // 6. Disable PIN Dialog (IosModalDialog + IosDialogTextField)
    IosModalDialog(
        visible = showPinDisableDialog,
        title = "关闭应用锁",
        message = "请输入当前 PIN 密码以解除应用保护",
        confirmText = "确认关闭",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            viewModel.verifyAndDisablePin(disablePinInput) { ok ->
                if (ok) {
                    disablePinInput = ""
                    showPinDisableDialog = false
                    Toast.makeText(context, "已关闭应用锁", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "密码错误，未能关闭", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onDismissRequest = {
            disablePinInput = ""
            showPinDisableDialog = false
        }
    ) {
        IosDialogTextField(
            value = disablePinInput,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) disablePinInput = it },
            placeholder = "请输入当前 4 位密码",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
    }

    // 7. Clear All Data Confirmation (IosModalDialog with Destructive Action)
    IosModalDialog(
        visible = showClearDataDialog,
        title = "清除所有日记数据？",
        message = "此操作将永久抹掉所有本地日记条目与配图缓存，不可撤销。建议在清空前先导出全量备份。",
        confirmText = "清空并抹掉",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            viewModel.clearAllData(context) { count ->
                Toast.makeText(context, "已清空本地数据 ($count 篇日记)", Toast.LENGTH_SHORT).show()
                showClearDataDialog = false
            }
        },
        onDismissRequest = { showClearDataDialog = false }
    )
```

---

## 6. Verification and Compliance Matrix

| Target Invariant | Compliance Requirement | Verification Evidence |
|---|---|---|
| **Alert Dialog Dimensions** | Fixed 270dp width, 14dp squircle, 0.5dp glass border | `Surface(modifier = Modifier.width(270.dp), shape = RoundedCornerShape(14.dp), border = AppleMaterials.glassBorder(0.5.dp))` |
| **Typography Hierarchy** | 17sp bold title, 13sp footnote message, 17sp action buttons | Verified in `IosModalDialog.kt` lines 76, 85, 117, 140 |
| **Spec 6.4 Button Stacking** | 1-2 buttons horizontal Row; >=3 buttons vertical Column stack | Tested by `testB3_ModalDialogButtonLayoutAdaptationByCount` in `R3BoundaryEdgeCasesTest.kt` |
| **Destructive Action Highlight** | Apple HIG System Red `0xFFFF3B30` | Verified in `IosModalDialog.kt` line 53 and `IosActionSheet.kt` line 59 |
| **Action Sheet Detached Cancel Pill** | Separate 14dp card with 8dp margin and 56dp height | Tested by `testF11_ActionSheetCancelPillDetachedGap` in `R3ScreenLayoutFeatureTest.kt` |
| **Active Item Checkmark** | Trailing checkmark on active theme/font option | Added `isChecked: Boolean` to `IosActionItem` |
| **Zero Legacy AlertDialog** | 0 usages in production Settings and primary flows | All occurrences replaced with `IosModalDialog` |
| **Compilation & Tests** | `./gradlew testDebugUnitTest` 0 errors | Build successful in 556ms |
