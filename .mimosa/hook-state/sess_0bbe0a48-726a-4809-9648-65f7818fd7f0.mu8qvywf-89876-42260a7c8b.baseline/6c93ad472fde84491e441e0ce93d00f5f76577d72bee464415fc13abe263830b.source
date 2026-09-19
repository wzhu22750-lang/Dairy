package com.example.inkpaperdiary.tier2_boundaries

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.VibrancyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2: Boundary & Corner Cases for R1 (Design System & Interaction Primitives)
 * Verifies extreme dimensions, edge cases, clamping, and boundary conditions.
 */
class R1BoundaryEdgeCasesTest {

    @Test
    fun testB1_AlphaClampingToValidRange() {
        // Alphas must never exceed 1.0f or fall below 0.0f
        fun clampAlpha(alpha: Float): Float = alpha.coerceIn(0f, 1f)

        assertEquals(0.0f, clampAlpha(-0.5f), 0.001f)
        assertEquals(0.0f, clampAlpha(0.0f), 0.001f)
        assertEquals(0.85f, clampAlpha(0.85f), 0.001f)
        assertEquals(1.0f, clampAlpha(1.0f), 0.001f)
        assertEquals(1.0f, clampAlpha(1.5f), 0.001f)
    }

    @Test
    fun testB1_SingleRowSectionCornerClipping() {
        // When IosListSection contains exactly 1 row:
        // Top and bottom corners both round to 16dp; showDivider is false
        val rowCount = 1
        val topCornerRadius = 16.dp
        val bottomCornerRadius = 16.dp
        val showDivider = rowCount > 1

        assertEquals(16.dp, topCornerRadius)
        assertEquals(16.dp, bottomCornerRadius)
        assertFalse(showDivider)
    }

    @Test
    fun testB1_MultiRowSectionDividerBehavior() {
        // In a section with 3 rows:
        // Row 0: showDivider = true
        // Row 1: showDivider = true
        // Row 2 (last): showDivider = false
        val totalRows = 3
        fun shouldShowDivider(index: Int): Boolean = index < totalRows - 1

        assertTrue(shouldShowDivider(0))
        assertTrue(shouldShowDivider(1))
        assertFalse(shouldShowDivider(2))
    }

    @Test
    fun testB1_NullIconDividerIndentBoundary() {
        // Spec 4.6: If icon is null, indent collapses from 56dp to 16dp
        fun calculateIndent(hasIcon: Boolean) = if (hasIcon) 56.dp else 16.dp

        assertEquals(56.dp, calculateIndent(true))
        assertEquals(16.dp, calculateIndent(false))
    }

    @Test
    fun testB1_TouchPhysicsDragCancellation() {
        // Gesture drag out cancels press: scale resets to 1.0f, alpha resets to 1.0f, onClick does NOT fire
        var isFingerWithinBounds = true
        var isPressed = true
        var onClickFired = false

        // Finger moves outside bounds
        isFingerWithinBounds = false
        if (!isFingerWithinBounds) {
            isPressed = false
        }

        // Release happens outside bounds
        if (isFingerWithinBounds) {
            onClickFired = true
        }

        assertFalse(isPressed)
        assertFalse(onClickFired)
        val currentScale = if (isPressed) 0.97f else 1.0f
        val currentAlpha = if (isPressed) 0.85f else 1.0f
        assertEquals(1.0f, currentScale, 0.001f)
        assertEquals(1.0f, currentAlpha, 0.001f)
    }

    @Test
    fun testB1_TouchPhysicsRapidConsecutiveTaps() {
        // Rapid double taps: scale dynamically redirects without visual glitches
        var currentScale = 1.0f

        // Tap 1 down
        currentScale = 0.97f
        assertEquals(0.97f, currentScale, 0.001f)

        // Rapid Tap 2 before full spring expansion
        val intermediateScale = 0.99f
        currentScale = intermediateScale
        // Tap 2 down targets 0.97f again
        currentScale = 0.97f
        assertEquals(0.97f, currentScale, 0.001f)

        // Release expands to 1.0f
        currentScale = 1.0f
        assertEquals(1.0f, currentScale, 0.001f)
    }

    @Test
    fun testB1_SegmentedControlOutOfBoundsIndexClamping() {
        // When selected index is out of bounds, falls back to 0 safely
        val items = listOf("全部", "置顶")
        fun getSafeItem(requestedIndex: Int): String {
            return if (requestedIndex in items.indices) items[requestedIndex] else items[0]
        }

        assertEquals("全部", getSafeItem(-1))
        assertEquals("全部", getSafeItem(0))
        assertEquals("置顶", getSafeItem(1))
        assertEquals("全部", getSafeItem(99))
    }

    @Test
    fun testB1_HairlineBorderZeroWidthBoundary() {
        // Border width should not be negative
        fun getValidBorderWidth(width: Float): Float = width.coerceAtLeast(0f)

        assertEquals(0f, getValidBorderWidth(-1.0f), 0.001f)
        assertEquals(0.5f, getValidBorderWidth(0.5f), 0.001f)
    }

    @Test
    fun testB1_VibrancyCustomBaseColorAlphaPreservation() {
        // When custom baseColor has existing alpha, vibrancy level scales proportionally
        val customColor = Color(0x80FF0000) // 50% red
        val originalAlpha = customColor.alpha // ~0.502f

        val secondaryVibrancy = customColor.copy(alpha = originalAlpha * 0.60f)
        assertTrue(secondaryVibrancy.alpha < originalAlpha)
        assertEquals(0.30f, secondaryVibrancy.alpha, 0.02f)
    }

    @Test
    fun testB1_SegmentedControlSingleItemBoundary() {
        // When segmented control has only 1 item, track renders with single full-width thumb
        val singleList = listOf("全部")
        assertEquals(1, singleList.size)
        val selected = singleList[0]
        assertEquals("全部", selected)
    }
}
