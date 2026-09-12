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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Human Interface Guidelines (HIG) - 材质系统 (Materials)
 * 官方文档：https://developer.apple.com/cn/design/human-interface-guidelines/materials
 *
 * 核心设计哲学：
 * 材质是指在前景和背景元素间创建景深感、分层感和层次感的半透明视觉效果。
 * 系统定义了 5 级物理材质厚度（Thickness Levels）与 4 级鲜明度（Vibrancy Levels）：
 * - ULTRA_THIN: 极薄，最高透光度，用于次要背景穿透与微弱覆盖层 (45% Light / 40% Dark)
 * - THIN: 薄，适度透光，用于轻量筛选条、分段控制滑轨、小胶囊 (60% Light / 55% Dark)
 * - REGULAR: 标准，经典平衡，用于导航顶栏 (TopAppBar)、搜索条、浮动输入面板 (90% Light / 85% Dark)
 * - THICK: 厚，低透光高对比，用于悬浮日记卡片 (PaperCard)、成组列表 (IosListSection) (96% Light / 95% Dark)
 * - ULTRA_THICK: 极厚，用于锁屏密码盘、高对比模态弹窗 (IosModalDialog)、操作动作表 (IosActionSheet) (99% Light / 98% Dark)
 */
enum class MaterialThickness {
    ULTRA_THIN,
    THIN,
    REGULAR,
    THICK,
    ULTRA_THICK
}

/**
 * Apple HIG Vibrancy (鲜明度/活力度)：
 * 动态调节前景文字、图标与微分割线的透明度，以保证在半透明材质之上的极佳对比度与通透感。
 */
enum class VibrancyLevel {
    PRIMARY,     // 100% 主标题/核心图标
    SECONDARY,   // 60% 次要文字/时间戳/标签
    TERTIARY,    // 30% 辅助提示/占位文字/禁用态
    QUATERNARY   // 18% 细发丝分界/微弱指示
}

object AppleMaterials {

    /**
     * 根据深浅色模式与材质厚度等级返回标准半透明底色（纯函数，支持无 Compose 环境与单元测试）
     */
    fun backgroundColor(thickness: MaterialThickness, isDark: Boolean): Color {
        return when (thickness) {
            MaterialThickness.ULTRA_THIN -> if (isDark) Color(0x661C1C1E) else Color(0x73FFFFFF)
            MaterialThickness.THIN -> if (isDark) Color(0x8C1C1C1E) else Color(0x99FFFFFF)
            MaterialThickness.REGULAR -> if (isDark) Color(0xD9161618) else Color(0xE6F2F2F7)
            MaterialThickness.THICK -> if (isDark) Color(0xF21C1C1E) else Color(0xF5FFFFFF)
            MaterialThickness.ULTRA_THICK -> if (isDark) Color(0xFA121214) else Color(0xFDFFFFFF)
        }
    }

    /**
     * Composable 便捷重载：自动读取当前系统深浅色模式
     */
    @Composable
    fun backgroundColor(thickness: MaterialThickness): Color =
        backgroundColor(thickness, isSystemInDarkTheme())

    /**
     * iOS 底部标签栏与顶部常驻收起栏的 93% 通透底色
     */
    fun barBackgroundColor(isDark: Boolean): Color =
        if (isDark) Color(0xEE000000) else Color(0xEEF2F2F7)

    @Composable
    fun barBackgroundColor(): Color =
        barBackgroundColor(isSystemInDarkTheme())

    /**
     * iOS 成组列表与卡片内部发丝线分割色 (56dp / 16dp 缩进分割线)
     */
    fun separatorColor(isDark: Boolean): Color =
        if (isDark) Color(0x2EFFFFFF) else Color(0x1F000000)

    @Composable
    fun separatorColor(): Color =
        separatorColor(isSystemInDarkTheme())

    /**
     * Apple HIG 玻璃边缘折射与发丝线高光边框 (Hairline Glass Border)
     * 浅色模式具有顶部微光高光与极淡环境阴影线；深色模式具有晶莹发丝反射线。
     */
    fun glassBorder(
        isDark: Boolean,
        width: Dp = 0.5.dp
    ): BorderStroke {
        val brush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x38FFFFFF), // 顶部边缘微反射 (22% white)
                    Color(0x14FFFFFF)  // 底部极淡透光 (8% white)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x99FFFFFF), // 顶部高光 (60% white specular)
                    Color(0x1F000000)  // 底部极细阴影 (12% black contact shadow)
                )
            )
        }
        return BorderStroke(width, brush)
    }

    @Composable
    fun glassBorder(
        width: Dp = 0.5.dp
    ): BorderStroke = glassBorder(isSystemInDarkTheme(), width)

    /**
     * Apple HIG Vibrancy (鲜明度/活力度)：动态调节前景文字/图标的透明度
     */
    fun vibrancyColor(
        level: VibrancyLevel,
        isDark: Boolean,
        baseColor: Color = Color.Unspecified
    ): Color {
        val defaultBase = if (isDark) Color.White else Color.Black
        val targetBase = if (baseColor != Color.Unspecified) baseColor else defaultBase

        val alpha = when (level) {
            VibrancyLevel.PRIMARY -> 1.0f
            VibrancyLevel.SECONDARY -> 0.60f
            VibrancyLevel.TERTIARY -> 0.30f
            VibrancyLevel.QUATERNARY -> 0.18f
        }
        return targetBase.copy(alpha = alpha)
    }

    @Composable
    fun vibrancyColor(
        level: VibrancyLevel,
        baseColor: Color = Color.Unspecified
    ): Color = vibrancyColor(level, isSystemInDarkTheme(), baseColor)
}

/**
 * 颜色扩展函数：按 Vibrancy 等级快速叠加透明度
 */
fun Color.withVibrancy(level: VibrancyLevel): Color {
    val alphaMultiplier = when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.60f
        VibrancyLevel.TERTIARY -> 0.30f
        VibrancyLevel.QUATERNARY -> 0.18f
    }
    return this.copy(alpha = this.alpha * alphaMultiplier)
}

/**
 * Compose 修饰符：快速为任何组件施加 Apple HIG 材质层级与发丝线微光边缘
 */
fun Modifier.appleMaterial(
    thickness: MaterialThickness = MaterialThickness.REGULAR,
    shape: Shape? = null,
    hasBorder: Boolean = true,
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

/**
 * Compose 修饰符：为已有自定义背景或透明组件单独应用 0.5dp 发丝线微光玻璃边框
 */
fun Modifier.glassBorder(
    shape: Shape = RectangleShape,
    width: Dp = 0.5.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    border(AppleMaterials.glassBorder(isDark, width), shape = shape)
}

/**
 * Compose 修饰符：快速调整组件整体 Vibrancy 透明度
 */
fun Modifier.vibrancyAlpha(level: VibrancyLevel): Modifier = this.alpha(
    when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.60f
        VibrancyLevel.TERTIARY -> 0.30f
        VibrancyLevel.QUATERNARY -> 0.18f
    }
)

/**
 * 局部 Composable 容器：为子树中的 Text 和 Icon 自动注入 Vibrancy 内容色彩
 */
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
