package com.example.inkpaperdiary.challenger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleDefaults
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleScrollState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Empirical Challenger Suite for Milestone M2:
 * `IosLargeTitleScaffold` dynamic scroll physics, density scaling, boundary math,
 * and interpolation monotonicity.
 */
class IosLargeTitleEmpiricalChallengeTest {

    // =========================================================================================
    // 1. HIG Architectural Constants Verification
    // =========================================================================================

    @Test
    fun challenge_constants_matchAppleHigSpecifications() {
        assertEquals("Collapse threshold must be exactly 52dp", 52.dp, IosLargeTitleDefaults.CollapseThresholdDp)
        assertEquals("TopBar height must be 44dp", 44.dp, IosLargeTitleDefaults.TopBarHeight)
        assertEquals("Hairline border width must be 0.5dp", 0.5.dp, IosLargeTitleDefaults.HairlineBorderWidth)
        assertEquals(
            "Frosted glass alpha threshold must be 0.95f",
            0.95f,
            IosLargeTitleDefaults.FROSTED_GLASS_ALPHA_THRESHOLD,
            0.0001f
        )
    }

    // =========================================================================================
    // 2. Dynamic 52dp Threshold Under Varying Screen Densities (1.0x to 4.0x)
    // =========================================================================================

    @Test
    fun challenge_dynamicThreshold_varyingDensitiesScaling() {
        val testDensities = listOf(
            1.0f to 52.0f,     // mdpi / standard 1.0x
            1.5f to 78.0f,     // hdpi
            2.0f to 104.0f,    // xhdpi (Retina @2x)
            2.625f to 136.5f,  // Pixel / 420dpi devices
            3.0f to 156.0f,    // xxhdpi (Super Retina @3x)
            3.5f to 182.0f,    // Quad HD
            4.0f to 208.0f     // xxxhdpi 4.0x
        )

        for ((densityVal, expectedPx) in testDensities) {
            val density = Density(density = densityVal)
            val thresholdPx = with(density) { IosLargeTitleDefaults.CollapseThresholdDp.toPx() }

            assertEquals(
                "Threshold px for density $densityVal must be $expectedPx",
                expectedPx,
                thresholdPx,
                0.001f
            )

            // Verify alpha progression is invariant to density when offset scales proportionally
            val halfOffset = thresholdPx * 0.5f
            val alphaHalf = IosLargeTitleDefaults.calculateInlineTitleAlpha(halfOffset, thresholdPx)
            assertEquals("Alpha at 50% scroll must be 0.5f regardless of density ($densityVal)", 0.5f, alphaHalf, 0.0001f)

            val fullOffset = thresholdPx
            val alphaFull = IosLargeTitleDefaults.calculateInlineTitleAlpha(fullOffset, thresholdPx)
            assertEquals("Alpha at 100% scroll must be 1.0f regardless of density ($densityVal)", 1.0f, alphaFull, 0.0001f)

            val zeroOffset = 0f
            val alphaZero = IosLargeTitleDefaults.calculateInlineTitleAlpha(zeroOffset, thresholdPx)
            assertEquals("Alpha at 0 scroll must be 0.0f regardless of density ($densityVal)", 0.0f, alphaZero, 0.0001f)
        }
    }

    // =========================================================================================
    // 3. Negative Scroll Offsets (Overscroll / Rubber-banding / Bounce Clamping to 0.0f)
    // =========================================================================================

    @Test
    fun challenge_negativeScrollOffsets_clampingToZero() {
        val thresholdPx = 156.0f // 52dp @ 3.0x density

        val negativeOffsets = listOf(
            -0.00001f,
            -0.5f,
            -1.0f,
            -10.0f,
            -52.0f,
            -156.0f,
            -1000.0f,
            -1_000_000.0f,
            Float.NEGATIVE_INFINITY
        )

        for (negativeOffset in negativeOffsets) {
            val inlineAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(negativeOffset, thresholdPx)
            val largeAlpha = IosLargeTitleDefaults.calculateLargeTitleAlpha(negativeOffset, thresholdPx)

            assertEquals(
                "Negative offset $negativeOffset must clamp inline title alpha strictly to 0.0f",
                0.0f,
                inlineAlpha,
                0.0f
            )
            assertEquals(
                "Negative offset $negativeOffset must clamp large title alpha strictly to 1.0f",
                1.0f,
                largeAlpha,
                0.0f
            )
            assertFalse(
                "Negative offset $negativeOffset must never trigger frosted glass elevation",
                IosLargeTitleDefaults.isHeaderFrosted(inlineAlpha)
            )
        }
    }

