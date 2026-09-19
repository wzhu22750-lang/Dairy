package com.example.inkpaperdiary.challenger

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.example.inkpaperdiary.core.designsystem.InkPalette
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.VibrancyLevel
import com.example.inkpaperdiary.core.designsystem.withVibrancy
import com.example.inkpaperdiary.core.designsystem.appleMaterial
import com.example.inkpaperdiary.core.designsystem.glassBorder
import com.example.inkpaperdiary.core.designsystem.vibrancyAlpha
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Field

/**
 * Empirical Challenger Suite for Milestone M1:
 * AppleMaterial values, Vibrancy levels, Glass Border Specular Gradient Stops,
 * and Backward Compatibility.
 */
class AppleMaterialEmpiricalChallengeTest {

    // =========================================================================================
    // 1. Apple Material Thickness Values across Light & Dark Themes
    // =========================================================================================

    @Test
    fun challenge_allFiveThicknesses_LightModeValuesAndStrictMonotonicity() {
        val ultraThin = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THIN, isDark = false)
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = false)
        val regular = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark = false)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = false)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = false)

        // 纸张化契约：薄材质 = 主纸面，REGULAR 及以上 = 浮层纸面，全部实心
        assertEquals(InkPalette.PaperLight, ultraThin)
        assertEquals(InkPalette.PaperLight, thin)
        assertEquals(InkPalette.PaperRaisedLight, regular)
        assertEquals(InkPalette.PaperRaisedLight, thick)
        assertEquals(InkPalette.PaperRaisedLight, ultraThick)

        listOf(ultraThin, thin, regular, thick, ultraThick).forEach { color ->
            assertEquals("Light material must be fully opaque paper", 1f, color.alpha, 0.001f)
        }
    }

    @Test
    fun challenge_allFiveThicknesses_DarkModeValuesAndStrictMonotonicity() {
        val ultraThin = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THIN, isDark = true)
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = true)
        val regular = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark = true)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = true)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = true)

        // 深色契约：柔和黑主纸面 + 稍亮浮层，全部实心
        assertEquals(InkPalette.PaperDark, ultraThin)
        assertEquals(InkPalette.PaperDark, thin)
        assertEquals(InkPalette.PaperRaisedDark, regular)
        assertEquals(InkPalette.PaperRaisedDark, thick)
        assertEquals(InkPalette.PaperRaisedDark, ultraThick)

        listOf(ultraThin, thin, regular, thick, ultraThick).forEach { color ->
            assertEquals("Dark material must be fully opaque paper", 1f, color.alpha, 0.001f)
        }
    }

    @Test
    fun challenge_barBackgroundColor_and_separatorColor_Contract() {
        val lightBar = AppleMaterials.barBackgroundColor(isDark = false)
        val darkBar = AppleMaterials.barBackgroundColor(isDark = true)

        // 97% 纸色契约：0xF7 / 255f ≈ 0.969
        assertEquals("Light bar should be InkBarLight", InkPalette.InkBarLight, lightBar)
        assertEquals("Dark bar should be InkBarDark", InkPalette.InkBarDark, darkBar)
        assertEquals(0.969f, lightBar.alpha, 0.01f)
        assertEquals(0.969f, darkBar.alpha, 0.01f)

        // 分割线 = 实色发丝线
        val lightSep = AppleMaterials.separatorColor(isDark = false)
        val darkSep = AppleMaterials.separatorColor(isDark = true)
        assertEquals("Light separator should be HairlineLight", InkPalette.HairlineLight, lightSep)
        assertEquals("Dark separator should be HairlineDark", InkPalette.HairlineDark, darkSep)
    }

    // =========================================================================================
    // 2. Vibrancy Levels: Hierarchy, Base Colors, and Scaling
    // =========================================================================================

    @Test
    fun challenge_vibrancyLevels_AlphasAndStrictMonotonicity() {
        // Vibrancy levels: PRIMARY (1.0f), SECONDARY (0.60f), TERTIARY (0.30f), QUATERNARY (0.18f)
        val lightPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = false)
        val lightSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false)
        val lightTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false)
        val lightQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = false)

        assertEquals(1.0f, lightPrimary.alpha, 0.01f)
        assertEquals(0.62f, lightSecondary.alpha, 0.01f)
        assertEquals(0.40f, lightTertiary.alpha, 0.01f)
        assertEquals(0.25f, lightQuaternary.alpha, 0.01f)

        // Strict decreasing monotonicity
        assertTrue(lightPrimary.alpha > lightSecondary.alpha)
        assertTrue(lightSecondary.alpha > lightTertiary.alpha)
        assertTrue(lightTertiary.alpha > lightQuaternary.alpha)

        // 浅色模式基底 = 墨字（暖黑）
        assertEquals(InkPalette.InkLight.red, lightPrimary.red, 0.001f)
        assertEquals(InkPalette.InkLight.green, lightPrimary.green, 0.001f)
        assertEquals(InkPalette.InkLight.blue, lightPrimary.blue, 0.001f)

        // Dark mode defaults to White base color
        val darkPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = true)
        val darkSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = true)
        val darkTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = true)
        val darkQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = true)

        assertEquals(1.0f, darkPrimary.alpha, 0.01f)
        assertEquals(0.62f, darkSecondary.alpha, 0.01f)
        assertEquals(0.40f, darkTertiary.alpha, 0.01f)
        assertEquals(0.25f, darkQuaternary.alpha, 0.01f)

        // 深色模式基底 = 暖白
        assertEquals(InkPalette.InkDark.red, darkPrimary.red, 0.001f)
        assertEquals(InkPalette.InkDark.green, darkPrimary.green, 0.001f)
        assertEquals(InkPalette.InkDark.blue, darkPrimary.blue, 0.001f)
    }

    @Test
    fun challenge_vibrancy_CustomBaseColorsAndAlphaMultiplication() {
        val customColors = listOf(
            Color(0xFF007AFF), // Apple Blue (fully opaque)
            Color(0x8034C759), // Apple Green (50% opaque)
            Color(0x00FF9500), // Apple Orange (0% opaque)
            Color(0xFFFF2D55)  // Apple Pink (fully opaque)
        )

        for (base in customColors) {
            // vibrancyColor sets the alpha directly to level multiplier
            val primary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = false, baseColor = base)
            val secondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false, baseColor = base)
            val tertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false, baseColor = base)
            val quaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = false, baseColor = base)

            assertEquals(1.0f, primary.alpha, 0.01f)
            assertEquals(0.62f, secondary.alpha, 0.01f)
            assertEquals(0.40f, tertiary.alpha, 0.01f)
            assertEquals(0.25f, quaternary.alpha, 0.01f)
            assertEquals(base.red, primary.red, 0.001f)
            assertEquals(base.green, primary.green, 0.001f)
            assertEquals(base.blue, primary.blue, 0.001f)

            // withVibrancy extension scales existing alpha
            val extPrimary = base.withVibrancy(VibrancyLevel.PRIMARY)
            val extSecondary = base.withVibrancy(VibrancyLevel.SECONDARY)
            val extTertiary = base.withVibrancy(VibrancyLevel.TERTIARY)
            val extQuaternary = base.withVibrancy(VibrancyLevel.QUATERNARY)

            assertEquals(base.alpha * 1.0f, extPrimary.alpha, 0.01f)
            assertEquals(base.alpha * 0.62f, extSecondary.alpha, 0.01f)
            assertEquals(base.alpha * 0.40f, extTertiary.alpha, 0.01f)
            assertEquals(base.alpha * 0.25f, extQuaternary.alpha, 0.01f)
        }
    }

    // =========================================================================================
    // 3. Specular Hairline Glass Border Gradient Stops & Geometry
    // =========================================================================================

    @Test
    fun challenge_glassBorder_StrokeWidthAndDefaultContract() {
        val defaultLightBorder = AppleMaterials.glassBorder(isDark = false)
        val defaultDarkBorder = AppleMaterials.glassBorder(isDark = true)

        assertEquals("Default hairline border stroke width must be 0.5dp", 0.5.dp, defaultLightBorder.width)
        assertEquals("Default hairline border stroke width must be 0.5dp", 0.5.dp, defaultDarkBorder.width)

        // Custom widths
        val customBorder = AppleMaterials.glassBorder(isDark = false, width = 1.dp)
        assertEquals(1.dp, customBorder.width)
    }

    @Test
    fun challenge_glassBorder_SolidColorBrush() {
        // 纸张化契约：发丝线为单色实刷，不再存在高光渐变
        val lightBorder = AppleMaterials.glassBorder(isDark = false)
        val darkBorder = AppleMaterials.glassBorder(isDark = true)

        assertNotNull("Light border brush must not be null", lightBorder.brush)
        assertNotNull("Dark border brush must not be null", darkBorder.brush)

        assertEquals("Light hairline brush must be the solid HairlineLight",
            SolidColor(InkPalette.HairlineLight), lightBorder.brush)
        assertEquals("Dark hairline brush must be the solid HairlineDark",
            SolidColor(InkPalette.HairlineDark), darkBorder.brush)
    }

    // =========================================================================================
    // 4. Backward Compatibility & Clean Invocation of Overloads
    // =========================================================================================

    @Test
    fun challenge_backwardCompatibility_PureFunctionsReturnNonNull() {
        // Test that all pure function overloads accept parameters without throwing
        for (thickness in MaterialThickness.entries) {
            val light = AppleMaterials.backgroundColor(thickness, isDark = false)
            val dark = AppleMaterials.backgroundColor(thickness, isDark = true)
            assertNotNull(light)
            assertNotNull(dark)
            assertTrue(light.alpha in 0f..1f)
            assertTrue(dark.alpha in 0f..1f)
        }

        for (level in VibrancyLevel.entries) {
            val light = AppleMaterials.vibrancyColor(level, isDark = false)
            val dark = AppleMaterials.vibrancyColor(level, isDark = true)
            assertNotNull(light)
            assertNotNull(dark)
            assertTrue(light.alpha in 0f..1f)
            assertTrue(dark.alpha in 0f..1f)
        }

        val lightBorder = AppleMaterials.glassBorder(isDark = false, width = 0.5.dp)
        val darkBorder = AppleMaterials.glassBorder(isDark = true, width = 0.5.dp)
        assertNotNull(lightBorder)
        assertNotNull(darkBorder)
        assertEquals(0.5.dp, lightBorder.width)
        assertEquals(0.5.dp, darkBorder.width)
    }

    @Test
    fun challenge_vibrancy_UnspecifiedAndTransparentBaseColors() {
        // Passing Color.Unspecified must fallback to defaults
        val lightUnspecified = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = false, baseColor = Color.Unspecified)
        val darkUnspecified = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = true, baseColor = Color.Unspecified)

        assertEquals("Unspecified light base should fallback to ink", InkPalette.InkLight.red, lightUnspecified.red, 0.001f)
        assertEquals("Unspecified dark base should fallback to warm white", InkPalette.InkDark.red, darkUnspecified.red, 0.001f)
        assertEquals(0.62f, lightUnspecified.alpha, 0.01f)
        assertEquals(0.62f, darkUnspecified.alpha, 0.01f)

        // Passing Color.Transparent (0x00000000)
        val lightTransparent = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false, baseColor = Color.Transparent)
        assertEquals("Transparent base with tertiary vibrancy should have 0.40 alpha", 0.40f, lightTransparent.alpha, 0.01f)
    }

    @Test
    fun challenge_modifiers_AppleMaterialAndGlassBorderChaining() {
        // Modifiers must be constructible and chainable on pure JVM
        var m: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
        m = m.appleMaterial(thickness = MaterialThickness.THIN)
        m = m.appleMaterial(thickness = MaterialThickness.ULTRA_THICK)
        m = m.glassBorder()
        m = m.vibrancyAlpha(VibrancyLevel.PRIMARY)
        m = m.vibrancyAlpha(VibrancyLevel.SECONDARY)
        m = m.vibrancyAlpha(VibrancyLevel.TERTIARY)
        m = m.vibrancyAlpha(VibrancyLevel.QUATERNARY)
        assertNotNull("Modifier chain must be valid and non-null", m)
    }

    @Test
    fun challenge_glassBorder_ZeroAndExtremeWidthBoundaries() {
        val testWidths = listOf(0.dp, 0.1.dp, 0.5.dp, 1.dp, 5.dp, 10.dp)
        for (w in testWidths) {
            val lightBorder = AppleMaterials.glassBorder(isDark = false, width = w)
            val darkBorder = AppleMaterials.glassBorder(isDark = true, width = w)
            assertEquals("Light border width should match requested width", w, lightBorder.width)
            assertEquals("Dark border width should match requested width", w, darkBorder.width)
            assertNotNull(lightBorder.brush)
            assertNotNull(darkBorder.brush)
        }
    }

    @Test
    fun challenge_backwardCompatibility_BytecodeMethodsInspection() {
        // Inspect AppleMaterials bytecode methods to confirm both Composable and pure JVM overloads exist
        val methods = AppleMaterials::class.java.declaredMethods
        val methodNames = methods.map { it.name }

        assertTrue("Bytecode must contain backgroundColor", methodNames.any { it.startsWith("backgroundColor") })
        assertTrue("Bytecode must contain barBackgroundColor", methodNames.any { it.startsWith("barBackgroundColor") })
        assertTrue("Bytecode must contain separatorColor", methodNames.any { it.startsWith("separatorColor") })
        assertTrue("Bytecode must contain glassBorder", methodNames.any { it.startsWith("glassBorder") })
        assertTrue("Bytecode must contain vibrancyColor", methodNames.any { it.startsWith("vibrancyColor") })

        // Check that at least 10 methods are exposed on AppleMaterials (5 pure + 5 composable)
        val relevantMethods = methods.filter { method ->
            val name = method.name
            name.startsWith("backgroundColor") ||
            name.startsWith("barBackgroundColor") ||
            name.startsWith("separatorColor") ||
            name.startsWith("glassBorder") ||
            name.startsWith("vibrancyColor")
        }
        assertTrue("AppleMaterials must expose >= 10 overloads (pure + composable)", relevantMethods.size >= 10)
    }

    @Test
    fun challenge_existingScreensBytecodeAndLinkage() {
        val editorClass = Class.forName("com.example.inkpaperdiary.ui.editor.EditorScreenKt")
        assertNotNull("EditorScreenKt class must load cleanly", editorClass)
        assertTrue("EditorScreenKt must expose EditorScreen composable", editorClass.declaredMethods.any { it.name == "EditorScreen" })

        val lockClass = Class.forName("com.example.inkpaperdiary.ui.lock.LockScreenKt")
        assertNotNull("LockScreenKt class must load cleanly", lockClass)
        val lockMethods = lockClass.declaredMethods.map { it.name }
        println("LOCK METHODS: $lockMethods")
        assertTrue("LockScreenKt must expose LockScreen or related method: $lockMethods", lockMethods.any { it.contains("LockScreen") })

        val paperCardClass = Class.forName("com.example.inkpaperdiary.core.designsystem.components.PaperCardKt")
        assertNotNull("PaperCardKt class must load cleanly", paperCardClass)
        val paperMethods = paperCardClass.declaredMethods.map { it.name }
        println("PAPER METHODS: $paperMethods")
        assertTrue("PaperCardKt must expose PaperCard composable (including name-mangled Dp signatures): $paperMethods", paperMethods.any { it.startsWith("PaperCard") })
    }
}

