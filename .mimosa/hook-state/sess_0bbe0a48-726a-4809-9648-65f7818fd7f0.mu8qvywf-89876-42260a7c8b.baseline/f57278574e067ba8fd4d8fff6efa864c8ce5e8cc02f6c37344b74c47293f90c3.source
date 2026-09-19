package com.example.inkpaperdiary.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dairy Design System 2.0 — 纸张材质 (Paper Surfaces)
 *
 * 原 iOS 毛玻璃材质已全部压平：书页是实心的纸，光不穿透它。
 * 仅保留一个 0.5dp 实色发丝线，用于浮层与列表分组的极轻轮廓。
 * API 面保持兼容（MaterialThickness / AppleMaterials），内部值全部指向纸张色板。
 */
enum class MaterialThickness {
    ULTRA_THIN,
    THIN,
    REGULAR,
    THICK,
    ULTRA_THICK
}

/** 前景墨色透明层级（替代原 Vibrancy 概念）。 */
enum class VibrancyLevel {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    QUATERNARY
}

object AppleMaterials {

    /** 浮层底色：统一为纸张面，厚度仅影响不透明度细节。 */
    fun backgroundColor(thickness: MaterialThickness, isDark: Boolean): Color {
        return when (thickness) {
            MaterialThickness.ULTRA_THIN -> InkTones.paper(isDark)
            MaterialThickness.THIN -> InkTones.paper(isDark)
            MaterialThickness.REGULAR -> InkTones.raised(isDark)
            MaterialThickness.THICK -> InkTones.raised(isDark)
            MaterialThickness.ULTRA_THICK -> InkTones.raised(isDark)
        }
    }

    @Composable
    fun backgroundColor(thickness: MaterialThickness): Color =
        backgroundColor(thickness, isSystemInDarkTheme())

    /** 顶栏 / 底栏：97% 纸色，微透出滚动内容。 */
    fun barBackgroundColor(isDark: Boolean): Color =
        if (isDark) InkPalette.InkBarDark else InkPalette.InkBarLight

    @Composable
    fun barBackgroundColor(): Color =
        barBackgroundColor(isSystemInDarkTheme())

    /** 分割线 / 描边：实色发丝线。 */
    fun separatorColor(isDark: Boolean): Color =
        InkTones.hairline(isDark)

    @Composable
    fun separatorColor(): Color =
        separatorColor(isSystemInDarkTheme())

    /** 发丝线边框：0.5dp 实色，无渐变高光。 */
    fun glassBorder(
        isDark: Boolean,
        width: Dp = 0.5.dp
    ): BorderStroke {
        return BorderStroke(width, InkTones.hairline(isDark))
    }

    @Composable
    fun glassBorder(
        width: Dp = 0.5.dp
    ): BorderStroke = glassBorder(isSystemInDarkTheme(), width)

    /** 墨色透明层级。 */
    fun vibrancyColor(
        level: VibrancyLevel,
        isDark: Boolean,
        baseColor: Color = Color.Unspecified
    ): Color {
        val defaultBase = if (isDark) InkPalette.InkDark else InkPalette.InkLight
        val targetBase = if (baseColor != Color.Unspecified) baseColor else defaultBase

        val alpha = when (level) {
            VibrancyLevel.PRIMARY -> 1.0f
            VibrancyLevel.SECONDARY -> 0.62f
            VibrancyLevel.TERTIARY -> 0.40f
            VibrancyLevel.QUATERNARY -> 0.25f
        }
        return targetBase.copy(alpha = alpha)
    }

    @Composable
    fun vibrancyColor(
        level: VibrancyLevel,
        baseColor: Color = Color.Unspecified
    ): Color = vibrancyColor(level, isSystemInDarkTheme(), baseColor)
}

/** 按墨色层级快速叠加透明度。 */
fun Color.withVibrancy(level: VibrancyLevel): Color {
    val alphaMultiplier = when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.62f
        VibrancyLevel.TERTIARY -> 0.40f
        VibrancyLevel.QUATERNARY -> 0.25f
    }
    return this.copy(alpha = this.alpha * alphaMultiplier)
}

/** 为组件施加纸张底色与可选发丝线。 */
fun Modifier.appleMaterial(
    thickness: MaterialThickness = MaterialThickness.REGULAR,
    shape: Shape? = null,
    hasBorder: Boolean = false,
    borderWidth: Dp = 0.5.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val bgColor = AppleMaterials.backgroundColor(thickness, isDark)
    val resolvedShape = shape ?: RectangleShape

    var m = this
    if (hasBorder) {
        m = m.border(AppleMaterials.glassBorder(isDark, borderWidth), resolvedShape)
    }
    m = m.background(bgColor, shape = resolvedShape)
    if (shape != null) {
        m = m.clip(shape)
    }
    m
}

/** 为已有底色的组件单独施加 0.5dp 发丝线。 */
fun Modifier.glassBorder(
    shape: Shape = RectangleShape,
    width: Dp = 0.5.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    border(AppleMaterials.glassBorder(isDark, width), shape = shape)
}

fun Modifier.vibrancyAlpha(level: VibrancyLevel): Modifier = this.alpha(
    when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.62f
        VibrancyLevel.TERTIARY -> 0.40f
        VibrancyLevel.QUATERNARY -> 0.25f
    }
)

/** 为子树注入统一的墨色前景。 */
@Composable
fun ProvideVibrancy(
    level: VibrancyLevel,
    baseColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val vibrantColor = AppleMaterials.vibrancyColor(level, baseColor = baseColor)
    CompositionLocalProvider(
        LocalContentColor provides vibrantColor,
        content = content
    )
}