    // =========================================================================================
    // 4. Extremely Large Scroll Offsets (Clamping to 1.0f)
    // =========================================================================================

    @Test
    fun challenge_extremelyLargeScrollOffsets_clampingToOne() {
        val thresholdPx = 156.0f

        val extremeOffsets = listOf(
            156.0001f,
            157.0f,
            200.0f,
            500.0f,
            1000.0f,
            50_000.0f,
            1_000_000.0f,
            100_000_000.0f,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY
        )

        for (offset in extremeOffsets) {
            val inlineAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(offset, thresholdPx)
            val largeAlpha = IosLargeTitleDefaults.calculateLargeTitleAlpha(offset, thresholdPx)

            assertEquals(
                "Extreme offset $offset must clamp inline title alpha strictly to 1.0f",
                1.0f,
                inlineAlpha,
                0.0f
            )
            assertEquals(
                "Extreme offset $offset must clamp large title alpha strictly to 0.0f",
                0.0f,
                largeAlpha,
                0.0f
            )
            assertTrue(
                "Extreme offset $offset must trigger frosted glass elevation",
                IosLargeTitleDefaults.isHeaderFrosted(inlineAlpha)
            )
        }
    }

    // =========================================================================================
    // 5. Monotonicity & Floating-Point Precision across [0, 52dp]
    // =========================================================================================

    @Test
    fun challenge_interpolationMonotonicity_acrossScrollRange() {
        val testDensities = listOf(1.0f, 2.0f, 2.625f, 3.0f, 4.0f)

        for (densityVal in testDensities) {
            val density = Density(density = densityVal)
            val thresholdPx = with(density) { IosLargeTitleDefaults.CollapseThresholdDp.toPx() }

            val totalSteps = 2000
            val stepSize = thresholdPx / totalSteps

            var previousInlineAlpha = -1.0f
            var previousLargeAlpha = 2.0f

            for (i in 0..totalSteps) {
                val scrollOffset = i * stepSize
                val inlineAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(scrollOffset, thresholdPx)
                val largeAlpha = IosLargeTitleDefaults.calculateLargeTitleAlpha(scrollOffset, thresholdPx)

                // 1. Within valid [0.0, 1.0] bounds
                assertTrue("Inline alpha at step $i ($inlineAlpha) must be >= 0.0f", inlineAlpha >= 0.0f)
                assertTrue("Inline alpha at step $i ($inlineAlpha) must be <= 1.0f", inlineAlpha <= 1.0f)
                assertTrue("Large alpha at step $i ($largeAlpha) must be >= 0.0f", largeAlpha >= 0.0f)
                assertTrue("Large alpha at step $i ($largeAlpha) must be <= 1.0f", largeAlpha <= 1.0f)

                // 2. Strict non-decreasing monotonicity for inline title
                assertTrue(
                    "Inline alpha must be monotonic non-decreasing at step $i: prev=$previousInlineAlpha, current=$inlineAlpha",
                    inlineAlpha >= previousInlineAlpha
                )

                // 3. Strict non-increasing monotonicity for large title
                assertTrue(
                    "Large alpha must be monotonic non-increasing at step $i: prev=$previousLargeAlpha, current=$largeAlpha",
                    largeAlpha <= previousLargeAlpha
                )

                // 4. Complementary crossfade identity: inlineAlpha + largeAlpha == 1.0f
                val sum = inlineAlpha + largeAlpha
                assertEquals(
                    "Sum of crossfading alphas at offset $scrollOffset must equal 1.0f",
                    1.0f,
                    sum,
                    0.0001f
                )

                previousInlineAlpha = inlineAlpha
                previousLargeAlpha = largeAlpha
            }
        }
    }

    // =========================================================================================
    // 6. Crossfade Symmetry & Exact Midpoint Hand-off
    // =========================================================================================

