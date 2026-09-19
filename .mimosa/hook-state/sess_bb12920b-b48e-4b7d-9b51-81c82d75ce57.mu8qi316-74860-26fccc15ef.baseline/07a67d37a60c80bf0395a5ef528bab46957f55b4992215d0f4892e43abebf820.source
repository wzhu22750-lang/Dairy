package com.example.inkpaperdiary.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Dairy Design System 2.0 — 排版系统 (Typography)
 *
 * Kindle 式双层文字体系：
 * 1. 界面骨架 (Chrome)：无衬线系统字体，小号、安静，仅存在于顶栏/底栏/设置行。
 * 2. 书页正文 (Reading)：默认衬线体，字号、行距、页宽由用户在阅读设置中调节。
 *
 * 界面文字永不抢戏；书页文字是一切的主角。
 */

/** 阅读正文字体选项。 */
enum class ReaderFont(val label: String) {
    SERIF("衬线"),
    SANS("无衬线");

    fun fontFamily(): FontFamily = when (this) {
        SERIF -> FontFamily.Serif
        SANS -> FontFamily.SansSerif
    }
}

/** 阅读排版设置：全局持久化，经 [LocalReadingSettings] 注入整棵 UI 树。 */
data class ReadingSettings(
    val font: ReaderFont = ReaderFont.SERIF,
    /** 正文字号缩放，1.0 = 18sp 基准，范围 0.8 ~ 1.4。 */
    val fontScale: Float = 1f,
    /** 行距系数：正文 lineHeight = fontSize * lineSpacing。 */
    val lineSpacing: Float = 1.75f,
    /** 阅读区域宽度缩放，1.0 = 铺满书页边距。 */
    val pageWidth: Float = 1f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val bodyFontSize: Float get() = 18f * fontScale
    val bodyLineHeight: Float get() = bodyFontSize * lineSpacing
}

/** 应用主题模式。 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("夜间")
}

val LocalReadingSettings = staticCompositionLocalOf { ReadingSettings() }

/**
 * 旧排版兼容：现代阅读器界面骨架的文字阶梯（无衬线）。
 * 数值收敛为安静的小字号，替代原 iOS 大标题系统。
 */
val PaperTypography = Typography(
    // 页面主标题（设置 / 回忆 等页首），收敛的 24sp
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // 浮层标题
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // 区块标题
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // 列表行标题
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // 次级标题
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // 正文（界面骨架用；书页正文请用 InkType）
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    )
)

/** 兼容别名：旧代码引用的字体族名（Phase 7 移除）。 */
val SansFontFamily = FontFamily.SansSerif
val SerifFontFamily = FontFamily.Serif

/**
 * InkType — 书页正文的动态文字样式。
 *
 * 所有阅读内容（时间书、阅读页、正文输入）一律经由 InkType 取样式，
 * 以响应用户在阅读设置中的字体 / 字号 / 行距调节。
 */
object InkType {

    /** 阅读页大标题（日记标题）：衬线，随 fontScale 温和缩放。 */
    @Composable
    fun readerTitle(): TextStyle {
        val s = LocalReadingSettings.current
        val size = (24f * (0.85f + 0.15f * s.fontScale)).sp
        return TextStyle(
            fontFamily = s.font.fontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = size,
            lineHeight = (size.value * 1.4f).sp,
            letterSpacing = 0.sp
        )
    }

    /** 书页日期章节数字：大号衬线细体。 */
    @Composable
    fun chapterDay(): TextStyle = TextStyle(
        fontFamily = LocalReadingSettings.current.font.fontFamily(),
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    /** 年 / 月章节标签：小号衬线，宽字距。 */
    @Composable
    fun chapterLabel(): TextStyle = TextStyle(
        fontFamily = LocalReadingSettings.current.font.fontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.5.sp
    )

    /** 书页正文：完全跟随阅读设置。 */
    @Composable
    fun body(): TextStyle {
        val s = LocalReadingSettings.current
        return TextStyle(
            fontFamily = s.font.fontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = s.bodyFontSize.sp,
            lineHeight = s.bodyLineHeight.sp,
            letterSpacing = 0.sp
        )
    }

    /** 引用：正文样式弱化为淡墨。 */
    @Composable
    fun quote(): TextStyle = body().copy(
        color = InkTones.secondary(isSystemInDarkTheme())
    )

    /** 辅助信息（日期、字数、元信息）：界面层小字。 */
    val meta: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.3.sp
    )
}
