# Technical Investigation & Blueprint: IosLargeTitleScaffold & IosLargeTitleTopBar

## 1. Executive Summary

This report delivers the complete architectural specification and drop-in code blueprint for `IosLargeTitleScaffold.kt` in `com.example.inkpaperdiary.core.designsystem.scaffold`. 

In Milestone 2 (Root Navigation & Collapsible Large Title), the application transitions away from Android/Material 3 top app bars and floating action buttons (FAB) toward Apple Human Interface Guidelines (HIG) navigation structures. The core challenge is delivering a fluid, scroll-coupled header that:
1. Expands at rest to display an authentic **34sp Bold Large Title** (with optional uppercase category subtitle).
2. Smoothly transitions into a centered **17sp SemiBold inline title** as the user scrolls past **52dp** of content offset.
3. Automatically elevates with **frosted glass material** (`AppleMaterials.barBackgroundColor`, 93% translucency) and a **0.5dp hairline bottom border** (`AppleMaterials.separatorColor`) when collapsed (`progress >= 0.95f`).
4. Accommodates iOS-style navigation actions (leading back/dismiss button and trailing action icons/text buttons) with **tactile spring touch physics** (`Modifier.iosIconClick` / `Modifier.iosClick`) and **zero Material ink ripples**.
5. Seamlessly couples with both `LazyListState` (`LazyColumn`) and `ScrollState` (`Column.verticalScroll`), providing window insets handling for edge-to-edge layouts.

---

## 2. Codebase Investigation Findings

