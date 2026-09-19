package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PaperPattern {
    BLANK,
    RULED_LINES,
    DOTTED_GRID
}

/**
 * 现代 iOS 便签质感底纹 (Apple Notes Subtle Surface Pattern)
 */
fun Modifier.paperTexture(
    pattern: PaperPattern = PaperPattern.BLANK,
    lineColor: Color,
    lineSpacing: Dp = 28.dp,
    dotSpacing: Dp = 20.dp
): Modifier = this.drawBehind {
    when (pattern) {
        PaperPattern.BLANK -> {
            // 纯粹留白与通透空间
        }
        PaperPattern.RULED_LINES -> {
            val spacingPx = lineSpacing.toPx()
            var y = spacingPx
            while (y < size.height) {
                drawLine(
                    color = lineColor.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.6f
                )
                y += spacingPx
            }
        }
        PaperPattern.DOTTED_GRID -> {
            val spacingPx = dotSpacing.toPx()
            var x = spacingPx
            while (x < size.width) {
                var y = spacingPx
                while (y < size.height) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = 0.9f,
                        center = Offset(x, y)
                    )
                    y += spacingPx
                }
                x += spacingPx
            }
        }
    }
}