    @Test
    fun challenge_crossfadeMidpointSymmetry() {
        val thresholdPx = 156.0f
        val midpoint = thresholdPx * 0.5f

        val inlineAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(midpoint, thresholdPx)
        val largeAlpha = IosLargeTitleDefaults.calculateLargeTitleAlpha(midpoint, thresholdPx)

        assertEquals("Inline alpha at midpoint must be exactly 0.5f", 0.5f, inlineAlpha, 0.0001f)
        assertEquals("Large title alpha at midpoint must be exactly 0.5f", 0.5f, largeAlpha, 0.0001f)
        assertEquals("Alphas must be identical at midpoint", inlineAlpha, largeAlpha, 0.0001f)
    }

    // =========================================================================================
    // 7. Frosted Glass Elevation Trigger (Threshold = 0.95f)
    // =========================================================================================

    @Test
    fun challenge_frostedGlassElevationTrigger_boundaryStressTest() {
        val thresholdPx = 100.0f // Normalized threshold

        // Test boundary values
        val triggerOffset = thresholdPx * 0.95f // 95.0f

        // 1. Immediately below trigger point
        val belowOffset = triggerOffset - 0.01f
        val belowAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(belowOffset, thresholdPx)
        assertFalse(
            "Below 0.95 threshold (alpha=$belowAlpha), frosted glass must NOT be active",
            IosLargeTitleDefaults.isHeaderFrosted(belowAlpha)
        )

        // 2. Exactly at trigger point
        val exactAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(triggerOffset, thresholdPx)
        assertTrue(
            "At exactly 0.95 threshold (alpha=$exactAlpha), frosted glass MUST be active",
            IosLargeTitleDefaults.isHeaderFrosted(exactAlpha)
        )

        // 3. Immediately above trigger point
        val aboveOffset = triggerOffset + 0.01f
        val aboveAlpha = IosLargeTitleDefaults.calculateInlineTitleAlpha(aboveOffset, thresholdPx)
        assertTrue(
            "Above 0.95 threshold (alpha=$aboveAlpha), frosted glass MUST be active",
            IosLargeTitleDefaults.isHeaderFrosted(aboveAlpha)
        )

        // 4. Boundary values for isHeaderFrosted directly
        assertFalse("alpha = 0.0f", IosLargeTitleDefaults.isHeaderFrosted(0.0f))
        assertFalse("alpha = 0.50f", IosLargeTitleDefaults.isHeaderFrosted(0.50f))
        assertFalse("alpha = 0.94999f", IosLargeTitleDefaults.isHeaderFrosted(0.94999f))
        assertTrue("alpha = 0.95000f", IosLargeTitleDefaults.isHeaderFrosted(0.95000f))
        assertTrue("alpha = 0.95001f", IosLargeTitleDefaults.isHeaderFrosted(0.95001f))
        assertTrue("alpha = 1.0f", IosLargeTitleDefaults.isHeaderFrosted(1.0f))
        assertTrue("alpha = 1.5f (coerced)", IosLargeTitleDefaults.isHeaderFrosted(1.5f))
        assertFalse("alpha = -0.1f", IosLargeTitleDefaults.isHeaderFrosted(-0.1f))
    }

    // =========================================================================================
    // 8. Zero and Negative Threshold Robustness (Division-by-Zero Safety)
    // =========================================================================================

    @Test
    fun challenge_zeroAndNegativeThreshold_safety() {
        // Zero threshold
        val zeroInline = IosLargeTitleDefaults.calculateInlineTitleAlpha(50f, 0f)
        val zeroLarge = IosLargeTitleDefaults.calculateLargeTitleAlpha(50f, 0f)
        assertEquals("Zero threshold must safely fallback inline title alpha to 1.0f", 1.0f, zeroInline, 0.0f)
        assertEquals("Zero threshold must safely fallback large title alpha to 0.0f", 0.0f, zeroLarge, 0.0f)

        // Negative threshold
        val negInline = IosLargeTitleDefaults.calculateInlineTitleAlpha(50f, -100f)
        val negLarge = IosLargeTitleDefaults.calculateLargeTitleAlpha(50f, -100f)
        assertEquals("Negative threshold must safely fallback inline title alpha to 1.0f", 1.0f, negInline, 0.0f)
        assertEquals("Negative threshold must safely fallback large title alpha to 0.0f", 0.0f, negLarge, 0.0f)
    }