### 2.1 Current File State: `IosLargeTitleScaffold.kt`
- **Path**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`
- **Lines**: 187 lines currently present.
- **Current Exports**:
  - `IosLargeTitleTopBar(...)`: Top bar with hardcoded `scrollThresholdPx: Float = 120f`.
  - `IosLargeTitleItem(...)`: Large title item (34sp Bold), but missing scroll-coupled fade-out alpha.
  - `rememberLazyListScrollOffset(...)`: Clamps offset to `140f`, preventing full collapse threshold reach.
  - `rememberScrollStateOffset(...)`: Clamps offset to `140f`.
- **Critical Missing Component**: `IosLargeTitleScaffold` composable is **completely absent** despite the filename and specification in `PROJECT.md` line 103–113!

### 2.2 Screen Integration Status
1. **`TimelineScreen.kt`**:
   - Manually composes `Box` + `LazyColumn(state = listState)` + `IosLargeTitleTopBar`.
   - Embeds `IosLargeTitleItem("日记", currentDateStr)` as the first item in the list.
   - Embeds top-right actions (Search and New Diary) with `Modifier.iosIconClick`.
2. **`SettingsScreen.kt`**:
   - Manually composes `Box` + `Column(Modifier.verticalScroll(scrollState))` + `IosLargeTitleTopBar`.
   - Embeds `IosLargeTitleItem("设置")`.
   - Uses `rememberScrollStateOffset(scrollState)`.
3. **`CalendarScreen.kt`, `OnThisDayScreen.kt`, `TrashScreen.kt`, `StatsScreen.kt`**:
   - Currently still rely on Android Material 3 `Scaffold` and `TopAppBar`.
   - Need to be refactored to use `IosLargeTitleScaffold` with iOS navigation buttons.

### 2.3 Test Suite & Contract Analysis
From `app/src/test/java/com/example/inkpaperdiary/tier1_features/R2NavigationFeatureTest.kt` and `app/src/test/java/com/example/inkpaperdiary/tier2_boundaries/R2BoundaryEdgeCasesTest.kt`:
1. **Collapse Threshold**:
   - `val collapseThreshold = 52.dp` (line 120 of `R2NavigationFeatureTest.kt`).
   - At 3.0 density, `52dp * 3.0 = 156f px`.
2. **Title Crossfade Interpolation Formula**:
   - Inline Title Alpha: `(scrollOffsetPx / thresholdPx).coerceIn(0f, 1f)` (lines 124–133).
   - Large Title Alpha: `(1f - (scrollOffsetPx / thresholdPx)).coerceIn(0f, 1f)` (lines 138–147).
3. **Typography**:
   - Expanded Large Title: `34.sp`, `FontWeight.Bold`, `lineHeight = 41.sp`, `letterSpacing = (-0.4).sp` (or `0.37.sp`).
   - Collapsed Inline Title: `17.sp`, `FontWeight.SemiBold`, `lineHeight = 22.sp`, `letterSpacing = (-0.4).sp`, centered in top bar.
4. **Frosted Glass Elevation Transition**:
   - Header is considered frosted when `alpha >= 0.95f` (`testB2_LargeTitleElevationFrostedGlassTransition`).
   - Background has 93% translucency (`Color(0xEEF2F2F7)` Light / `Color(0xEE000000)` Dark), matching `AppleMaterials.barBackgroundColor`.
5. **Hairline Bottom Border**:
   - Thickness: `0.5.dp`, colored with `AppleMaterials.separatorColor(isDark)` and animated with progress.
6. **Top Bar Geometry**:
   - Inline bar content height: `44.dp` (`testF6_TopInlineNavBarHeight`).
   - Status bar padding: `Modifier.windowInsetsPadding(WindowInsets.statusBars)`.

---

## 3. Defects in Current Implementation & Required Fixes

| Issue | Current Behavior | Required Fix |
|-------|------------------|--------------|
| **Missing Scaffold** | No `IosLargeTitleScaffold` function exists in `IosLargeTitleScaffold.kt`. | Implement overloaded `IosLargeTitleScaffold` accepting `ScrollState`, `LazyListState`, and raw offset. |
| **Hardcoded Threshold** | `scrollThresholdPx = 120f` is hardcoded regardless of screen density. | Calculate dynamically: `with(LocalDensity.current) { 52.dp.toPx() }` (e.g. 156px on 3x density). |
| **Max Offset Capping** | `rememberLazyListScrollOffset` and `rememberScrollStateOffset` cap offset at `140f`. On 3x devices, 140/156 = 0.897, so the header NEVER reaches full collapse (progress < 0.95)! | Remove `140f` clamp. For `LazyListState`, if `firstVisibleItemIndex > 0`, return `>= thresholdPx * 2f`. |
| **No Large Title Fade** | `IosLargeTitleItem` has static color opacity (no fade-out on scroll). | Add `scrollOffset` and `scrollThresholdPx` parameters, applying `(1f - progress).coerceIn(0f, 1f)` via `graphicsLayer { alpha = ... }`. |
| **Inline Title Collision** | Inline title padding `horizontal = 64.dp` may clip or collide with dual action icons (36+36+4 = 76dp). | Increase inline title horizontal margin to `72.dp` or calculate based on action count. |
| **Action Button Boilerplate** | Screens duplicate 36dp `Box` + `iosIconClick` + `Icon` logic. | Provide standardized `IosNavIconButton`, `IosNavTextButton`, and `IosNavBackButton` helpers. |

---

## 4. Complete Drop-In Code Blueprint

The following implementation completely replaces `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`.

```kotlin
package com.example.inkpaperdiary.core.designsystem.scaffold

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick

/**
 * Apple Human Interface Guidelines (HIG) Large Title Design Constants and Formulas.
 */
object IosLargeTitleDefaults {
    val CollapseThresholdDp: Dp = 52.dp
    val TopBarHeight: Dp = 44.dp
    val HairlineBorderWidth: Dp = 0.5.dp
    const val FROSTED_GLASS_ALPHA_THRESHOLD: Float = 0.95f

    /**
     * Compute inline title alpha from scroll offset and threshold in pixels.
     * Formula: (scrollOffset / thresholdPx).coerceIn(0f, 1f)
     */
    fun calculateInlineTitleAlpha(scrollOffsetPx: Float, thresholdPx: Float): Float {
        if (thresholdPx <= 0f) return 1f
        return (scrollOffsetPx / thresholdPx).coerceIn(0f, 1f)
    }

    /**
     * Compute large title alpha from scroll offset and threshold in pixels.
     * Formula: (1f - (scrollOffset / thresholdPx)).coerceIn(0f, 1f)
     */
    fun calculateLargeTitleAlpha(scrollOffsetPx: Float, thresholdPx: Float): Float {
        if (thresholdPx <= 0f) return 0f
        return (1f - (scrollOffsetPx / thresholdPx)).coerceIn(0f, 1f)
    }

