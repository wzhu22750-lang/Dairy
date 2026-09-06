package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors

/**
 * 现代 iOS HIG 材质卡片组件 (Apple HIG Material Surface Card)
 * 遵循官方规范：https://developer.apple.com/cn/design/human-interface-guidelines/materials
 *
 * 遵循规范：
 * 1. 16.dp 连续平滑圆角卡片 (Squircle)
 * 2. Apple HIG 材质层级 (默认采用 Thick 厚材质，高对比度悬浮)
 * 3. 0.5.dp 细发丝微光玻璃折射边框 (Hairline Specular Glass Border)
 * 4. 极简轻量环境柔和微投影 (Elevation 1~2.dp)
 * 5. 置顶指示：左侧精致圆润药丸指示条
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    thickness: MaterialThickness = MaterialThickness.THICK,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = AppleMaterials.backgroundColor(thickness),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 0.5.dp,
    elevation: Dp = 1.dp,
    hasCeladonAccent: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBorder = AppleMaterials.glassBorder(width = borderWidth)

    Card(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .then(
                if (hasCeladonAccent) {
                    Modifier.drawBehind {
                        // 现代 iOS 极简胶囊侧边置顶条
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(4.dp.toPx(), 14.dp.toPx()),
                            size = Size(3.dp.toPx(), size.height - 28.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = glassBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            content = content
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PaperCardPreview() {
    com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme {
        PaperCard(modifier = Modifier.padding(16.dp)) {
            Text("现代极简卡片标题", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text("这是一段 iOS 极简黑白通透毛玻璃质感的内容卡片示例...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
