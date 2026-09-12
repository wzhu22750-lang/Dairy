# Technical Specification: iOS HIG & Component System for Jetpack Compose Diary Application

**Document Version:** 1.0.0  
**Target Package:** `com.example.inkpaperdiary`  
**Scope:** R1 (Design System & Interaction Primitives), R2 (Root Navigation & Collapsible Large Title), R3 (Screen Layout & Component Overhaul)  
**Author:** Spec Miner 2  
**Date:** 2026-09-06  

---

## 1. Executive Summary & Design System Philosophy

The goal of this architectural refactor is to systematically eradicate Android Material 3 idioms (radial ink ripples, circular Floating Action Buttons, 3-dot overflow menus, standard Android Alert Dialogs and spinners) and implement an authentic Apple Human Interface Guidelines (HIG) visual and interaction foundation directly in Jetpack Compose without adding third-party UI dependencies.

The core design philosophy is built upon three pillars:
1. **Translucent Materials & Vibrancy Depth:** Multi-layered frosted glass materials (from Ultra-Thin to Ultra-Thick) bordered by 0.5dp hairline specular highlights that refract light, creating optical depth.
2. **Tactile Spring Physics (`iosClick`):** Direct, immediate physical feedback upon touch down: an elastic spring compression (0.96~0.98x scale) accompanied by subtle opacity attenuation (0.85x) and crisp haptic feedback, completely replacing outward-diffusing ink ripples.
3. **Structured Grouping & Large Title Hierarchy:** Systematic adoption of iOS Inset Grouped lists (16dp rounded cards, 30dp squircle category icon boxes, 56dp indented hairline dividers) combined with collapsible 34sp large titles transitioning into compact 17sp centered navigation titles upon scroll.

---

