package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Dairy Design System 2.0 — E-ink 纸张色彩系统 (Kindle-inspired Paper & Ink Palette)
 *
 * 核心审美：
 * 1. 纸张即界面：页面背景就是一张纸，没有卡片、没有毛玻璃、没有投影分层。
 * 2. 暖色调黑纸白字：浅色为纸张白上写墨字，深色为柔和黑上写暖白，像一台夜间背光柔和的阅读器。
 * 3. 零饱和度：全应用只存在黑、白、灰与发丝线，唯一的"颜色"是墨的浓淡。
 */
object InkPalette {
    // -----------------------------------------------------------
    // 浅色模式：纸张白 (Day Paper)
    // -----------------------------------------------------------
    val PaperLight = Color(0xFFFBFAF7)          // 纸张白：主背景（暖白，非冷灰）
    val PaperRaisedLight = Color(0xFFFFFFFF)    // 浮层/弹层白：稍亮于主背景
    val WashLight = Color(0xFFF0EEE8)           // 淡墨晕染：选中底、输入填充
    val InkLight = Color(0xFF1D1C1A)            // 墨字：主标题与正文
    val InkSoftLight = Color(0xFF6E6B64)        // 淡墨：次级文字
    val InkFaintLight = Color(0xFFA09D95)       // 极淡墨：辅助信息、占位
    val HairlineLight = Color(0xFFE7E4DD)       // 发丝线：分割线与描边
    val InkBarLight = Color(0xF7FBFAF7)         // 顶栏/底栏：97% 纸白（微透出滚动内容）

    // -----------------------------------------------------------
    // 深色模式：柔和黑 (Night Ink)
    // -----------------------------------------------------------
    val PaperDark = Color(0xFF151412)           // 柔和黑：主背景（非纯黑，保留纸感）
    val PaperRaisedDark = Color(0xFF1E1C1A)     // 浮层黑：稍亮于主背景
    val WashDark = Color(0xFF232120)            // 夜间晕染：选中底、输入填充
    val InkDark = Color(0xFFE6E3DB)             // 暖白字：主标题与正文
    val InkSoftDark = Color(0xFF9B988F)         // 淡暖白：次级文字
    val InkFaintDark = Color(0xFF6C6961)        // 极淡暖白：辅助信息、占位
    val HairlineDark = Color(0xFF2C2A27)        // 夜间发丝线
    val InkBarDark = Color(0xF7151412)          // 顶栏/底栏：97% 柔和黑

    // 破坏性操作：朱砂红（全应用唯一的彩色，仅用于删除）
    val Cinnabar = Color(0xFFB3432B)
}

/**
 * 语义化墨阶：按"墨的浓淡"组织的前景色，供正文字层级直接取用。
 */
object InkTones {
    fun primary(isDark: Boolean) = if (isDark) InkPalette.InkDark else InkPalette.InkLight
    fun secondary(isDark: Boolean) = if (isDark) InkPalette.InkSoftDark else InkPalette.InkSoftLight
    fun tertiary(isDark: Boolean) = if (isDark) InkPalette.InkFaintDark else InkPalette.InkFaintLight
    fun paper(isDark: Boolean) = if (isDark) InkPalette.PaperDark else InkPalette.PaperLight
    fun raised(isDark: Boolean) = if (isDark) InkPalette.PaperRaisedDark else InkPalette.PaperRaisedLight
    fun wash(isDark: Boolean) = if (isDark) InkPalette.WashDark else InkPalette.WashLight
    fun hairline(isDark: Boolean) = if (isDark) InkPalette.HairlineDark else InkPalette.HairlineLight
}
