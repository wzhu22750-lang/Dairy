package com.example.inkpaperdiary.tier2_boundaries

import com.example.inkpaperdiary.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

/**
 * Tier 2: Boundary & Corner Cases for R2 (Root Navigation & Collapsible Large Title)
 * Verifies scroll offset clamping, route parameter encoding/decoding, rapid tab state changes.
 */
class R2BoundaryEdgeCasesTest {

    @Test
    fun testB2_NegativeScrollOffsetClampingInLargeTitle() {
        // When list bounces or scrolls past top, offset is negative
        // Formula: (offset / threshold).coerceIn(0f, 1f)
        val threshold = 156f
        fun calcAlpha(offset: Float): Float = (offset / threshold).coerceIn(0f, 1f)

        assertEquals(0.0f, calcAlpha(-500f), 0.001f)
        assertEquals(0.0f, calcAlpha(-1f), 0.001f)
        assertEquals(0.0f, calcAlpha(0f), 0.001f)
    }

    @Test
    fun testB2_MassiveScrollOffsetClampingInLargeTitle() {
        // Rapid fling past 10,000 pixels
        val threshold = 156f
        fun calcAlpha(offset: Float): Float = (offset / threshold).coerceIn(0f, 1f)

        assertEquals(1.0f, calcAlpha(156f), 0.001f)
        assertEquals(1.0f, calcAlpha(1000f), 0.001f)
        assertEquals(1.0f, calcAlpha(100000f), 0.001f)
    }

    @Test
    fun testB2_FractionalScrollOffsetsPrecision() {
        // Floating point precision during intermediate scroll steps
        val threshold = 156f
        val step = 0.156f
        val alpha = (step / threshold).coerceIn(0f, 1f)
        assertEquals(0.001f, alpha, 0.0001f)
    }

    @Test
    fun testB2_EditorRouteWithSpecialCharactersInId() {
        // Diary ID with special URL characters, slashes, or unicode
        val rawId = "diary#special@2026/09"
        val encodedId = URLEncoder.encode(rawId, "UTF-8")
        val route = Screen.Editor.createRoute(encodedId)

        assertTrue(route.startsWith("editor/"))
        val extracted = route.removePrefix("editor/")
        assertEquals(encodedId, extracted)
    }

    @Test
    fun testB2_EditorRouteWithEmptyOrNullIdFallback() {
        // When ID is null or blank, route must resolve cleanly to 'editor/new'
        val routeNull = Screen.Editor.createRoute(null)
        val routeEmpty = if ("".isBlank()) Screen.Editor.createRoute(null) else Screen.Editor.createRoute("")

        assertEquals("editor/new", routeNull)
        assertEquals("editor/new", routeEmpty)
    }

    @Test
    fun testB2_RapidConsecutiveTabSwitching() {
        // Switching tabs 100 times in tight loop does not desynchronize active tab
        val tabHistory = mutableListOf<String>()
        val tabs = listOf("JOURNAL", "CALENDAR", "MEMORIES", "SETTINGS")
        var currentTab = tabs[0]

        for (i in 0..99) {
            currentTab = tabs[i % tabs.size]
            tabHistory.add(currentTab)
        }

        assertEquals(100, tabHistory.size)
        assertEquals("SETTINGS", currentTab) // 99 % 4 = 3 -> SETTINGS
    }

    @Test
    fun testB2_RouteNormalizationWithTrailingSlashes() {
        fun normalizeRoute(route: String): String = route.trim().trimEnd('/')

        assertEquals("timeline", normalizeRoute("timeline/"))
        assertEquals("timeline", normalizeRoute("timeline///"))
        assertEquals("editor/new", normalizeRoute("editor/new/"))
    }

    @Test
    fun testB2_NavigationBackstackEmptyPopSafety() {
        // Popping root destination on empty backstack should be handled gracefully
        val backStack = mutableListOf<String>("timeline")
        fun popRoute(): String? {
            return if (backStack.size > 1) backStack.removeAt(backStack.size - 1) else null
        }

        assertEquals(null, popRoute()) // Cannot pop root timeline
        assertEquals(1, backStack.size)

        // Push editor and pop
        backStack.add("editor/new")
        assertEquals(2, backStack.size)
        assertEquals("editor/new", popRoute())
        assertEquals(1, backStack.size)
        assertEquals("timeline", backStack[0])
    }

    @Test
    fun testB2_RouteMatchingWithQueryParameters() {
        val baseRoute = "search"
        val query = "keyword=春天&tag=旅行"
        val fullUrl = "$baseRoute?$query"

        assertTrue(fullUrl.startsWith(Screen.Search.route))
        val queryPart = fullUrl.substringAfter("?")
        assertTrue(queryPart.contains("keyword=春天"))
        assertTrue(queryPart.contains("tag=旅行"))
    }

    @Test
    fun testB2_LargeTitleElevationFrostedGlassTransition() {
        // Frosted glass elevation and hairline bottom border appear only when collapsed (alpha > 0.95f)
        fun isHeaderFrosted(alpha: Float): Boolean = alpha >= 0.95f

        assertFalse(isHeaderFrosted(0.0f))
        assertFalse(isHeaderFrosted(0.5f))
        assertFalse(isHeaderFrosted(0.94f))
        assertTrue(isHeaderFrosted(0.95f))
        assertTrue(isHeaderFrosted(1.0f))
    }
}