    // =========================================================================================
    // 9. IosLargeTitleScrollState Dynamic Simulation
    // =========================================================================================

    @Test
    fun challenge_scrollState_preScrollAndPostScrollSimulation() {
        val thresholdPx = 156f
        val state = IosLargeTitleScrollState(thresholdPx = thresholdPx)

        assertEquals(0f, state.scrollOffset, 0.0001f)
        assertEquals(0f, state.progress, 0.0001f)

        // 1. PreScroll: Scrolling up by 50px (available.y = -50f, delta = 50f)
        val consumed1 = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -50f),
            source = NestedScrollSource.UserInput
        )
        assertEquals("Must consume 50px of pre-scroll", -50f, consumed1.y, 0.001f)
        assertEquals("ScrollOffset must now be 50f", 50f, state.scrollOffset, 0.001f)
        assertEquals("Progress must be 50/156", 50f / 156f, state.progress, 0.001f)

        // 2. PreScroll: Scroll past threshold by another 120px (total requested delta = 170px)
        val consumed2 = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -120f),
            source = NestedScrollSource.UserInput
        )
        // delta = 120, prev = 50, new offset = min(170, 312) = 170
        assertEquals("Must consume 120px up to 2*threshold", -120f, consumed2.y, 0.001f)
        assertEquals(170f, state.scrollOffset, 0.001f)
        // Progress should be clamped to 1.0f
        assertEquals("Progress past threshold must clamp to 1.0f", 1.0f, state.progress, 0.0001f)

        // 3. PreScroll: When already past thresholdPx (scrollOffset >= 156f),
        // onPreScroll should NOT consume further scroll (allowing list to scroll)
        val consumed3 = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -50f),
            source = NestedScrollSource.UserInput
        )
        assertEquals("PreScroll past threshold must not consume delta", Offset.Zero, consumed3)

        // 4. PostScroll: Scrolling back down (available.y = 100f, delta = -100f)
        val consumedPost1 = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 100f),
            source = NestedScrollSource.UserInput
        )
        assertEquals("PostScroll must consume unwind delta", 100f, consumedPost1.y, 0.001f)
        assertEquals(70f, state.scrollOffset, 0.001f)
        assertEquals(70f / 156f, state.progress, 0.001f)

        // 5. PostScroll: Scroll down past zero (overscroll / bounce)
        val consumedPost2 = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 200f),
            source = NestedScrollSource.UserInput
        )
        // previous was 70, delta = -200 -> coerced to 0f, consumed = 0 - 70 = -70
        assertEquals("PostScroll must clamp to zero and consume only remaining 70px", 70f, consumedPost2.y, 0.001f)
        assertEquals("ScrollOffset must be strictly 0f", 0f, state.scrollOffset, 0.0f)
        assertEquals("Progress must be strictly 0f", 0f, state.progress, 0.0f)
    }

    // =========================================================================================
    // 10. Rapid Alternating Scroll Direction Jitter Stress Test
    // =========================================================================================

    @Test
    fun challenge_rapidScrollDirectionJitterStress() {
        val thresholdPx = 156f
        val state = IosLargeTitleScrollState(thresholdPx = thresholdPx)

        // Simulate 1000 jittery scroll events
        for (i in 1..1000) {
            val direction = if (i % 2 == 0) -1f else 1f
            val delta = direction * (i % 15).toFloat()

            if (delta < 0) { // scrolling up
                state.nestedScrollConnection.onPreScroll(
                    available = Offset(0f, delta),
                    source = NestedScrollSource.UserInput
                )
            } else { // scrolling down
                state.nestedScrollConnection.onPostScroll(
                    consumed = Offset.Zero,
                    available = Offset(0f, delta),
                    source = NestedScrollSource.UserInput
                )
            }

            // Invariants must hold at every single iteration
            assertTrue("Scroll offset must be >= 0f", state.scrollOffset >= 0f)
            assertTrue("Scroll offset must be <= 2 * threshold", state.scrollOffset <= thresholdPx * 2f)
            assertTrue("Progress must be >= 0f", state.progress >= 0f)
            assertTrue("Progress must be <= 1f", state.progress <= 1f)
        }
    }
}
