# Implementation Blueprint: `IosTabBar.kt` (Apple HIG Translucent Bottom Navigation)

**Target Component:** `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`  
**Milestone:** M2 (Root Navigation & Collapsible Large Title)  
**Author:** Explorer M2-1 (Gen 2)  
**Date:** 2026-09-06  
**Status:** Comprehensive Blueprint (Ready for Worker Execution)

---

## 1. Executive Summary & Design Philosophy

The `IosTabBar` is the foundational root navigation primitive for the InkPaperDiary application refactor, establishing an authentic Apple Human Interface Guidelines (HIG) visual and interaction architecture. In accordance with Apple HIG, `IosTabBar`:

1. **Eradicates Android Material Navigation Idioms:** Completely removes the Material 3 `NavigationBar`, floating action buttons (FAB), and Android ink ripples, replacing them with a docked, translucent bottom tab bar with spring physics.
2. **Dynamic 93% Translucency Material:** Utilizes Apple's standard 93.3% frosted glass bar translucency (`Color(0xEEF2F2F7)` for light mode, `Color(0xEE000000)` for dark mode), allowing underlying diary list content to subtly shine through during scrolling.
3. **0.5dp Specular Hairline Glass Border:** Replaces coarse Android elevation shadows with an authentic 0.5dp specular gradient hairline top border (`AppleMaterials.glassBorder`), featuring a specular reflection highlight in light mode and a subtle reflective hairline in dark mode.
4. **Authentic 4-Tab Hierarchy:** Provides the four canonical root tabs: **Journal (日记)**, **Calendar (日历)**, **Memories (回忆)**, and **Settings (设置)**, each pairing an unselected outlined glyph with a selected filled glyph.
5. **Color & Vibrancy System:** Implements active tinting (Apple System Blue `#007AFF` or theme primary) vs inactive secondary tinting (Apple System Gray `#8E8E93` / `MonoGray500`) with smooth 200ms color cross-fading.
6. **Tactile Spring Touch Physics:** Implements iOS-native spring scale-down (0.92f compression), subtle alpha dimming (0.80f), and instant haptic tick feedback via `Modifier.iosTabClick` / `Modifier.iosClick`, with zero ripple diffusion.
7. **Flawless Edge-to-Edge Safe Area Insets:** Dynamically absorbs `WindowInsets.navigationBars` to seamlessly float content above the Android home indicator while bleeding the translucent frosted glass all the way to the physical screen edge.

---

## 2. Audit of Existing Codebase vs Proposed Blueprint

A forensic audit of the existing codebase (`IosTabBar.kt`, `AppleMaterial.kt`, `IosTouchPhysics.kt`, `AppNavigation.kt`, and `R2NavigationFeatureTest.kt`) revealed several key areas requiring refinement:

