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
import com.example.inkpaperdiary.core.designsystem.InkPalette
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
 * Spec 6.4: Adapts button layout based on button count.
 * 1 or 2 buttons -> horizontal Row, side-by-side.
 * 3+ buttons -> vertical Column stack.
 */
fun isDialogButtonLayoutVertical(buttonCount: Int): Boolean = buttonCount >= 3

/**
 * 纸张化警告对话框 (Dairy 2.0)
 *
 * Characteristics:
 * - 270dp standardized fixed width
 * - 14dp 微圆角
 * - 居中标题与消息
 * - Spec 6.4 adaptive button layout:
 *   * 1 button: 44dp full width
 *   * 2 buttons: 44dp horizontal split row with 0.5dp vertical hairline divider
 *   * 3+ buttons: 44dp vertical column stack with 0.5dp horizontal hairline dividers
 * - 确认按钮为墨色（跟随主题），破坏性操作为朱砂（全应用唯一彩色）
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
        if (!cancelText.isNullOrBlank()) {
            add(IosDialogAction(title = cancelText, onClick = onDismissRequest))
        }
        add(
            IosDialogAction(
                title = confirmText,
                isDestructive = isDestructive,
                isDefault = true,
                onClick = onConfirm
            )
        )
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

/**
 * Multi-action overload for IosModalDialog supporting arbitrary action lists.
 */
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
    val destructiveRed = InkPalette.Cinnabar
    val systemBlue = MaterialTheme.colorScheme.onBackground

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
                        fontWeight = FontWeight.SemiBold,
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
                        val textColor = when {
                            action.isDestructive -> destructiveRed
                            action.isDefault -> systemBlue
                            else -> MaterialTheme.colorScheme.primary
                        }
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
                            val firstColor = when {
                                firstAction.isDestructive -> destructiveRed
                                firstAction.isDefault -> systemBlue
                                else -> MaterialTheme.colorScheme.primary
                            }
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
                            val secondColor = when {
                                secondAction.isDestructive -> destructiveRed
                                secondAction.isDefault -> systemBlue
                                else -> MaterialTheme.colorScheme.primary
                            }
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
                                val textColor = when {
                                    action.isDestructive -> destructiveRed
                                    action.isDefault -> systemBlue
                                    else -> MaterialTheme.colorScheme.primary
                                }
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
 * Standardized at 34dp height, 6dp squircle corners, subtle translucent fill, and 0.5dp hairline border.
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