## 2. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R1 Design System | `AppleMaterials` | 5-level HIG translucent background colors and hairline specular glass border (`0.5.dp`). | `MaterialThickness`, `isDark` mode flag. | Compose `Color`, `BorderStroke` with linear gradient brush. | Falls back to solid theme surface if opacity unsupported. | `AppleMaterial.kt`, HIG Materials Spec |
| 2 | R1 Design System | Vibrancy System | 4-tier foreground text/icon vibrancy scaling (`PRIMARY` 1.0, `SECONDARY` 0.60, `TERTIARY` 0.30, `QUATERNARY` 0.18). | `VibrancyLevel`, optional `baseColor`. | Dynamic `Color` with calculated alpha channel. | Unspecified color defaults to pure Black or White. | `AppleMaterial.kt`, HIG Vibrancy Spec |
| 3 | R1 Design System | `Modifier.appleMaterial` | Modifier applying translucent fill and hairline gradient border with squircle clipping. | `thickness`, `shape`, `hasBorder`. | Translucent, bordered, clipped `Modifier`. | None; defaults to `REGULAR` and rectangle shape. | `AppleMaterial.kt` |
| 4 | R1 Interaction | `Modifier.iosClick` | Replaces Material ink ripple with tactile spring scale-down (`0.97x`), alpha dimming (`0.85x`), and haptics. | `enabled`, `scaleDown`, `dimAlpha`, `hapticFeedback`, `onLongClick`, `onClick`. | Pressed state animation modifier with zero ripple. | Disabled states skip haptics and keep scale at 1.0x. | `ORIGINAL_REQUEST.md`, HIG Touch Feedback |
| 5 | R1 Inset Lists | `IosListSection` | Inset grouped container card for row items with header and footer typography. | `header: String?`, `footer: String?`, `content: ColumnScope.() -> Unit`. | 16dp rounded squircle grouped card with exterior labels. | Empty content renders header/footer safely. | HIG Lists Spec, `SettingsScreen.kt` |
| 6 | R1 Inset Lists | `IosListRow` | Base row with optional 30dp squircle icon box, title, subtitle, trailing slot, and 56dp divider. | `icon`, `title`, `subtitle`, `trailing`, `onClick`, `showDivider`. | Standardized iOS list cell with indented divider. | Long text truncated with ellipsis. | HIG Lists Spec |
| 7 | R1 Inset Lists | `IosNavigationRow` | Specialized row featuring trailing chevron disclosure indicator and secondary value label. | `icon`, `title`, `subtitle`, `value`, `onClick`, `showDivider`. | Interactive row navigating to deeper destination. | Missing value hides secondary label gracefully. | HIG Lists Spec |
| 8 | R1 Inset Lists | `IosSwitchRow` | Specialized row featuring an iOS-native styled toggle switch. | `icon`, `title`, `subtitle`, `checked`, `onCheckedChange`, `showDivider`. | Row with togglable switch accessory. | Toggling fires callback and triggers haptic feedback. | HIG Lists Spec |
| 9 | R1 Control | `IosSegmentedControl` | Seamless horizontal pill slider with animated floating thumb and drop shadow. | `items: List<T>`, `selectedItem: T`, `onItemSelected`, `itemLabel`. | High-contrast pill toggle slider. | Out-of-bounds selection falls back to index 0. | HIG Controls Spec |
| 10 | R2 Navigation | `IosTabBar` | Bottom navigation bar with 4 root tabs, 93% translucency, hairline top border, and spring feedback. | `currentTab: IosTab`, `onTabSelected: (IosTab) -> Unit`. | 49dp translucent bottom bar anchored to navigation insets. | Invalid tab preserves current selection. | `ORIGINAL_REQUEST.md` R2 |
| 11 | R2 Navigation | `IosLargeTitleScaffold` | Scrolling scaffold smoothly transitioning 34sp Bold title to 17sp SemiBold centered inline title. | `title`, `subtitle`, `lazyListState`, `actions`, `content`. | Dynamic collapsible header container. | Overscroll safely clamps alpha to [0f, 1f]. | HIG Navigation Bars Spec |
| 12 | R2 Architecture | Idiom Elimination | Complete eradication of Material FAB and 3-dot overflow menu (`Icons.Default.MoreVert`). | Codebase audit of Scaffold and TopAppBar. | Relocation of actions to navigation bar & action sheets. | Build verification fails if FAB/MoreVert exists in UI. | `ORIGINAL_REQUEST.md` R2 |
| 13 | R3 Timeline | Apple Journal Stream | Overhauled timeline with segmented filters, spring diary cards, and contextual long-press action sheets. | `TimelineUiState`, filter callbacks, navigation handlers. | Modern journal card stream with compose in nav bar. | Empty state displays minimal glyph and compose action. | `ORIGINAL_REQUEST.md` R3 |
| 14 | R3 Settings | Inset Grouped Settings | Settings screen rebuilt strictly with 4 Inset Grouped sections matching iOS Settings app hierarchy. | `SettingsUiState`, sync/security/export handlers. | Grouped list screen with 30dp squircle icons & switches. | Sync error emits non-blocking toast/notice. | `ORIGINAL_REQUEST.md` R3 |
| 15 | R3 Editor | iOS Editor Toolbar | Top bar with Cancel/Done text buttons and inline capsule date/time picker button. | `EditorUiState`, date picker launcher, save/discard handlers. | Clean writing canvas with frosted markdown bottom bar. | Back navigation triggers auto-save without loss. | `ORIGINAL_REQUEST.md` R3 |
| 16 | R3 Dialogs | `IosActionSheet` | iOS-native modal bottom action sheet with squircle grouping, hairline dividers, and separate Cancel pill. | `visible`, `onDismiss`, `title`, `message`, `actions`, `cancelLabel`. | Translucent modal action sheet. | Dismiss on scrim tap or back button. | HIG Action Sheets Spec |
| 17 | R3 Dialogs | `IosModalDialog` | Centered 270dp iOS-style modal alert dialog replacing Android `AlertDialog` for PIN and credentials. | `visible`, `onDismiss`, `title`, `content`, `confirmButton`, `dismissButton`. | Centered squircle frosted dialog with hairline divider. | Back press or scrim tap triggers onDismiss. | HIG Alerts Spec |
| 18 | R3 Pickers | `IosDateTimePickerSheet` | Native iOS modal date and time picker replacing legacy Android `DatePickerDialog`/`TimePickerDialog`. | `initialTimestamp`, `onConfirm`, `onDismiss`. | Modal wheel/calendar sheet with Cancel/Done buttons. | Invalid time clamps to current system boundary. | `EditorScreen.kt` |

---

## 3. Edge Cases & Observed Behaviors