    /**
     * Determine whether header frosted glass elevation is active.
     */
    fun isHeaderFrosted(alpha: Float): Boolean = alpha >= FROSTED_GLASS_ALPHA_THRESHOLD
}

/**
 * Primary Apple HIG Large Title Scaffold for Jetpack Compose.
 *
 * Implements a 2-tier scrolling hierarchy:
 * - Pinned frosted glass top navigation bar at the top edge (44dp + status bar insets).
 * - Underlying content viewport that scrolls underneath the top bar.
 * - Dynamic scroll coupling driving the 34sp -> 17sp title transition and frosted elevation.
 *
 * @param title The canonical screen title (e.g. "日记", "设置").
 * @param scrollState ScrollState of a scrollable Column.
 * @param navigationIcon Optional leading action (typically back button or modal dismiss).
 * @param actions Optional trailing action buttons (icons or text buttons).
 * @param modifier Outer container modifier.
 * @param bottomBar Optional bottom navigation bar (e.g. IosTabBar).
 * @param snackbarHost Optional snackbar host slot.
 * @param containerColor Background color of the scaffold.
 * @param contentColor Default text/icon content color.
 * @param scrollThreshold Dp offset required to fully collapse the title (default: 52dp).
 * @param content Screen content lambda receiving inner padding values for edge-to-edge alignment.
 */
@Composable
fun IosLargeTitleScaffold(
    title: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    scrollThreshold: Dp = IosLargeTitleDefaults.CollapseThresholdDp,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollOffset = rememberScrollStateOffset(scrollState)
    IosLargeTitleScaffold(
        title = title,
        scrollOffset = scrollOffset,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        scrollThreshold = scrollThreshold,
        content = content
    )
}

/**
 * Overload for LazyColumn / LazyListState containers.
 */
@Composable
fun IosLargeTitleScaffold(
    title: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    scrollThreshold: Dp = IosLargeTitleDefaults.CollapseThresholdDp,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = remember(density, scrollThreshold) {
        with(density) { scrollThreshold.toPx() }
    }
    val scrollOffset = rememberLazyListScrollOffset(lazyListState, thresholdPx)
    IosLargeTitleScaffold(
        title = title,
        scrollOffset = scrollOffset,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        scrollThreshold = scrollThreshold,
        content = content
    )
}

/**
 * Low-level overload supporting arbitrary float scroll offsets.
 */
@Composable
fun IosLargeTitleScaffold(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    scrollThreshold: Dp = IosLargeTitleDefaults.CollapseThresholdDp,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density, scrollThreshold) {
        with(density) { scrollThreshold.toPx() }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarTotalHeight = statusBarTop + IosLargeTitleDefaults.TopBarHeight

    val innerPadding = remember(topBarTotalHeight) {
        PaddingValues(
            top = topBarTotalHeight,
            bottom = 0.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        // 1. Content Viewport (renders behind the floating translucent top bar)
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(innerPadding)
        }

        // 2. Pinned Top Layer: Translucent Large Title Top Bar
        IosLargeTitleTopBar(
            title = title,
            scrollOffset = scrollOffset,
            scrollThresholdPx = scrollThresholdPx,
            navigationIcon = navigationIcon,
            actions = actions,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Optional Bottom Bar (e.g. IosTabBar)
        if (bottomBar != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                bottomBar()
            }
        }

        // 4. Optional Snackbar Host
        if (snackbarHost != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (bottomBar != null) 60.dp else 16.dp)
            ) {
                snackbarHost()
            }
        }
    }
}

/**
 * Apple HIG Collapsible Large Title Top Bar Header
 *
 * Implements iOS UINavigationBar behavior:
 * - Resting state: Clear/transparent background, inline title hidden, large title visible in content.
 * - Scrolling state: Smooth crossfade based on 52dp collapse threshold.
 * - Collapsed state: Frosted glass material (93% translucency), 0.5dp hairline border, 17sp SemiBold title.
 */
