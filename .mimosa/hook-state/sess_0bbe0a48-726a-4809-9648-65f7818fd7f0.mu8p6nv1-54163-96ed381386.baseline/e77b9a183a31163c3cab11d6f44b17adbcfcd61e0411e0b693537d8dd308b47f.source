package com.example.inkpaperdiary.challenger

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

        // Exact ARGB checks
        assertEquals("Light ULTRA_THIN should be 0x73FFFFFF", Color(0x73FFFFFF), ultraThin)
        assertEquals("Light THIN should be 0x99FFFFFF", Color(0x99FFFFFF), thin)
        assertEquals("Light REGULAR should be 0xE6F2F2F7", Color(0xE6F2F2F7), regular)
        assertEquals("Light THICK should be 0xF5FFFFFF", Color(0xF5FFFFFF), thick)
        assertEquals("Light ULTRA_THICK should be 0xFDFFFFFF", Color(0xFDFFFFFF), ultraThick)

        // Expected alpha ranges according to HIG
        assertEquals(0.45f, ultraThin.alpha, 0.01f) // ~45%
        assertEquals(0.60f, thin.alpha, 0.01f)      // ~60%
        assertEquals(0.90f, regular.alpha, 0.01f)   // ~90%
        assertEquals(0.96f, thick.alpha, 0.01f)     // ~96%
        assertEquals(0.99f, ultraThick.alpha, 0.01f) // ~99%

        // Strict monotonicity check: Each level MUST be strictly more opaque than previous
        val alphas = listOf(ultraThin.alpha, thin.alpha, regular.alpha, thick.alpha, ultraThick.alpha)
        for (i in 0 until alphas.size - 1) {
            assertTrue(
                "Alpha at level $i (${alphas[i]}) must be strictly less than level ${i + 1} (${alphas[i + 1]})",
                alphas[i] < alphas[i + 1]
            )
        }

        // Distinctness check: All colors must be unique
        val colors = setOf(ultraThin, thin, regular, thick, ultraThick)
        assertEquals("All 5 Light mode material colors must be unique", 5, colors.size)
    }

    @Test
    fun challenge_allFiveThicknesses_DarkModeValuesAndStrictMonotonicity() {
        val ultraThin = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THIN, isDark = true)
        val thin = AppleMaterials.backgroundColor(MaterialThickness.THIN, isDark = true)
        val regular = AppleMaterials.backgroundColor(MaterialThickness.REGULAR, isDark = true)
        val thick = AppleMaterials.backgroundColor(MaterialThickness.THICK, isDark = true)
        val ultraThick = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK, isDark = true)

        // Exact ARGB checks
        assertEquals("Dark ULTRA_THIN should be 0x661C1C1E", Color(0x661C1C1E), ultraThin)
        assertEquals("Dark THIN should be 0x8C1C1C1E", Color(0x8C1C1C1E), thin)
        assertEquals("Dark REGULAR should be 0xD9161618", Color(0xD9161618), regular)
        assertEquals("Dark THICK should be 0xF21C1C1E", Color(0xF21C1C1E), thick)
        assertEquals("Dark ULTRA_THICK should be 0xFA121214", Color(0xFA121214), ultraThick)

        // Expected alpha ranges according to HIG
        assertEquals(0.40f, ultraThin.alpha, 0.01f) // ~40%
        assertEquals(0.55f, thin.alpha, 0.01f)      // ~55%
        assertEquals(0.85f, regular.alpha, 0.01f)   // ~85%
        assertEquals(0.95f, thick.alpha, 0.01f)     // ~95%
        assertEquals(0.98f, ultraThick.alpha, 0.01f) // ~98%

        // Strict monotonicity check: Each level MUST be strictly more opaque than previous
        val alphas = listOf(ultraThin.alpha, thin.alpha, regular.alpha, thick.alpha, ultraThick.alpha)
        for (i in 0 until alphas.size - 1) {
            assertTrue(
                "Alpha at level $i (${alphas[i]}) must be strictly less than level ${i + 1} (${alphas[i + 1]})",
                alphas[i] < alphas[i + 1]
            )
        }

        // Distinctness check: All colors must be unique
        val colors = setOf(ultraThin, thin, regular, thick, ultraThick)
        assertEquals("All 5 Dark mode material colors must be unique", 5, colors.size)
    }

    @Test
    fun challenge_barBackgroundColor_and_separatorColor_Contract() {
        val lightBar = AppleMaterials.barBackgroundColor(isDark = false)
        val darkBar = AppleMaterials.barBackgroundColor(isDark = true)

        // 93% translucency contract: 0xEE / 255f = 0.9333f
        assertEquals("Light bar should be 0xEEF2F2F7", Color(0xEEF2F2F7), lightBar)
        assertEquals("Dark bar should be 0xEE000000", Color(0xEE000000), darkBar)
        assertEquals(0.933f, lightBar.alpha, 0.01f)
        assertEquals(0.933f, darkBar.alpha, 0.01f)

        // Separator hairline colors
        val lightSep = AppleMaterials.separatorColor(isDark = false)
        val darkSep = AppleMaterials.separatorColor(isDark = true)
        assertEquals("Light separator should be 0x1F000000 (12% black)", Color(0x1F000000), lightSep)
        assertEquals("Dark separator should be 0x2EFFFFFF (18% white)", Color(0x2EFFFFFF), darkSep)
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
        assertEquals(0.60f, lightSecondary.alpha, 0.01f)
        assertEquals(0.30f, lightTertiary.alpha, 0.01f)
        assertEquals(0.18f, lightQuaternary.alpha, 0.01f)

        // Strict decreasing monotonicity
        assertTrue(lightPrimary.alpha > lightSecondary.alpha)
        assertTrue(lightSecondary.alpha > lightTertiary.alpha)
        assertTrue(lightTertiary.alpha > lightQuaternary.alpha)

        // Light mode defaults to Black base color
        assertEquals(0f, lightPrimary.red, 0.001f)
        assertEquals(0f, lightPrimary.green, 0.001f)
        assertEquals(0f, lightPrimary.blue, 0.001f)

        // Dark mode defaults to White base color
        val darkPrimary = AppleMaterials.vibrancyColor(VibrancyLevel.PRIMARY, isDark = true)
        val darkSecondary = AppleMaterials.vibrancyColor(VibrancyLevel.SECONDARY, isDark = true)
        val darkTertiary = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = true)
        val darkQuaternary = AppleMaterials.vibrancyColor(VibrancyLevel.QUATERNARY, isDark = true)

        assertEquals(1.0f, darkPrimary.alpha, 0.01f)
        assertEquals(0.60f, darkSecondary.alpha, 0.01f)
        assertEquals(0.30f, darkTertiary.alpha, 0.01f)
        assertEquals(0.18f, darkQuaternary.alpha, 0.01f)

        assertEquals(1f, darkPrimary.red, 0.001f)
        assertEquals(1f, darkPrimary.green, 0.001f)
        assertEquals(1f, darkPrimary.blue, 0.001f)
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
            assertEquals(0.60f, secondary.alpha, 0.01f)
            assertEquals(0.30f, tertiary.alpha, 0.01f)
            assertEquals(0.18f, quaternary.alpha, 0.01f)
            assertEquals(base.red, primary.red, 0.001f)
            assertEquals(base.green, primary.green, 0.001f)
            assertEquals(base.blue, primary.blue, 0.001f)

            // withVibrancy extension scales existing alpha
            val extPrimary = base.withVibrancy(VibrancyLevel.PRIMARY)
            val extSecondary = base.withVibrancy(VibrancyLevel.SECONDARY)
            val extTertiary = base.withVibrancy(VibrancyLevel.TERTIARY)
            val extQuaternary = base.withVibrancy(VibrancyLevel.QUATERNARY)

            assertEquals(base.alpha * 1.0f, extPrimary.alpha, 0.01f)
            assertEquals(base.alpha * 0.60f, extSecondary.alpha, 0.01f)
            assertEquals(base.alpha * 0.30f, extTertiary.alpha, 0.01f)
            assertEquals(base.alpha * 0.18f, extQuaternary.alpha, 0.01f)
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
    fun challenge_glassBorder_SpecularGradientStopsExtraction() {
        val lightBorder = AppleMaterials.glassBorder(isDark = false)
        val darkBorder = AppleMaterials.glassBorder(isDark = true)

        val lightBrush = lightBorder.brush
        val darkBrush = darkBorder.brush

        assertNotNull("Light border brush must not be null", lightBrush)
        assertNotNull("Dark border brush must not be null", darkBrush)

        // Extract colors from LinearGradient brush using reflection
        fun extractColors(brush: Brush): List<Color>? {
            return try {
                val field = brush.javaClass.declaredFields.firstOrNull { it.name == "colors" }
                if (field != null) {
                    field.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    field.get(brush) as? List<Color>
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        val lightColors = extractColors(lightBrush)
        assertNotNull("Light border gradient colors must be extractable from Brush", lightColors)
        assertEquals("Light glass border must have exactly 2 gradient stops (top highlight, bottom shadow)", 2, lightColors!!.size)
        // Top highlight: 60% white specular
        assertEquals("Light top specular stop must be 0x99FFFFFF", Color(0x99FFFFFF), lightColors[0])
        // Bottom shadow: 12% black contact shadow
        assertEquals("Light bottom shadow stop must be 0x1F000000", Color(0x1F000000), lightColors[1])

        val darkColors = extractColors(darkBrush)
        assertNotNull("Dark border gradient colors must be extractable from Brush", darkColors)
        assertEquals("Dark glass border must have exactly 2 gradient stops (top reflex, bottom reflex)", 2, darkColors!!.size)
        // Top reflex: 22% white
        assertEquals("Dark top specular stop must be 0x38FFFFFF", Color(0x38FFFFFF), darkColors[0])
        // Bottom reflex: 8% white
        assertEquals("Dark bottom specular stop must be 0x14FFFFFF", Color(0x14FFFFFF), darkColors[1])
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

        assertEquals("Unspecified light base should fallback to black", 0f, lightUnspecified.red, 0.001f)
        assertEquals("Unspecified dark base should fallback to white", 1f, darkUnspecified.red, 0.001f)
        assertEquals(0.60f, lightUnspecified.alpha, 0.01f)
        assertEquals(0.60f, darkUnspecified.alpha, 0.01f)

        // Passing Color.Transparent (0x00000000)
        val lightTransparent = AppleMaterials.vibrancyColor(VibrancyLevel.TERTIARY, isDark = false, baseColor = Color.Transparent)
        assertEquals("Transparent base with tertiary vibrancy should have 0.30 alpha", 0.30f, lightTransparent.alpha, 0.01f)
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