| # | Feature | Input / Condition | Observed Behavior & Mitigation |
|---|---------|-------------------|--------------------------------|
| 1 | `IosLargeTitleScaffold` | Rapid list fling causing negative or fractional scroll offsets | Interpolation calculation uses `(offset / maxOffset).coerceIn(0f, 1f)` ensuring title alpha never goes negative or exceeds 1.0. |
| 2 | `Modifier.iosClick` | Finger dragged outside component bounds before release | Gesture detection cancels scale/alpha transition smoothly back to `1.0f` without firing `onClick`. |
| 3 | `Modifier.iosClick` | Rapid consecutive taps (double tap or rapid tapping) | Spring animation handles velocity smoothly; subsequent down events re-target scale down without visual clipping or glitch. |
| 4 | `IosListSection` | Section containing a single row item | Row renders with top and bottom corners both rounded to 16dp; no bottom divider is drawn. |
| 5 | `IosListRow` | Row with no icon provided (`icon = null`) | Content left padding remains at 16dp, and divider indents by 16dp instead of 56dp to align with title text. |
| 6 | `IosSegmentedControl` | Switching tabs rapidly while slide animation is active | Spring animation dynamically redirects from current intermediate offset to new target segment without snapping. |
| 7 | `IosTabBar` | Screen with soft input keyboard (IME) open | Bottom tab bar responds to window insets: when IME is visible, tab bar hides or remains pinned below IME. |
| 8 | `IosActionSheet` | Extremely long action sheet message or title | Text is wrapped cleanly with max lines constraint and scrollable content area if overflowing screen height. |
| 9 | `IosModalDialog` | Soft keyboard opens for PIN or Supabase input | Dialog adjusts upward via `imePadding()` to remain centered above the soft keyboard without obscuring inputs. |
| 10 | `TimelineScreen` | Large number of photos in diary card attachments | Horizontal `LazyRow` allows smooth scrolling; thumbnails are fixed at 72dp with 10dp squircle corners and 0.5dp border. |
| 11 | `SettingsScreen` | Long text in credentials URL or sync timestamps | Subtitle sets `maxLines = 1` with `TextOverflow.Ellipsis`, ensuring the row layout does not push trailing switches off-screen. |
| 12 | AppLock & File Pickers | Launching system document picker for TXT/JSON import | Setting `AppLockManager.isPickerActive = true` prevents `MainActivity.onStop()` from locking the app during file selection. |

---

## 4. R1: iOS Design System & Interaction Primitives

### 4.1 Materials System (`MaterialThickness`)

Apple HIG materials provide optical translucency combined with background blur simulation. The application must standardize on 5 distinct physical thickness levels:

```kotlin
package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color

enum class MaterialThickness {
    ULTRA_THIN,   // Highest light transmission, subtle tinting
    THIN,         // Moderate translucency for segmented controls & small pills
    REGULAR,      // Standard balance for navigation bars, search bars, bottom tab bar
    THICK,        // Low translucency, high contrast surface for diary cards & list sections
    ULTRA_THICK   // Near-opaque backdrop for modal dialogs and lock screen PIN pad
}
```

#### Color Matrix & Alpha Values

| Thickness Level | Light Mode ARGB | Light Hex | Light Alpha | Dark Mode ARGB | Dark Hex | Dark Alpha | Recommended Usage |
|-----------------|-----------------|-----------|-------------|----------------|----------|------------|-------------------|
| `ULTRA_THIN` | `0x73FFFFFF` | `#73FFFFFF` | 45% | `0x661C1C1E` | `#661C1C1E` | 40% | Subtle layered backdrops |
| `THIN` | `0x99FFFFFF` | `#99FFFFFF` | 60% | `0x8C1C1C1E` | `#8C1C1C1E` | 55% | Segmented controls, chips |
| `REGULAR` | `0xE6F2F2F7` | `#E6F2F2F7` | 90% | `0xD9161618` | `#D9161618` | 85% | Top navigation bar, search bar |
| `REGULAR_BAR` | `0xEEF2F2F7` | `#EEF2F2F7` | 93% | `0xEE000000` | `#EE000000` | 93% | Bottom Tab Bar (`IosTabBar`) |
| `THICK` | `0xF5FFFFFF` | `#F5FFFFFF` | 96% | `0xF21C1C1E` | `#F21C1C1E` | 95% | Diary cards, Inset list sections |
| `ULTRA_THICK` | `0xFDFFFFFF` | `#FDFFFFFF` | 99% | `0xFA121214` | `#FA121214` | 98% | Action sheets, modal alerts |