| Dimension | Existing Implementation | Proposed Blueprint (HIG Standard) | Rationale & Test Conformance |
|---|---|---|---|
| **Content Height** | `50.dp` | **`49.dp`** | iOS `UITabBar` standard is 49pt. Directly validated by `R2NavigationFeatureTest.testF5_TabBarGeometryAndHeight` (`assertEquals(49f, barHeight.value)`). |
| **Material Translucency** | `AppleMaterials.backgroundColor(REGULAR)` (85% Dark / 90% Light) | **`AppleMaterials.barBackgroundColor()`** (`0xEE000000` / `0xEEF2F2F7` = **93.3%**) | Requirement R2 explicitly dictates 93% translucency; matches `testF5_TabBarTranslucencyAndColors` (`assertEquals(0.93f, bg.alpha)`). |
| **Top Hairline Border** | Flat `HorizontalDivider(0.5.dp, separatorColor)` | **0.5dp Specular Hairline Glass Border** (`AppleMaterials.glassBorder` gradient brush) | Fulfills requirement for specular reflection highlight (`0x99FFFFFF` to `0x1F000000` in Light; `0x38FFFFFF` to `0x14FFFFFF` in Dark). |
| **Memories Icon Pair** | `Icons.Outlined.AutoAwesome` / `Icons.Filled.AutoAwesome` | **`Icons.Outlined.History` / `Icons.Filled.History`** (with configurable alternative) | Matches `PROJECT.md` line 91 interface contract: `MEMORIES("回忆", Icons.Outlined.History, Icons.Filled.History)` and `OnThisDayScreen.kt`. |
| **Icon Sizing** | `23.dp` | **`24.dp`** | Aligns with `testF5_TabBarItemTypographyAndIconSize` (`assertEquals(24f, iconSize.value)`). |
| **Active Tint** | Hardcoded to `MaterialTheme.colorScheme.primary` | **Configurable `activeColor: Color = Color(0xFF007AFF)`** (Apple System Blue) with fallback to theme primary | Satisfies dispatch requirement (`AppleTheme.colors.systemBlue`) while preserving monochromatic theme compatibility. |
| **Inactive Tint** | Hardcoded to `PaperColors.MonoGray500` | **Configurable `inactiveColor: Color = PaperColors.MonoGray500`** (`0xFF8E8E93` = Apple System Gray) | Matches Apple HIG Secondary Label standard. |
| **Window Insets** | `windowInsetsPadding(WindowInsets.navigationBars)` on outer `Column` | **Full-width hairline border + bottom inset padding on Column + horizontal safe area on Row** | Prevents hairline border clipping in landscape; ensures edge-to-edge background bleed behind gesture pill. |
| **Accessibility** | Missing Tab Semantics | **`Modifier.semantics { role = Role.Tab; selected = isSelected }`** | TalkBack screen reader accessibility: announces tab name, tab count (1 of 4), and selection state. |
| **Color Animation** | Default animation spec | **`tween(durationMillis = 200, easing = FastOutSlowInEasing)`** | Smooth, flicker-free tab transition. |

---

## 3. Data Structures & Constants Specification

### 3.1 `enum class IosTab`
Defines the 4 canonical root tabs in order of appearance:

```kotlin
enum class IosTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    JOURNAL(
        label = "日记",
        icon = Icons.Outlined.Book,
        selectedIcon = Icons.Filled.Book
    ),
    CALENDAR(
        label = "日历",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth
    ),
    MEMORIES(
        label = "回忆",
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History
    ),
    SETTINGS(
        label = "设置",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}
```

*Note on Memories Icon:* `PROJECT.md` specifies `Icons.Outlined.History` and `Icons.Filled.History`. If sparkle aesthetic is desired, `AutoAwesome` can be alternatively provided via custom tab mapping, but `History` is the canonical contract.

### 3.2 `object AppleTabDefaults`
Encapsulates all HIG geometry, color constants, and animation parameters:

```kotlin
object AppleTabDefaults {
    /** Standard iOS UITabBar content height in dp */
    val BarHeight: Dp = 49.dp

    /** 0.5dp Hairline border thickness */
    val HairlineBorderWidth: Dp = 0.5.dp

    /** Standard tab icon dimensions */
    val IconSize: Dp = 24.dp

    /** Tab label typography size */
    val LabelFontSize: TextUnit = 10.sp

    /** Apple HIG System Blue active tint */
    val SystemBlue: Color = Color(0xFF007AFF)

    /** Apple HIG System Gray inactive tint (Secondary Label) */
    val SystemGray: Color = PaperColors.MonoGray500 // Color(0xFF8E8E93)

    /** Light mode 93.3% translucent frosted glass background */
    val LightBarBackground: Color = Color(0xEEF2F2F7)

    /** Dark mode 93.3% translucent frosted glass background */
    val DarkBarBackground: Color = Color(0xEE000000)

    /** Tab color transition animation spec */
    val ColorTransitionSpec = tween<Color>(durationMillis = 200)
}
```

---

## 4. Complete Kotlin Implementation Blueprint (`IosTabBar.kt`)

Below is the complete, production-grade source code ready to be applied to `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`:

```kotlin
package com.example.inkpaperdiary.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.PaperDiaryTheme
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosTabClick

/**
 * Design constants and default styling for Apple HIG Tab Bar.
 */
object AppleTabDefaults {
    val BarHeight: Dp = 49.dp
    val HairlineBorderWidth: Dp = 0.5.dp
    val IconSize: Dp = 24.dp
    val LabelFontSize: TextUnit = 10.sp

    val SystemBlue: Color = Color(0xFF007AFF)
    val SystemGray: Color = PaperColors.MonoGray500 // Color(0xFF8E8E93)

    val LightBarBackground: Color = Color(0xEEF2F2F7) // 93.3% alpha
    val DarkBarBackground: Color = Color(0xEE000000)  // 93.3% alpha

    val ColorTransitionSpec = tween<Color>(durationMillis = 200)
}

/**
 * Apple HIG 4-tab Root Navigation Specification.
 *
 * Interface Contract defined in PROJECT.md:
 * - JOURNAL: 日记, Outlined.Book -> Filled.Book
 * - CALENDAR: 日历, Outlined.CalendarMonth -> Filled.CalendarMonth
 * - MEMORIES: 回忆, Outlined.History -> Filled.History
 * - SETTINGS: 设置, Outlined.Settings -> Filled.Settings
 */
enum class IosTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    JOURNAL(
        label = "日记",
        icon = Icons.Outlined.Book,
        selectedIcon = Icons.Filled.Book
    ),
    CALENDAR(
        label = "日历",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth
    ),
    MEMORIES(
        label = "回忆",
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History
    ),
    SETTINGS(
        label = "设置",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}

/**
 * iOS Translucent Bottom Tab Bar (UITabBar equivalent in Jetpack Compose).
 *
 * Visual & Behavioral Characteristics:
 * - 49dp content height (UITabBar standard) + navigation bar insets bleed.
 * - Dynamic 93% translucency frosted glass material (AppleMaterials.barBackgroundColor).
 * - 0.5dp specular hairline top border with subtle gradient reflections.
 * - Active tint (Apple System Blue #007AFF) vs Inactive tint (Apple System Gray #8E8E93).
 * - Active filled icon glyph vs Inactive outlined icon glyph.
 * - Spring touch physics via `Modifier.iosTabClick` (scale 0.92f, alpha 0.80f, haptic tick, zero ripple).
 * - Full TalkBack accessibility semantics (Role.Tab, selected state).
 * - Automatic window insets handling for bottom home indicator and horizontal safe areas.
 *
 * @param selectedTab Currently active root tab.
 * @param onTabSelected Callback invoked when a tab item is pressed.
 * @param modifier Modifier for external positioning (e.g. Modifier.align(Alignment.BottomCenter)).
 * @param activeColor Color applied to the active tab icon and label (defaults to Apple System Blue).
 * @param inactiveColor Color applied to inactive tab icons and labels (defaults to Apple System Gray).
 */
@Composable
fun IosTabBar(
    selectedTab: IosTab,
    onTabSelected: (IosTab) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AppleTabDefaults.SystemBlue,
    inactiveColor: Color = AppleTabDefaults.SystemGray
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = AppleMaterials.barBackgroundColor(isDark)

    // Specular gradient hairline top border matching AppleMaterials.glassBorder
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0x38FFFFFF), // 顶部发丝微反射 (22% white)
                Color(0x14FFFFFF)  // 底部极淡透光 (8% white)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x99FFFFFF), // 顶部发丝高光 (60% white specular)
                Color(0x1F000000)  // 底部环境微阴影 (12% black contact line)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    ) {
        // 1. 0.5dp Specular Hairline Top Border (spans 100% full width edge-to-edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppleTabDefaults.HairlineBorderWidth)
                .background(borderBrush)
        )

        // 2. 49dp Tab Items Row (with horizontal safe area padding for landscape)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                .height(AppleTabDefaults.BarHeight),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                IosTabItem(
                    tab = tab,
                    isSelected = isSelected,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Individual Tab Item Composable with iOS spring feedback and accessibility semantics.
 */
@Composable
private fun IosTabItem(
    tab: IosTab,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = AppleTabDefaults.ColorTransitionSpec,
        label = "tabIconTint_${tab.name}"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = AppleTabDefaults.ColorTransitionSpec,
        label = "tabLabelColor_${tab.name}"
    )

    Column(
        modifier = modifier
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .iosTabClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
            contentDescription = tab.label,
            tint = iconTint,
            modifier = Modifier.size(AppleTabDefaults.IconSize)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = AppleTabDefaults.LabelFontSize,
            fontFamily = SansFontFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            color = labelColor,
            maxLines = 1
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------------------

@Preview(name = "IosTabBar - Light Theme", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun PreviewIosTabBarLight() {
    PaperDiaryTheme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IosTabBar(
                selectedTab = IosTab.JOURNAL,
                onTabSelected = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview(name = "IosTabBar - Dark Theme", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewIosTabBarDark() {
    PaperDiaryTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IosTabBar(
                selectedTab = IosTab.JOURNAL,
                onTabSelected = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
```

