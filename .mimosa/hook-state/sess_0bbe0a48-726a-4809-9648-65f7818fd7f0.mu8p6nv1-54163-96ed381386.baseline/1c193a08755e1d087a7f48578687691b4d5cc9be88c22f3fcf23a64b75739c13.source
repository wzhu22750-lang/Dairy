package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun testBackgroundColorLightModeAlphaValues() {
        val ultraThin = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THIN, isDark = false)
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = false)
        val regular = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark = false)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = false)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = false)

        assertEquals(0x73, (ultraThin.alpha * 255).toInt())
        assertEquals(0x99, (thin.alpha * 255).toInt())
        assertEquals(0xE6, (regular.alpha * 255).toInt())
        assertEquals(0xF5, (thick.alpha * 255).toInt())
        assertEquals(0xFD, (ultraThick.alpha * 255).toInt())

        // Monotonic increase in opacity
        assertTrue(ultraThin.alpha < thin.alpha)
        assertTrue(thin.alpha < regular.alpha)
        assertTrue(regular.alpha < thick.alpha)
        assertTrue(thick.alpha < ultraThick.alpha)
    }

    @Test
    fun testBackgroundColorDarkModeAlphaValues() {
        val ultraThin = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THIN, isDark = true)
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = true)
        val regular = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark = true)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = true)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = true)

        assertEquals(0x66, (ultraThin.alpha * 255).toInt())
        assertEquals(0x8C, (thin.alpha * 255).toInt())
        assertEquals(0xD9, (regular.alpha * 255).toInt())
        assertEquals(0xF2, (thick.alpha * 255).toInt())
        assertEquals(0xFA, (ultraThick.alpha * 255).toInt())

        // Monotonic increase in opacity
        assertTrue(ultraThin.alpha < thin.alpha)
        assertTrue(thin.alpha < regular.alpha)
        assertTrue(regular.alpha < thick.alpha)
        assertTrue(thick.alpha < ultraThick.alpha)
    }

    @Test
    fun testBarBackgroundColor() {
        val lightBar = AppleMaterials.barBackgroundColor(isDark = false)
        val darkBar = AppleMaterials.barBackgroundColor(isDark = true)

        assertEquals(Color(0xEEF2F2F7), lightBar)
        assertEquals(Color(0xEE000000), darkBar)
        assertEquals(0xEE, (lightBar.alpha * 255).toInt())
        assertEquals(0xEE, (darkBar.alpha * 255).toInt())
    }

    @Test
    fun testSeparatorColors() {
        val lightSep = AppleMaterials.separatorColor(isDark = false)
        val darkSep = AppleMaterials.separatorColor(isDark = true)

        assertEquals(Color(0x1F000000), lightSep)
        assertEquals(Color(0x2EFFFFFF), darkSep)
    }

    @Test
    fun testVibrancyColorDefaultBase() {
        // Light mode -> defaults to black base
        val lightPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = false)
        val lightSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false)
        val lightTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false)
        val lightQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = false)

        assertEquals(1.0f, lightPrimary.alpha, 0.01f)
        assertEquals(0.60f, lightSecondary.alpha, 0.01f)
        assertEquals(0.30f, lightTertiary.alpha, 0.01f)
        assertEquals(0.18f, lightQuaternary.alpha, 0.01f)
        assertEquals(0f, lightPrimary.red, 0.001f) // pure black

        // Dark mode -> defaults to white base
        val darkPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = true)
        val darkSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = true)
        val darkTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = true)
        val darkQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = true)

        assertEquals(1.0f, darkPrimary.alpha, 0.01f)
        assertEquals(0.60f, darkSecondary.alpha, 0.01f)
        assertEquals(0.30f, darkTertiary.alpha, 0.01f)
        assertEquals(0.18f, darkQuaternary.alpha, 0.01f)
        assertEquals(1f, darkPrimary.red, 0.001f) // pure white
    }

    @Test
    fun testVibrancyColorCustomBase() {
        val blue = Color(0xFF007AFF)
        val vibrantBlue = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false, baseColor = blue)
        assertEquals(0.60f, vibrantBlue.alpha, 0.01f)
        assertEquals(blue.red, vibrantBlue.red, 0.001f)
        assertEquals(blue.green, vibrantBlue.green, 0.001f)
        assertEquals(blue.blue, vibrantBlue.blue, 0.001f)
    }

    @Test
    fun testColorWithVibrancyExtension() {
        val color = Color(0xFF1C1C1E)
        val res = color.withVibrancy(VibrancyLevel.TERTIARY)
        assertEquals(0.30f, res.alpha, 0.01f)
    }

    @Test
    fun testGlassBorderSpecification() {
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
}