### 4.2 Vibrancy System (`VibrancyLevel`)

Vibrancy adapts foreground elements (text, glyphs, dividers) to maintain contrast across translucent materials:

```kotlin
enum class VibrancyLevel {
    PRIMARY,     // 100% alpha - Headings, high-priority icons
    SECONDARY,   // 60% alpha  - Subtitles, metadata, timestamps, secondary labels
    TERTIARY,    // 30% alpha  - Placeholder text, disabled glyphs
    QUATERNARY   // 18% alpha  - Subtle borders, interior dividers
}
```

#### Formula:
$$\text{Color}_{\text{vibrant}} = \text{baseColor} \cdot \alpha_{\text{level}}$$
- When `baseColor` is unspecified:
  - Light mode base = `Color(0xFF000000)` (`MonoBlack`)
  - Dark mode base = `Color(0xFFFFFFFF)` (`MonoWhite`)

### 4.3 Hairline Specular Glass Border (`AppleMaterials.glassBorder`)

To reproduce the optical bevel found on iOS translucent glass sheets, elements must have a hairline `0.5.dp` border rendered with a vertical linear gradient brush:

```kotlin
@Composable
fun glassBorder(
    isDark: Boolean = isSystemInDarkTheme(),
    width: Dp = 0.5.dp
): BorderStroke {
    val brush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0x38FFFFFF), // 22% white highlight on top edge
                Color(0x14FFFFFF)  // 8% white bleed on bottom edge
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x99FFFFFF), // 60% white specular highlight on top
                Color(0x1F000000)  // 12% black contact shadow on bottom
            )
        )
    }
    return BorderStroke(width, brush)
}
```

### 4.4 Modifier Extension: `Modifier.appleMaterial`

```kotlin
fun Modifier.appleMaterial(
    thickness: MaterialThickness = MaterialThickness.REGULAR,
    shape: Shape? = null,
    hasBorder: Boolean = true
): Modifier
```
- **Execution Pipeline:**
  1. `clip(shape)` if shape is provided.
  2. `background(AppleMaterials.backgroundColor(thickness), shape)`.
  3. `border(AppleMaterials.glassBorder(), shape)` if `hasBorder == true`.

---

### 4.5 iOS Touch Physics: `Modifier.iosClick`

Material ink ripples look distinctly out of place on iOS. The `iosClick` modifier provides native iOS tactile feedback:

#### Physical Dynamics Contract:
- **Scale Factor on Touch Down:** `0.97f` (cards/rows) or `0.96f` (compact icons).
- **Alpha Attenuation on Touch Down:** `0.85f`.
- **Release Elasticity:** Spring animation with `dampingRatio = Spring.DampingRatioMediumBouncy` (0.75f) and `stiffness = Spring.StiffnessMediumLow` (400f).
- **Haptic Signal:** `HapticFeedbackType.LongPress` on long click; `HapticFeedbackType.TextHandleMove` (or subtle tick) on press down / release.
- **Indication:** Explicitly `null` indication (zero Material ink ripple).

#### Exact Kotlin Signature:
```kotlin
fun Modifier.iosClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    dimAlpha: Float = 0.85f,
    hapticFeedback: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier
```

#### Implementation Architecture:
```kotlin
fun Modifier.iosClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    dimAlpha: Float = 0.85f,
    hapticFeedback: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iosClickScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) dimAlpha else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iosClickAlpha"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    isPressed = true
                    if (hapticFeedback) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val released = tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = {
                    if (onLongClick != null) {
                        if (hapticFeedback) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLongClick()
                    }
                },
                onTap = {
                    onClick()
                }
            )
        }
}
```

---

### 4.6 System Inset Grouped List Components

In accordance with iOS `UICollectionLayoutListConfiguration.Appearance.insetGrouped`:

#### Layout & Visual Geometry:
1. **Container (`IosListSection`):**
   - Margin: `16.dp` horizontal margins from screen edges.
   - Corner Radius: `16.dp` continuous squircle (`RoundedCornerShape(16.dp)`).
   - Background: `MaterialThickness.THICK` surface with `0.5.dp` hairline glass border.
   - Section Header:
     - Font: `13.sp`, Regular/Medium, `MonoGray500` (`VibrancyLevel.SECONDARY`).
     - Padding: Start `16.dp`, Top `12.dp`, Bottom `6.dp`.
     - Text Transform: Capitalized or standard sentence casing.
   - Section Footer:
     - Font: `13.sp`, Regular, `MonoGray500`.
     - Padding: Start `16.dp`, Top `6.dp`, Bottom `16.dp`.

