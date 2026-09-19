package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 极简黑白与现代毛玻璃色彩系统 (iOS Minimalist Monochrome & Frosted Glass Spec)
 *
 * 核心审美：
 * 1. 极致黑白与高灰阶梯：去除高饱和度与彩斑，以纯黑、纯白与精细灰阶建构层级秩序。
 * 2. 现代毛玻璃半透明性：85%~93% 空间透光层级，配合 0.5dp 细发丝边框与柔和环境弥散阴影。
 * 3. 完美兼容 iOS 7 至今的扁平化空间通透感。
 */
object PaperColors {
    // -----------------------------------------------------------
    // 极简黑白核心基准色 (Minimalist Monochrome Core)
    // -----------------------------------------------------------
    val MonoBlack = Color(0xFF000000)               // 纯黑：主标题、核心强调、主操作
    val MonoWhite = Color(0xFFFFFFFF)               // 纯白：浅色卡片底色、深色文字

    // 细致灰阶层级 (Apple HIG Grayscale Scale)
    val MonoGray50 = Color(0xFFF9FAFB)
    val MonoGray100 = Color(0xFFF2F2F7)             // iOS 浅色系统成组背景 (System Grouped Background)
    val MonoGray200 = Color(0xFFE5E5EA)             // 细分隔线、浅色填充、未激活描边
    val MonoGray300 = Color(0xFFD1D1D6)             // 辅助线条
    val MonoGray400 = Color(0xFFC7C7CC)             // 占位提示文字
    val MonoGray500 = Color(0xFF8E8E93)             // iOS 次要标签文字 (Secondary Label)
    val MonoGray600 = Color(0xFF636366)             // 次级正文
    val MonoGray700 = Color(0xFF3A3A3C)             // 深色分隔线
    val MonoGray800 = Color(0xFF2C2C2E)             // 深色次级背景 (Secondary Grouped Dark)
    val MonoGray900 = Color(0xFF1C1C1E)             // iOS 深色卡片背景 (Elevated Dark Surface)
    val MonoBlackBg = Color(0xFF000000)             // iOS 深色主画布 (AMOLED True Black)

    // -----------------------------------------------------------
    // 现代毛玻璃半透明体系 (Frosted Glass Translucency)
    // -----------------------------------------------------------
    // 浅色模式毛玻璃
    val GlassLight = Color(0xD9FFFFFF)              // 85% 悬浮卡片毛玻璃
    val GlassLightBar = Color(0xEEF2F2F7)           // 93% 顶栏与底栏毛玻璃
    val GlassLightFill = Color(0x0A000000)          // 4% 超轻黑羽化填充
    val GlassBorderLight = Color(0x14000000)        // 8% 细发丝描边 (0.5dp)

    // 深色模式毛玻璃
    val GlassDark = Color(0xD91C1C1E)               // 85% 深色卡片毛玻璃
    val GlassDarkBar = Color(0xEE000000)            // 93% 深色顶底栏毛玻璃
    val GlassDarkFill = Color(0x14FFFFFF)           // 8% 深色微光填充
    val GlassBorderDark = Color(0x2EFFFFFF)         // 18% 深色高光细边框 (0.5dp)

    // -----------------------------------------------------------
    // 单色调雅致阶梯 (用于心情/天气符号在极简黑白中的优雅区分)
    // -----------------------------------------------------------
    val SlateCharcoal = Color(0xFF1C1C1E)           // 炭墨深灰
    val SlateGraphite = Color(0xFF3A3A3C)           // 石墨灰
    val SlateSteel = Color(0xFF48484A)              // 冷钢灰
    val SlateMedium = Color(0xFF636366)             // 烟灰
    val SlateSilver = Color(0xFF8E8E93)             // 银灰
    val SlateLight = Color(0xFFAEAEB2)              // 浅灰
    val SlateMuted = Color(0xFFC7C7CC)              // 雾灰

    // -----------------------------------------------------------
    // 兼容性语义别名 (平滑兼容老组件引用，全面转为极简黑白)
    // -----------------------------------------------------------
    val XuanPaper = MonoGray100                     // 浅色主背景转为 iOS Grouped Background
    val AgedPaper = MonoWhite                       // 浅色卡片转为纯白纸面
    val InkWashTint = MonoGray200                   // 浅灰填充
    val InkWashBorder = MonoGray200                 // 细分割线

    val InkBlack = MonoBlack                        // 标题主文字 -> 纯黑
    val MediumInk = MonoGray600                     // 次级文字
    val LightInk = MonoGray500                      // 辅助文字
    val GhostInk = MonoGray400                      // 占位底纹
    val InkWash = MonoGray200

    // 标志色映射为纯粹的黑白基调
    val RuCeladon = MonoBlack                       // 原天青主强调色 -> 纯黑/高对比黑
    val CeladonPale = MonoGray200                   // 选中态淡底
    val SealVermillion = MonoBlack                  // 印章红 -> 极简墨黑胶囊
    val SealVermillionLight = MonoGray200
    val Ochre = SlateSteel
    val PineSmokeIndigo = SlateCharcoal
    val BambooMoss = SlateGraphite

    // 浅色模式语义别名
    val LightParchmentBg = MonoGray100
    val LightPaperSurface = MonoWhite
    val LightPaperCard = MonoWhite
    val LightPaperBorder = GlassBorderLight
    val LightPaperDottedGrid = MonoGray300
    val LightPaperRuledLine = MonoGray200

    val LightInkPrimary = MonoBlack
    val LightInkSecondary = MonoGray500
    val LightInkTertiary = MonoGray400

    val SealRed = MonoBlack
    val SealRedDark = MonoBlack
    val SealRedLight = MonoGray200

    val VintageOlive = SlateGraphite
    val VintageOliveLight = MonoGray200
    val VintageAmber = SlateSteel
    val VintageAmberLight = MonoGray200
    val VintageNavy = SlateCharcoal
    val VintageNavyLight = MonoGray200
    val VintageRose = SlateMedium
    val VintageRoseLight = MonoGray200
    val VintageTeal = SlateSteel
    val VintageTealLight = MonoGray200

    // 深色模式语义别名
    val DarkParchmentBg = MonoBlackBg               // 纯黑 AMOLED
    val DarkPaperSurface = MonoGray900              // 深灰卡片
    val DarkPaperCard = MonoGray900
    val DarkPaperBorder = GlassBorderDark
    val DarkPaperDottedGrid = MonoGray700
    val DarkPaperRuledLine = MonoGray700

    val DarkInkPrimary = MonoWhite
    val DarkInkSecondary = MonoGray500
    val DarkInkTertiary = MonoGray600

    val DarkSealRed = MonoWhite
    val DarkSealRedBg = MonoGray800
    val DarkRuCeladon = MonoWhite
}
