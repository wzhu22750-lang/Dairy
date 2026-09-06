package com.example.inkpaperdiary.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * - ultraThin: 极薄，最高透光度，用于次要背景穿透
 * - thin: 薄，适度透光，用于轻量筛选、小胶囊
 * - regular: 标准，经典平衡，用于导航顶栏 (TopAppBar)、搜索条、底部悬浮操作栏
 * - thick: 厚，低透光高对比，用于悬浮日记卡片、成组列表
 * - ultraThick: 极厚，用于锁屏密码盘、高对比模态弹窗
 */
enum class MaterialThickness {
    ULTRA_THIN,
    THIN,
    REGULAR,
    THICK,
    ULTRA_THICK
}

object AppleMaterials {
    /**
     * 根据当前系统深浅色模式与 Apple HIG 材质厚度等级，返回标准半透明底色
     */
    @Composable
    fun backgroundColor(thickness: MaterialThickness): Color {
        val isDark = isSystemInDarkTheme()
        return when (thickness) {
            MaterialThickness.ULTRA_THIN -> if (isDark) Color(0x661C1C1E) else Color(0x73FFFFFF)
            MaterialThickness.THIN -> if (isDark) Color(0x8C1C1C1E) else Color(0x99FFFFFF)
            MaterialThickness.REGULAR -> if (isDark) Color(0xD9161618) else Color(0xE6F2F2F7)
            MaterialThickness.THICK -> if (isDark) Color(0xF21C1C1E) else Color(0xF5FFFFFF)
            MaterialThickness.ULTRA_THICK -> if (isDark) Color(0xFA121214) else Color(0xFDFFFFFF)
        }
    }

    /**
     * Apple HIG 玻璃边缘折射与发丝线高光边框 (Hairline Glass Border)
     * 浅色模式具有顶部微光高光与极淡环境阴影线；深色模式具有晶莹发丝反射线。
     */
    @Composable
    fun glassBorder(
        isDark: Boolean = isSystemInDarkTheme(),
        width: Dp = 0.5.dp
    ): BorderStroke {
        val brush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x38FFFFFF), // 顶部边缘微反射
                    Color(0x14FFFFFF)  // 底部极淡透光
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x99FFFFFF), // 浅色顶部高光
                    Color(0x1F000000)  // 浅色底部极细分割阴影
                )
            )
        }
        return BorderStroke(width, brush)
    }

    /**
     * Apple HIG Vibrancy (鲜明度/活力度)：根据当前材质动态调节前景文字/图标的透明度
     */
    @Composable
    fun vibrancyColor(level: VibrancyLevel, baseColor: Color = Color.Unspecified): Color {
        val isDark = isSystemInDarkTheme()
        val defaultBase = if (isDark) Color.White else Color.Black
        val targetBase = if (baseColor != Color.Unspecified) baseColor else defaultBase

        return when (level) {
            VibrancyLevel.PRIMARY -> targetBase.copy(alpha = 1.0f)     // 100% 主标题/核心图标
            VibrancyLevel.SECONDARY -> targetBase.copy(alpha = 0.60f)  // 60% 次要文字/标签
            VibrancyLevel.TERTIARY -> targetBase.copy(alpha = 0.30f)   // 30% 辅助提示/占位
            VibrancyLevel.QUATERNARY -> targetBase.copy(alpha = 0.18f) // 18% 细分界/微弱指示
        }
    }
}

enum class VibrancyLevel {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    QUATERNARY
}

/**
 * Compose 修饰符：快速为任何组件施加 Apple HIG 材质层级与发丝线微光边缘
 */
fun Modifier.appleMaterial(
    thickness: MaterialThickness = MaterialThickness.REGULAR,
    shape: Shape? = null,
    hasBorder: Boolean = true
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val bgColor = AppleMaterials.backgroundColor(thickness)
    val borderStroke = if (hasBorder) AppleMaterials.glassBorder(isDark) else null

    var m = this
    if (shape != null) {
        m = m.clip(shape)
    }
    m = m.background(bgColor, shape = shape ?: androidx.compose.ui.graphics.RectangleShape)
    if (borderStroke != null) {
        m = m.border(borderStroke, shape = shape ?: androidx.compose.ui.graphics.RectangleShape)
    }
    m
}
