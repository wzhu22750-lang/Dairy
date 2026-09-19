package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * 现代 iOS 胶囊标签组件 (iOS Capsule Pill Tag)
 *
 * 遵循规范：
 * 1. 50% 完美胶囊形状 (CapsuleShape)
 * 2. 0.5.dp 细发丝描边
 * 3. 极简黑白灰阶与微量半透明
 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val tagShape = CapsuleShape
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .clip(tagShape)
            .background(backgroundColor)
            .border(0.5.dp, borderColor, tagShape)
            .then(
                if (onClick != null) Modifier.iosClick(onClick = onClick) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else color)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            letterSpacing = 0.sp
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .iosClick { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Tag",
                    modifier = Modifier.size(12.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
