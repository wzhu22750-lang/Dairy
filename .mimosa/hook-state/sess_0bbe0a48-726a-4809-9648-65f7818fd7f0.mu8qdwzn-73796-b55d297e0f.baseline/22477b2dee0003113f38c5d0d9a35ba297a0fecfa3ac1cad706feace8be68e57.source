package com.example.inkpaperdiary.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.InkTones
import com.example.inkpaperdiary.core.designsystem.PaperTypography
import com.example.inkpaperdiary.core.designsystem.ReaderFont
import com.example.inkpaperdiary.core.designsystem.ReadingSettings
import com.example.inkpaperdiary.core.designsystem.SerifFontFamily
import com.example.inkpaperdiary.core.designsystem.ThemeMode
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * 阅读设置面板（Aa）— Kindle 式的排版调节：
 * 字体、字号、行距、页宽、夜间模式。
 * 每一项立即生效并持久化。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    visible: Boolean,
    settings: ReadingSettings,
    onDismiss: () -> Unit,
    onSetFont: (ReaderFont) -> Unit,
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
    onSetLineSpacing: (Float) -> Unit,
    onSetPageWidth: (Float) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "排版",
                style = PaperTypography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(18.dp))

            // ---- 字体 ----
            OptionRow(label = "字体") {
                TextOption(
                    text = "衬线",
                    selected = settings.font == ReaderFont.SERIF,
                    fontFamily = SerifFontFamily,
                    onClick = { onSetFont(ReaderFont.SERIF) }
                )
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(
                    text = "无衬线",
                    selected = settings.font == ReaderFont.SANS,
                    onClick = { onSetFont(ReaderFont.SANS) }
                )
            }
            SheetDivider()

            // ---- 字号 ----
            OptionRow(label = "字号") {
                TextOption(text = "A", selected = false, small = true, onClick = onDecreaseFont)
                Spacer(modifier = Modifier.padding(2.dp))
                Text(
                    text = "${(settings.fontScale * 100).toInt()}",
                    style = PaperTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(text = "A", selected = false, big = true, onClick = onIncreaseFont)
            }
            SheetDivider()

            // ---- 行距 ----
            OptionRow(label = "行距") {
                TextOption(text = "紧凑", selected = settings.lineSpacing <= 1.5f, onClick = { onSetLineSpacing(1.5f) })
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(text = "标准", selected = settings.lineSpacing in 1.51f..1.85f, onClick = { onSetLineSpacing(1.75f) })
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(text = "宽松", selected = settings.lineSpacing > 1.85f, onClick = { onSetLineSpacing(2.0f) })
            }
            SheetDivider()

            // ---- 页宽 ----
            OptionRow(label = "页宽") {
                TextOption(text = "窄", selected = settings.pageWidth <= 0.8f, onClick = { onSetPageWidth(0.78f) })
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(text = "标准", selected = settings.pageWidth in 0.81f..0.94f, onClick = { onSetPageWidth(0.86f) })
                Spacer(modifier = Modifier.padding(2.dp))
                TextOption(text = "宽", selected = settings.pageWidth > 0.94f, onClick = { onSetPageWidth(1f) })
            }
            SheetDivider()

            // ---- 夜间模式 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "纸面",
                    style = PaperTypography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                    TextOption(text = "日", selected = settings.themeMode == ThemeMode.LIGHT, onClick = { onSetThemeMode(ThemeMode.LIGHT) })
                    Spacer(modifier = Modifier.padding(2.dp))
                    TextOption(text = "随系统", selected = settings.themeMode == ThemeMode.SYSTEM, onClick = { onSetThemeMode(ThemeMode.SYSTEM) })
                    Spacer(modifier = Modifier.padding(2.dp))
                    TextOption(text = "夜", selected = settings.themeMode == ThemeMode.DARK, onClick = { onSetThemeMode(ThemeMode.DARK) })
                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                    Icon(
                        Icons.Outlined.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = PaperTypography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = { content() })
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}

/** 面板中的文字选项：选中的呈墨色胶囊，未选中为淡墨。 */
@Composable
private fun TextOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    fontFamily: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
    small: Boolean = false,
    big: Boolean = false
) {
    val fontSize = when {
        small -> 13.sp
        big -> 19.sp
        else -> 14.sp
    }
    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color = if (selected) InkTones.primary(isDark = androidx.compose.foundation.isSystemInDarkTheme())
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .iosClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}
