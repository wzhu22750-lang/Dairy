package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dairy Design System 2.0 — 纸张材质契约测试。
 *
 * 新契约核心：
 * 1. 材质是实心纸面，不存在半透明渐变；
 * 2. 全系统唯一彩色是朱砂（破坏性操作）；
 * 3. 墨色按层级递减（PRIMARY > SECONDARY > TERTIARY > QUATERNARY）。
 */
class AppleMaterialTest {

    @Test
    fun testMaterialThicknessEnumCount() {
        assertEquals("MaterialThickness must have exactly 5 levels", 5, MaterialThickness.entries.size)
        assertTrue(MaterialThickness.entries.contains(MaterialThickness.ULTRA_THIN))
        assertTrue(MaterialThickness.entries.contains(MaterialThickness.THIN))
        assertTrue(MaterialThickness.entries.contains(MaterialThickness.REGULAR))
        assertTrue(MaterialThickness.entries.contains(MaterialThickness.THICK))
        assertTrue(MaterialThickness.entries.contains(MaterialThickness.ULTRA_THICK))
    }

    @Test
    fun testVibrancyLevelEnumCount() {
        assertEquals("VibrancyLevel must have exactly 4 tiers", 4, VibrancyLevel.entries.size)
        assertTrue(VibrancyLevel.entries.contains(VibrancyLevel.PRIMARY))
        assertTrue(VibrancyLevel.entries.contains(VibrancyLevel.SECONDARY))
        assertTrue(VibrancyLevel.entries.contains(VibrancyLevel.TERTIARY))
        assertTrue(VibrancyLevel.entries.contains(VibrancyLevel.QUATERNARY))
    }

    @Test
    fun testSurfacesAreSolidPaperLight() {
        // 浅色模式：薄材质为主纸面，厚材质为浮层纸面，全部实心
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = false)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = false)

        assertEquals(InkPalette.PaperLight, thin)
        assertEquals(InkPalette.PaperRaisedLight, thick)
        assertEquals(1f, thin.alpha, 0.001f)
        assertEquals(1f, thick.alpha, 0.001f)
    }

    @Test
    fun testSurfacesAreSolidPaperDark() {
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = true)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = true)

        assertEquals(InkPalette.PaperDark, thin)
        assertEquals(InkPalette.PaperRaisedDark, ultraThick)
    }

    @Test
    fun testBarBackgroundColor() {
        val lightBar = AppleMaterials.barBackgroundColor(isDark = false)
        val darkBar = AppleMaterials.barBackgroundColor(isDark = true)

        // 顶/底栏：97% 纸色，微透出滚动内容
        assertEquals(InkPalette.InkBarLight, lightBar)
        assertEquals(InkPalette.InkBarDark, darkBar)
        assertEquals(0xF7, (lightBar.alpha * 255).toInt())
        assertEquals(0xF7, (darkBar.alpha * 255).toInt())
    }

    @Test
    fun testSeparatorColors() {
        // 分割线 = 实色发丝线
        assertEquals(InkPalette.HairlineLight, AppleMaterials.separatorColor(isDark = false))
        assertEquals(InkPalette.HairlineDark, AppleMaterials.separatorColor(isDark = true))
    }

    @Test
    fun testVibrancyColorDefaultBase() {
        // 浅色模式 -> 墨字基底；深色模式 -> 暖白基底
        val lightPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = false)
        val lightSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false)
        val lightTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false)
        val lightQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = false)

        assertEquals(1.0f, lightPrimary.alpha, 0.01f)
        assertEquals(0.62f, lightSecondary.alpha, 0.01f)
        assertEquals(0.40f, lightTertiary.alpha, 0.01f)
        assertEquals(0.25f, lightQuaternary.alpha, 0.01f)

        // 墨色层级严格单调递减
        assertTrue(lightPrimary.alpha > lightSecondary.alpha)
        assertTrue(lightSecondary.alpha > lightTertiary.alpha)
        assertTrue(lightTertiary.alpha > lightQuaternary.alpha)

        val darkPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = true)
        assertEquals(InkPalette.InkDark, darkPrimary.copy(alpha = 1f))
    }

    @Test
    fun testVibrancyColorCustomBase() {
        val ink = Color(0xFF1D1C1A)
        val vibrantInk = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false, baseColor = ink)
        assertEquals(0.62f, vibrantInk.alpha, 0.01f)
        assertEquals(ink.red, vibrantInk.red, 0.001f)
        assertEquals(ink.green, vibrantInk.green, 0.001f)
        assertEquals(ink.blue, vibrantInk.blue, 0.001f)
    }

    @Test
    fun testColorWithVibrancyExtension() {
        val color = Color(0xFF1D1C1A)
        val res = color.withVibrancy(VibrancyLevel.TERTIARY)
        assertEquals(0.40f, res.alpha, 0.01f)
    }

    @Test
    fun testGlassBorderIsSolidHairline() {
        // 发丝线：0.5dp 实色，无渐变高光
        val lightBorder = AppleMaterials.glassBorder(isDark = false, width = 0.5.dp)
        val darkBorder = AppleMaterials.glassBorder(isDark = true, width = 0.5.dp)

        assertEquals(0.5.dp, lightBorder.width)
        assertEquals(0.5.dp, darkBorder.width)
        assertNotNull(lightBorder.brush)
        assertNotNull(darkBorder.brush)
    }

    @Test
    fun testGlassBorderCustomWidth() {
        val customBorder = AppleMaterials.glassBorder(isDark = false, width = 1.0.dp)
        assertEquals(1.0.dp, customBorder.width)
    }

    @Test
    fun testNoSaturatedColorsInCorePalette() {
        // E-ink 契约：核心色板所有颜色饱和度趋近于零（朱砂除外）
        val coreColors = listOf(
            InkPalette.PaperLight, InkPalette.PaperRaisedLight, InkPalette.WashLight,
            InkPalette.InkLight, InkPalette.InkSoftLight, InkPalette.InkFaintLight,
            InkPalette.HairlineLight,
            InkPalette.PaperDark, InkPalette.PaperRaisedDark, InkPalette.WashDark,
            InkPalette.InkDark, InkPalette.InkSoftDark, InkPalette.InkFaintDark,
            InkPalette.HairlineDark
        )
        coreColors.forEach { color ->
            val max = maxOf(color.red, color.green, color.blue)
            val min = minOf(color.red, color.green, color.blue)
            val saturation = if (max == 0f) 0f else (max - min) / max
            assertTrue(
                "颜色 $color 饱和度过高 ($saturation)，违反 E-ink 契约",
                saturation <= 0.20f
            )
        }
        // 朱砂是唯一允许的彩色
        assertTrue(InkPalette.Cinnabar.red > 0.5f)
    }
}