---

## 5. Technical Deep Dive & Mechanics

### 5.1 Translucency & Materials Pipeline
- **Color Representation:**
  - Light mode: `0xEEF2F2F7` (`alpha = 0.9333f`, `RGB = (242, 242, 247)`).
  - Dark mode: `0xEE000000` (`alpha = 0.9333f`, `RGB = (0, 0, 0)`).
- **Underlying Composition:** In `AppNavigation.kt`, the root screen content (e.g. `LazyColumn` in `TimelineScreen`) scrolls beneath the bottom bar. The 93.3% alpha allows diary card contours and image edges to soften underneath the bar, matching the native iOS `UIBlurEffect` / `chromeMaterial` appearance.
- **Why `barBackgroundColor` over `REGULAR`:** `REGULAR` thickness in `AppleMaterial.kt` is 85-90% alpha, which is tailored for inline navigation bars. `barBackgroundColor` is precisely calibrated at `0xEE` (93.3%) to prevent text unreadability while maintaining translucent spatial depth.

### 5.2 Specular Hairline Top Border
- **Specular Highlight:** On Apple displays, top borders are not flat gray dividers. They consist of a 0.5dp specular hairline:
  - **Light mode:** Vertical gradient from `Color(0x99FFFFFF)` (60% white specular reflection) to `Color(0x1F000000)` (12% contact shadow).
  - **Dark mode:** Vertical gradient from `Color(0x38FFFFFF)` (22% white highlight) to `Color(0x14FFFFFF)` (8% light bleed).
- **Edge-to-Edge Span:** The border `Box` is placed directly at the top of the outer `Column` before any horizontal window insets, ensuring the hairline extends 100% across the physical screen edge regardless of device orientation.

### 5.3 iOS Touch Physics & Haptics (`Modifier.iosTabClick`)
`Modifier.iosTabClick` delegates to `Modifier.iosClick`:
1. **Finger Down:**
   - Scale compresses to `0.92f` (`TAB_PRESSED_SCALE`) using a medium-bouncy spring (`dampingRatio = 0.75f`, `stiffness = 400f`).
   - Opacity attenuates to `0.80f` (`TAB_PRESSED_ALPHA`).
   - Tactile feedback: invokes `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`.
   - Ink ripples: completely suppressed via `SuppressMaterialRipples` and `NoIndication`.
2. **Finger Up / Release:**
   - Spring rebounds smoothly back to `1.0f` scale and `1.0f` alpha.
   - `onClick()` triggers state mutation in `AppNavigation`.