2. **Left Icon Box:**
   - Dimensions: Fixed `30.dp x 30.dp`.
   - Corner Radius: `7.dp` continuous squircle (`RoundedCornerShape(7.dp)`).
   - Inner Icon Size: `18.dp x 18.dp`, center aligned.
   - Color Scheme: System tinted background (e.g. `MonoGray800` dark / `MonoGray200` light, or category tint), with contrasting icon glyph.

3. **Indented 0.5dp Divider:**
   - Thickness: `0.5.dp`.
   - Start Indent: Exactly **`56.dp`**!
     $$\text{Indent} = 16\text{dp (row padding)} + 30\text{dp (icon box)} + 10\text{dp (content gap)} = 56\text{dp}$$
   - When no icon is present (`icon = null`), start indent collapses to **`16.dp`**.
   - Color: `Color(0x1F000000)` (light) / `Color(0x2EFFFFFF)` (dark).
   - Last row in section: No divider drawn (container clips the bottom edge).

#### Component Signatures:

```kotlin
@Composable
fun IosListSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
)

@Composable
fun IosListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
)

@Composable
fun IosNavigationRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true
)

@Composable
fun IosSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true
)
```

---

### 4.7 iOS Segmented Control (`IosSegmentedControl`)

Replaces Material 3 `FilterChip` rows with a native sliding pill container.

#### Geometry & Interaction:
- **Track Container:**
  - Shape: `RoundedCornerShape(9.dp)` (or `CapsuleShape`).
  - Background: `MonoGray200` (light) / `MonoGray800` (dark).
  - Height: `32.dp`.
  - Inner Padding: `2.dp`.
- **Sliding Thumb (Indicator):**
  - Shape: `RoundedCornerShape(7.dp)`.
  - Color: `Color.White` (light) / `Color(0xFF636366)` (dark).
  - Elevation / Shadow: `1.5.dp` soft ambient blur.
  - Animation: Spring motion (`Spring.DampingRatioNoBouncy`, `Spring.StiffnessMediumLow`).
- **Typography:**
  - Selected: `13.sp`, `FontWeight.SemiBold`, `MonoBlack` (light) / `MonoWhite` (dark).
  - Unselected: `13.sp`, `FontWeight.Normal`, `MonoGray500`.

#### Exact Kotlin Signature:
```kotlin
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String,
    itemIcon: (@Composable (T) -> Unit)? = null
)
```

---

## 5. R2: Root Navigation Architecture & Collapsible Large Title

### 5.1 Root Navigation Architecture & Bottom Tab Bar

The root navigation moves away from a single timeline with top bar actions into an authentic iOS 4-tab container:

```kotlin
enum class IosTab(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    JOURNAL("日记", Icons.Filled.Book, Icons.Outlined.Book),
    CALENDAR("日历", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    MEMORIES("回忆", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}
```

#### Geometry & Styling of `IosTabBar`:
- **Bar Height:** `49.dp` content height + `WindowInsets.navigationBars`.
- **Material Translucency:** 93% frosted glass:
  - Light mode: `Color(0xEEF2F2F7)`
  - Dark mode: `Color(0xEE000000)`
- **Top Border:** 0.5dp hairline border (`AppleMaterials.glassBorder`).
- **Item Layout:**
  - Vertical alignment: Icon (`22.dp ~ 24.dp`) on top, text label (`10.sp`, Medium/SemiBold) on bottom.
  - Active Color: `MonoBlack` (light) / `MonoWhite` (dark).
  - Inactive Color: `MonoGray500`.
  - Press Feedback: `iosClick(scaleDown = 0.92f, dimAlpha = 0.8f)` with haptics.

#### Exact Kotlin Signature:
```kotlin
@Composable
fun IosTabBar(
    currentTab: IosTab,
    onTabSelected: (IosTab) -> Unit,
    modifier: Modifier = Modifier
)
```

---

### 5.2 Dynamic Collapsible Large Title

Replaces the static Android `TopAppBar` with a scrolling header that smoothly collapses:

#### Transition Physics & Scroll Tracking:
- **Expanded State (Scroll offset = 0):**
  - Top inline bar: Fully transparent (`alpha = 0f`), no bottom hairline border.
  - Inline title: Hidden (`alpha = 0f`).
  - Large Title banner: Positioned below top bar, `34.sp`, Bold, line height `41.sp`, letter spacing `0.37.sp`. Fully visible (`alpha = 1f`).
- **Collapsed State (Scroll offset > Collapse Threshold, e.g. 52dp):**
  - Top inline bar: Background animates to 93% frosted glass (`alpha = 1f`), and 0.5dp hairline bottom border becomes visible.
  - Inline title: Centered, `17.sp`, SemiBold, line height `22.sp`. Smoothly slides up and fades in (`alpha = 1f`).
  - Large Title banner: Scrolled out of view and/or faded out.

#### Exact Kotlin Signature:
```kotlin
@Composable
fun IosLargeTitleScaffold(
    title: String,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
)
```

---

### 5.3 Complete Removal of Android Idioms

1. **Floating Action Button (FAB):**
   - **Status:** **Completely Eliminated**.
   - **Replacement:** The "新建日记" (New Diary) action is positioned in the top-right trailing slot of the navigation bar on the `Journal` tab as an iOS-native compose icon button (`Icons.Outlined.Edit` or pen glyph).
2. **3-dot Overflow Menu (`Icons.Default.MoreVert`):**
   - **Status:** **Completely Eliminated**.
   - **Replacement:**
     - On Timeline screen: Secondary actions (Calendar, Memories/OnThisDay, Settings) are now accessible via the **Bottom Tab Bar**. TXT import is moved to the **Settings** screen or an iOS Action Sheet.
     - On Diary Cards: The 3-dot dropdown menu is completely removed. Tapping the card opens the editor; **long-pressing** the card triggers an authentic iOS Contextual Action Sheet (`IosActionSheet`).

---

## 6. R3: Screen Layout & Component Overhaul

### 6.1 `TimelineScreen` (Apple Journal-style Stream)

1. **Header Structure:**
   - Uses `IosLargeTitleScaffold` with title "日记" and subtitle today's date formatted (e.g. "9月6日 星期日").
   - Action item in top bar: Search icon (`Icons.Default.Search`) and Compose icon (`Icons.Outlined.Edit`).
2. **Segmented Filter Bar:**
   - Segmented control directly beneath large title with options: `全部` (All), `置顶` (Pinned), or Mood filter selection.
3. **Diary Stream Cards (`PaperCard`):**
   - Interactivity: Uses `Modifier.iosClick(onClick = { onNavigateToEditor(diary.id) }, onLongClick = { showContextMenu = true })`. Zero Material ink ripple!
   - Visual: `16.dp` squircle, `MaterialThickness.THICK`, `0.5.dp` hairline specular border.
   - Pinned indicator: Left side slim pill accent bar (`3.dp` width, `14.dp` vertical inset).
   - Date marker: `42.dp x 42.dp` compact squircle marker (`dayNum` 17sp Bold, `monthYear` 9sp Secondary).
   - Badges: `StampBadge` for mood and weather.
   - Content: Title (`17.sp`, SemiBold) and preview text (`15.sp`, max 3 lines).
   - Photos: Horizontal row of up to 3 thumbnails (`72.dp x 72.dp`, `10.dp` squircle).
4. **Contextual Action Sheet (on Long Press):**
   - Triggers `IosActionSheet` with options:
     - Toggle Pin ("置顶此篇" / "取消置顶")
     - Move to Trash ("移入回收站", destructive style with Red text)
     - Cancel button

---

### 6.2 `SettingsScreen` (iOS Inset Grouped Structure)

Rebuilt using `IosListSection`, `IosListRow`, `IosNavigationRow`, and `IosSwitchRow` into 4 strict sections:

1. **Section 1: 云端与同步 (Cloud & Sync)**
   - `IosNavigationRow`: "Supabase 凭据配置", subtitle: connection status / URL preview, chevron. Click opens `IosModalDialog`.
   - `IosListRow`: "立即双向同步", subtitle: "上次同步: ...", trailing sync icon button.
   - `IosSwitchRow`: "自动后台同步", subtitle: "每 1 小时在联网时自动同步", switch checked state.