@Composable
fun IosLargeTitleTopBar(
    title: String,
    scrollOffset: Float,
    modifier: Modifier = Modifier,
    scrollThresholdPx: Float = with(LocalDensity.current) { IosLargeTitleDefaults.CollapseThresholdDp.toPx() },
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val progress = IosLargeTitleDefaults.calculateInlineTitleAlpha(scrollOffset, scrollThresholdPx)

    // Frosted glass background: 93% translucency when collapsed, transparent at rest
    val baseBarColor = AppleMaterials.barBackgroundColor(isDark)
    val barBgColor = baseBarColor.copy(alpha = progress * baseBarColor.alpha)

    // 0.5dp hairline bottom divider
    val baseSeparatorColor = AppleMaterials.separatorColor(isDark)
    val dividerColor = baseSeparatorColor.copy(alpha = progress * baseSeparatorColor.alpha)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBgColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IosLargeTitleDefaults.TopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            // Left Action / Navigation Button
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon()
                }
            }

            // Centered Inline Title (17sp SemiBold), fades in with scroll progress
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = progress),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 72.dp)
            )

            // Right Actions
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
        }

        // Bottom 0.5dp hairline divider
        HorizontalDivider(
            thickness = IosLargeTitleDefaults.HairlineBorderWidth,
            color = dividerColor
        )
    }
}

/**
 * Large Title item designed to sit at the top of a scrollable view (e.g. first item of LazyColumn).
 * Features smooth inverse-alpha fadeout as content collapses into the top bar.
 */
@Composable
fun IosLargeTitleItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollOffset: Float = 0f,
    scrollThresholdPx: Float = with(LocalDensity.current) { IosLargeTitleDefaults.CollapseThresholdDp.toPx() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    val alpha = IosLargeTitleDefaults.calculateLargeTitleAlpha(scrollOffset, scrollThresholdPx)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                this.alpha = alpha
            }
    ) {
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle.uppercase(),
                fontSize = 12.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = PaperColors.MonoGray500,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * Helper to compute scroll offset from a LazyListState.
 * When scrolled past the first item entirely, guarantees full collapse offset.
 */
@Composable
fun rememberLazyListScrollOffset(
    listState: LazyListState,
    thresholdPx: Float = with(LocalDensity.current) { IosLargeTitleDefaults.CollapseThresholdDp.toPx() }
): Float {
    return remember(listState, thresholdPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                thresholdPx * 2f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat().coerceAtLeast(0f)
            }
        }
    }.value
}

/**
 * Helper to compute scroll offset from a ScrollState.
 */
@Composable
fun rememberScrollStateOffset(scrollState: ScrollState): Float {
    return remember(scrollState) {
        derivedStateOf {
            scrollState.value.toFloat().coerceAtLeast(0f)
        }
    }.value
}

/**
 * iOS Navigation Bar Icon Button primitive with spring touch physics and zero ripple.
 */
@Composable
fun IosNavIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .iosIconClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * iOS Navigation Bar Text Button primitive ("取消", "完成", "清空").
 */
@Composable
fun IosNavTextButton(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .padding(horizontal = 8.dp)
            .iosClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontFamily = SansFontFamily,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
            color = if (enabled) color else color.copy(alpha = 0.38f)
        )
    }
}

/**
 * iOS Navigation Bar Back Button with optional text label and spring touch physics.
 */
@Composable
fun IosNavBackButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .iosClick(onClick = onNavigateBack)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        if (!label.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 17.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.Normal,
                color = tint
            )
        }
    }
}

/**
 * Optional nested scroll connection state holder for non-standard scroll containers
 * (e.g. LazyVerticalGrid, multi-nested scroll layouts).
 */