### 5.4 Window Insets Architecture
```
+-------------------------------------------------------------+  Top of IosTabBar
| 0.5dp Specular Hairline Top Border (Width: 100% Display)   |
+-------------------------------------------------------------+
|                                                             |
|   [ Journal ]     [ Calendar ]    [ Memories ]  [ Settings] |  49dp Content Height
|                                                             |
+-------------------------------------------------------------+
|                                                             |  Dynamic Inset Height
|      (Translucent Frosted Glass Background Bleed)           |  WindowInsets.navigationBars
|                 --- Android Gesture Pill ---                |  (e.g. 16dp - 24dp)
|                                                             |
+-------------------------------------------------------------+  Bottom of Display
```
1. Outer `Column` background covers the entire region including the bottom inset.
2. `HorizontalDivider`/`Box` border is NOT padded, maintaining full-bleed hairline.
3. Content `Row` has `height(49.dp)` and horizontal insets padding, keeping touch targets ergonomically proportioned and centered.

---

## 6. Integration Architecture with `AppNavigation.kt`

In `AppNavigation.kt`, `IosTabBar` is positioned at the root tier:

```kotlin
// AppNavigation.kt
Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "TabTransition"
    ) { tab ->
        when (tab) {
            IosTab.JOURNAL -> TimelineScreen(...)
            IosTab.CALENDAR -> CalendarScreen(...)
            IosTab.MEMORIES -> OnThisDayScreen(...)
            IosTab.SETTINGS -> SettingsScreen(...)
        }
    }

    // Docked bottom translucent tab bar
    IosTabBar(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}
```

### 6.1 Back Navigation Flow
`BackHandler` in `AppNavigation` enforces iOS tab navigation conventions:
1. **Modal Active:** Back button dismisses modal (returns to active tab).
2. **Secondary Tab Active (Calendar, Memories, Settings):** Back button switches back to `IosTab.JOURNAL` (primary tab).
3. **Journal Tab Active:** Back button exits app.

### 6.2 Bottom Padding in Root Screens
All root screens include safe bottom padding for their scrollable containers:
- `TimelineScreen`: `contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)`
- `CalendarScreen`: `modifier.padding(bottom = 60.dp)`
- `OnThisDayScreen`: `modifier.padding(bottom = 60.dp)`
- `SettingsScreen`: `modifier.padding(top = 56.dp, bottom = 80.dp)`

---

## 7. Verification & Testing Matrix

The implementation blueprint is verifiable against existing unit test suites and compiler checks:

### 7.1 Existing Unit Test Mapping
| Test Case | Method / Suite | Assertions Verified |
|---|---|---|
| Tab Count & Titles | `R2NavigationFeatureTest.testF5_TabBarContainsExactFourCanonicalTabs` | Exact 4 tabs: "日记", "日历", "回忆", "设置" |
| Dimensions & Geometry | `R2NavigationFeatureTest.testF5_TabBarGeometryAndHeight` | `barHeight == 49.dp`, `hairlineBorder == 0.5.dp` |
| Translucency & Alpha | `R2NavigationFeatureTest.testF5_TabBarTranslucencyAndColors` | Alpha `0.93f` on light/dark backgrounds |
| Typography & Icon Size | `R2NavigationFeatureTest.testF5_TabBarItemTypographyAndIconSize` | `labelFontSize == 10.sp`, `iconSize == 24.dp` |
| State Transition Cycle | `R2NavigationFeatureTest.testF5_TabBarStateTransitionCycle` | 4-state cyclical navigation sequence |
| Boundary Switching | `R2BoundaryEdgeCasesTest.testB2_RapidConsecutiveTabSwitching` | 100 rapid tab changes without desynchronization |

### 7.2 Independent Verification Commands
```bash
# 1. Compile Kotlin source code
./gradlew compileDebugKotlin

# 2. Run R2 navigation test suite
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier1_features.R2NavigationFeatureTest"

# 3. Run R2 boundary edge cases suite
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier2_boundaries.R2BoundaryEdgeCasesTest"

# 4. Run entire project test suite
./gradlew test
```

---

## 8. Summary for Worker Implementation

The Worker agent can implement `IosTabBar.kt` by replacing the existing draft in `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt` with the complete Kotlin code provided in Section 4. Zero modifications to external API signatures are required, ensuring 100% backward compatibility with `AppNavigation.kt` and all dependent screens.