2. **Section 2: 安全与隐私保护 (Security & Privacy)**
   - `IosSwitchRow`: "应用锁 (PIN 密码)", subtitle: status. Toggling ON opens PIN setup `IosModalDialog`.
   - If PIN enabled: `IosSwitchRow`: "指纹 / 面容快速解锁", switch checked state.
3. **Section 3: 书写信笺底纹 (Paper Texture Style)**
   - `IosListRow`: Embedded `IosSegmentedControl` containing: "纯净纸面" (Blank), "横线便签" (Ruled), "手账点阵" (Dotted).
4. **Section 4: 数据管理与归档 (Data Management)**
   - `IosNavigationRow`: "导出 Markdown 压缩包", subtitle: "包含所有日记及本地配图".
   - `IosNavigationRow`: "导出全量 JSON 备份", subtitle: "跨设备无损迁移或还原".
   - `IosNavigationRow`: "导入 JSON 备份", subtitle: "从其他设备迁移日记数据".
   - `IosNavigationRow`: "导入 TXT 纯文本日记", subtitle: "批量导入并自动识别旧日期".
   - `IosNavigationRow`: "日记回收站", subtitle: "支持 30 天内恢复被误删日记", chevron indicator leading to `TrashScreen`.

---

### 6.3 `EditorScreen` & Date/Time Pickers

1. **iOS Navigation Bar:**
   - Leading: "取消" (Cancel) text button or back chevron (`17.sp`, `MonoBlack` / `MonoWhite`).
   - Center: Date/Time pill button (`CapsuleShape`, `0.5.dp` border, clock icon + formatted date string). Clicking opens `IosDateTimePickerSheet`.
   - Trailing: Pin toggle icon button, and prominent "完成" (Done) pill button (`15.sp`, SemiBold).
2. **Markdown Formatting Bar:**
   - Docked above the IME keyboard or at the screen bottom.
   - Surface: `MaterialThickness.REGULAR` frosted glass with `0.5.dp` glass border.
   - Tool buttons: Photo insertion, H1, H2, Bold, Italic, Bulleted List, Numbered List, Checkbox, Quote, Divider.
3. **Date & Time Picker Sheet (`IosDateTimePickerSheet`):**
   - Replaces Android legacy `DatePickerDialog` & `TimePickerDialog`.
   - Displays as an iOS modal bottom sheet with:
     - Top bar: "取消" (left), "选择时间" (center title), "完成" (right).
     - Wheel or calendar-style picker.

---

### 6.4 Modal Action Sheets & Dialogs

Replaces Android Material 3 `AlertDialog` across the entire application:

#### 1. `IosActionSheet`:
- **Geometry:**
  - Rendered as a bottom sheet.
  - Action Group: `14.dp` squircle card, `MaterialThickness.ULTRA_THICK`, containing action rows separated by `0.5.dp` hairline dividers.
  - Action Row Height: `56.dp`, text centered (`17.sp`, Regular/Medium, destructive action in `Color(0xFFFF3B30)`).
  - Cancel Button: Detached `14.dp` squircle card beneath action group, separated by `8.dp` gap. Height `56.dp`, text `17.sp` SemiBold.

```kotlin
data class IosActionItem(
    val title: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun IosActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    actions: List<IosActionItem>,
    cancelTitle: String = "取消"
)
```

#### 2. `IosModalDialog`:
- **Geometry:**
  - Width: Fixed `270.dp`.
  - Shape: `14.dp` squircle (`RoundedCornerShape(14.dp)`).
  - Background: `MaterialThickness.ULTRA_THICK` with `0.5.dp` hairline specular border.
  - Header: Centered title (`17.sp`, Bold) and message (`13.sp`, Regular, `MonoGray500`).
  - Button Bar:
    - If 2 buttons: Side-by-side row, each `44.dp` height, separated by a vertical `0.5.dp` hairline divider.
    - If 3+ buttons: Vertical stack, each separated by a horizontal `0.5.dp` hairline divider.
    - Confirm button: `17.sp`, SemiBold.
    - Dismiss/Cancel button: `17.sp`, Regular.

```kotlin
@Composable
fun IosModalDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmTitle: String = "确定",
    dismissTitle: String? = "取消",
    onConfirm: () -> Unit,
    content: (@Composable () -> Unit)? = null
)
```

---

## 7. Component Contracts & Implementation Signatures