@Stable
class IosLargeTitleScrollState(
    val thresholdPx: Float = 156f
) {
    var scrollOffset by mutableFloatStateOf(0f)
        private set

    val progress: Float
        get() = (scrollOffset / thresholdPx).coerceIn(0f, 1f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = -available.y
            if (delta > 0 && scrollOffset < thresholdPx) {
                val prev = scrollOffset
                scrollOffset = (scrollOffset + delta).coerceIn(0f, thresholdPx * 2f)
                val consumed = scrollOffset - prev
                return Offset(0f, -consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = -available.y
            if (delta < 0 && scrollOffset > 0) {
                val prev = scrollOffset
                scrollOffset = (scrollOffset + delta).coerceIn(0f, thresholdPx * 2f)
                val consumed = scrollOffset - prev
                return Offset(0f, -consumed)
            }
            return Offset.Zero
        }
    }
}
```

---

## 5. Screen Integration Guide for Implementer

### 5.1 `TimelineScreen.kt` Integration
Replace the manual `Box` + `IosLargeTitleTopBar` with `IosLargeTitleScaffold`:
```kotlin
val listState = rememberLazyListState()
val scrollOffset = rememberLazyListScrollOffset(listState)

IosLargeTitleScaffold(
    title = "日记",
    lazyListState = listState,
    actions = {
        IosNavIconButton(
            icon = Icons.Outlined.Search,
            contentDescription = "搜索",
            onClick = onNavigateToSearch
        )
        IosNavIconButton(
            icon = Icons.Outlined.EditSquare,
            contentDescription = "写日记",
            onClick = { onNavigateToEditor(null) }
        )
    }
) { innerPadding ->
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = 72.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header_large_title") {
            IosLargeTitleItem(
                title = "日记",
                subtitle = currentDateStr,
                scrollOffset = scrollOffset,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        // Segmented filters and diary cards follow...
    }
}
```

### 5.2 `SettingsScreen.kt` Integration
Replace the manual `Box` + `IosLargeTitleTopBar` with `IosLargeTitleScaffold`:
```kotlin
val scrollState = rememberScrollState()
val scrollOffset = rememberScrollStateOffset(scrollState)

IosLargeTitleScaffold(
    title = "设置",
    scrollState = scrollState,
    navigationIcon = if (onNavigateBack != null) {
        { IosNavBackButton(onNavigateBack = onNavigateBack) }
    } else null
) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = innerPadding.calculateTopPadding(), bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IosLargeTitleItem(
            title = "设置",
            scrollOffset = scrollOffset,
            modifier = Modifier.padding(top = 10.dp)
        )
        // IosListSections follow...
    }
}
```

### 5.3 Secondary Screens (`CalendarScreen`, `OnThisDayScreen`, `TrashScreen`, `StatsScreen`)
Replace legacy Material 3 `Scaffold` + `TopAppBar` with `IosLargeTitleScaffold`:
```kotlin
// Example: TrashScreen.kt
IosLargeTitleScaffold(
    title = "回收站",
    lazyListState = listState,
    navigationIcon = { IosNavBackButton(onNavigateBack = onNavigateBack) },
    actions = {
        if (diaries.isNotEmpty()) {
            IosNavTextButton(
                text = "清空",
                color = MaterialTheme.colorScheme.error,
                onClick = { showEmptyConfirm = true }
            )
        }
    }
) { innerPadding ->
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IosLargeTitleItem(title = "回收站", scrollOffset = scrollOffset)
        }
        // Trash items follow...
    }
}
```

---

## 6. Verification Matrix & Acceptance Criteria

| Requirement | Verification Test / Method | Expected Output |
|---|---|---|
| **Expanded Large Title** | `testF6_LargeTitleExpandedTypography` | 34sp Bold, line height 41sp, letter spacing (-0.4)sp |
| **Collapsed Inline Title** | `testF6_InlineTitleCollapsedTypography` | 17sp SemiBold, line height 22sp, centered |
| **Collapse Threshold** | `testF6_ScrollCollapseThresholdAndInterpolationFormula` | Threshold = 52dp (156px @ 3x density) |
| **Title Crossfade** | `testF6_LargeTitleFadeOutInterpolation` | Large title alpha = `1 - progress`, inline = `progress` |
| **Top Bar Height** | `testF6_TopInlineNavBarHeight` | Exact 44dp content height |
| **Frosted Glass Transition** | `testB2_LargeTitleElevationFrostedGlassTransition` | `isHeaderFrosted == true` when alpha >= 0.95 |
| **Negative Scroll Clamping** | `testB2_NegativeScrollOffsetClampingInLargeTitle` | Coerced cleanly to 0.0f |
| **Massive Scroll Clamping** | `testB2_MassiveScrollOffsetClampingInLargeTitle` | Coerced cleanly to 1.0f |
| **Zero FAB / MoreVert** | `MaterialIdiomPurgeAuditTest` | 0 FloatingActionButtons, 0 MoreVert menus |
| **Build & Tests** | `./gradlew test` | 100% test pass with 0 errors |
