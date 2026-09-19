package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily

/**
 * 现代 iOS 极简胶囊徽章组件 (iOS Capsule Pill Badge)
 *
 * 扁平化无衬线风格，圆润通透，去除过度倾斜与厚重印泥质感。
 */
@Composable
fun StampBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
    rotation: Float = 0f // 保留参数兼容旧代码
) {
    val pillShape = CapsuleShape
    Box(
        modifier = modifier
            .clip(pillShape)
            .background(tintColor.copy(alpha = 0.08f), pillShape)
            .border(0.5.dp, tintColor.copy(alpha = 0.18f), pillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(12.dp),
                    contentAlignment = Alignment.Center
                ) { icon() }
            }
            Text(
                text = text,
                color = tintColor,
                fontFamily = SansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}