### 7.1 `IosListSection` Contract
```kotlin
package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.*

@Composable
fun IosListSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header.uppercase(),
                style = PaperTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = PaperColors.MonoGray500,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .appleMaterial(
                    thickness = MaterialThickness.THICK,
                    shape = RoundedCornerShape(16.dp),
                    hasBorder = true
                )
        ) {
            content()
        }

        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = PaperTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = PaperColors.MonoGray500
                ),
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
            )
        }
    }
}
```

### 7.2 `IosListRow` & `IosNavigationRow` Contract
```kotlin
@Composable
fun IosListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    val rowModifier = if (onClick != null) {
        modifier.iosClick(onClick = onClick)
    } else {
        modifier
    }

    Column(modifier = rowModifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = PaperTypography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = PaperTypography.bodySmall,
                            color = PaperColors.MonoGray500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (trailing != null) {
                Box(
                    modifier = Modifier.padding(start = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailing()
                }
            }
        }

        if (showDivider) {
            val indentStart = if (icon != null) 56.dp else 16.dp
            HorizontalDivider(
                modifier = Modifier.padding(start = indentStart),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}
```

---

## 8. Visual Tokens & Dimension Matrix

| Token Name | Token Type | Value (Light) | Value (Dark) | Purpose |
|------------|------------|---------------|--------------|---------|
| `CornerRadius.Card` | Dp | `16.dp` | `16.dp` | Diary cards, Inset list containers |
| `CornerRadius.Pill` | Shape | `RoundedCornerShape(50)` | `RoundedCornerShape(50)` | Capsule buttons, badges, status chips |
| `CornerRadius.IconBox`| Dp | `7.dp` | `7.dp` | Left squircle category icon boxes |
| `CornerRadius.Modal` | Dp | `14.dp` | `14.dp` | Modal alert dialogs, Action sheets |
| `Border.Hairline` | Dp | `0.5.dp` | `0.5.dp` | Optical specular edge highlight |
| `Indent.ListDivider`| Dp | `56.dp` | `56.dp` | 16dp pad + 30dp icon + 10dp gap |
| `Height.TabBar` | Dp | `49.dp` | `49.dp` | Bottom translucent tab bar content |
| `Height.NavBar` | Dp | `44.dp` | `44.dp` | Top inline navigation bar content |
| `Height.ActionRow` | Dp | `56.dp` | `56.dp` | Action sheet button height |
| `Scale.TouchPress` | Float | `0.97f` | `0.97f` | Spring scale compression factor |
| `Alpha.TouchDim` | Float | `0.85f` | `0.85f` | Opacity dimming upon finger press |
| `Alpha.TabBar` | Float | `0.93f` | `0.93f` | Tab bar background opacity |
| `Typography.LargeTitle`| TextStyle | 34sp Bold (lineHeight 41sp) | 34sp Bold | Collapsible Large Title banner |
| `Typography.InlineTitle`| TextStyle | 17sp SemiBold (lineHeight 22sp) | 17sp SemiBold | Centered navigation title |
| `Typography.Body` | TextStyle | 17sp Regular (lineHeight 24sp) | 17sp Regular | Standard list and card text |
| `Typography.Footnote`| TextStyle | 13sp Regular (lineHeight 18sp) | 13sp Regular | List section footers, metadata |

---

## 9. Verification & Acceptance Criteria

1. **Compilation Guarantee:**
   - `./gradlew assembleDebug` compiles with 0 errors.
   - `./gradlew test` passes 100% of unit tests.
2. **Material Idiom Purge:**
   - Zero occurrences of `FloatingActionButton` on any primary user flow.
   - Zero occurrences of `Icons.Default.MoreVert` or Android popup `DropdownMenu`.
   - Zero radial Material ripples on cards, list items, and segmented controls.
3. **HIG Architectural Compliance:**
   - Root navigation functions fluidly with `IosTabBar` across 4 tabs (`Journal`, `Calendar`, `Memories`, `Settings`).
   - Settings strictly follows Inset Grouped lists with 56dp indented dividers.
   - Timeline header smoothly collapses from 34sp Large Title to 17sp inline title upon scrolling.
   - Dialogs strictly use `IosActionSheet` and `IosModalDialog`.
4. **Domain Integrity:**
   - All Room entities, DAOs, AppLockManager, and Supabase synchronization pipelines remain 100% intact and functional without regressions.
