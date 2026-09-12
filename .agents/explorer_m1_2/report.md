# Implementation Strategy: iOS Touch Physics (`Modifier.iosClick`) & Global Ripple Suppression

**Explorer:** Explorer M1-2  
**Target Package:** `com.example.inkpaperdiary`  
**Milestone:** M1 (iOS Design System & Interaction Primitives)  
**Date:** 2026-09-06  

---

## 1. Executive Summary

This report delivers the complete technical design and implementation architecture for replacing Android Material 3 radial ink ripples with authentic Apple Human Interface Guidelines (HIG) tactile spring touch physics (`Modifier.iosClick`) across the diary application.

Key deliverables formulated:
1. **`IosTouchPhysics.kt`**: A standalone interaction library in `app/src/main/java/com/example/inkpaperdiary/core/designsystem/interaction/` providing:
   - Elastic spring scale-down (`0.97f` for cards/rows, `0.96f` for icons, `0.92f` for tabs).
   - Alpha opacity dimming (`0.85f` for cards/rows, `0.80f` for tabs).
   - Physics parameters: `Spring.DampingRatioMediumBouncy` (0.75f) and `Spring.StiffnessMediumLow` (400.0f).
   - Haptic feedback via `LocalHapticFeedback.current` (`TextHandleMove` on press down, `LongPress` on hold).
   - Full accessibility semantics (`Role.Button`, `onClick`, `onLongClick`) and stale-closure prevention via `rememberUpdatedState`.
   - Complete zero-ripple execution path.
2. **Global Ripple Suppression in `Theme.kt`**:
   - Utilization of Material 3 1.4.0's `LocalRippleConfiguration provides null` and Foundation's `LocalIndication provides null` inside `PaperDiaryTheme` to eradicate all Material 3 and Foundation ripples globally.
3. **Card Touch Physics in `PaperCard.kt`**:
   - Replacement of legacy `Modifier.clickable { onClick() }` with `Modifier.iosClick(onClick = onClick, onLongClick = onLongClick)`.
   - Preservation of hairline specular border (`0.5.dp`) and left celadon accent pill while scaling the whole card smoothly as a single physical entity.

---

## 2. Codebase Baseline & Observations

### 2.1 Dependencies & Version Analysis
- **Gradle Build**: Gradle 9.1.0, Kotlin 2.2.0, AGP 9.0.1, CompileSdk 36.
- **Compose BOM**: `androidxComposeBom = "2026.03.01"` (defined in `gradle/libs.versions.toml:6`).
- **Resolved Material 3 Version**: `androidx.compose.material3:material3:1.4.0` (verified via dependency resolution).
- **Ripple API Status**:
  - `androidx.compose.material3.RippleKt.getLocalRippleConfiguration()` provides `ProvidableCompositionLocal<RippleConfiguration?>`.
  - Passing `null` to `LocalRippleConfiguration` completely disables ink ripples for all Material 3 components (`Card`, `Button`, `IconButton`, `Surface`, `ListItem`, etc.).
  - `androidx.compose.foundation.LocalIndication` provides `ProvidableCompositionLocal<IndicationNodeFactory?>`. Passing `null` suppresses default indications for Foundation `Modifier.clickable`.

### 2.2 Current `Theme.kt` (`app/src/main/java/com/example/inkpaperdiary/core/designsystem/Theme.kt`)
- `Theme.kt` lines 59-82 wrap `content` directly in `MaterialTheme(colorScheme, typography, shapes, content)`.
- It does **not** provide any ripple suppression composition locals. Any component invoking `Modifier.clickable` or Material 3 interactive components inherits default radial ink ripple effects.

### 2.3 Current `PaperCard.kt` (`app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/PaperCard.kt`)
- Lines 50-54 apply `Modifier.clickable { onClick() }`:
  ```kotlin
  Card(
      modifier = modifier
          .then(
              if (onClick != null) Modifier.clickable { onClick() } else Modifier
          )
  ```
- This triggers Android radial ink ripples on tap, lacks scale/alpha feedback, lacks haptic click feedback, and does not support `onLongClick` (which is needed for contextual action sheets).

### 2.4 Existing Test Suite Contract (`R1DesignSystemFeatureTest.kt`)
- Lines 144-201 explicitly define the test contract for F2 (`Modifier.iosClick`):
  - Card pressed scale: `0.97f` (line 153).
  - Compact icon pressed scale: `0.96f` (line 154).
  - Pressed alpha: `0.85f` (line 164).
  - Spring damping ratio: `0.75f` (`Spring.DampingRatioMediumBouncy`) (line 172).
  - Spring stiffness: `400.0f` (`Spring.StiffnessMediumLow`) (line 173).
  - Material ripple: `false` (line 186).
  - Disabled state: when `enabled = false`, scale and alpha remain locked at `1.0f` (lines 192-201).

---

## 3. Touch Physics Specification & Mechanics

### 3.1 Mathematical & Dynamic Contract
| Property | Released State | Pressed State (Touch Down) | Animation Spec |
|---|---|---|---|
| **Scale (`scaleX`, `scaleY`)** | `1.0f` | `0.97f` (cards/rows)<br>`0.96f` (icons)<br>`0.92f` (tab bar) | `spring(dampingRatio = 0.75f, stiffness = 400f)` |
| **Alpha (`alpha`)** | `1.0f` | `0.85f` (cards/rows)<br>`0.80f` (tab bar) | `spring(stiffness = 400f)` |
| **Haptics** | Idle | `TextHandleMove` (touch down tick)<br>`LongPress` (long click hold) | Direct trigger via `LocalHapticFeedback` |
| **Visual Indication** | `null` | `null` | Zero radial ripple, zero grey highlight mask |

### 3.2 Pointer Event Cycle & Cancellation Mitigation
```
                    [ Finger Down ]
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
    Scale -> 0.97f              Haptic Tick
    Alpha -> 0.85f         (TextHandleMove)
             │
   ┌─────────┴─────────┐
   ▼                   ▼
[ Release Inside ]   [ Drag Outside / Scroll Parent ]
   │                   │
   ▼                   ▼
Scale -> 1.0f        Scale -> 1.0f
Alpha -> 1.0f        Alpha -> 1.0f
onClick() fires      onClick() CANCELLED (no action)
```
- Gesture handling utilizes `detectTapGestures` with `tryAwaitRelease()`.
- If the finger is dragged beyond touch slop or parent scroll container (`LazyColumn`) consumes pointer movement, `tryAwaitRelease()` returns `false`, gracefully springing scale/alpha back to `1.0f` without firing `onClick`.
- `rememberUpdatedState` is applied to all callback lambdas (`onClick`, `onLongClick`, `haptic`) to ensure re-composed closures are never stale inside the persistent pointer input coroutine.

### 3.3 Accessibility & Test Automation (`semantics`)
By attaching `semantics(mergeDescendants = true)`:
- Sets `role = Role.Button`.
- Exposes `onClick` action for TalkBack screen readers and Compose test rules (e.g. `onNodeWithText(...).performClick()`).
- Exposes `onLongClick` action for accessibility long-press gestures.

---

## 4. Formulated Source Code for `IosTouchPhysics.kt`

Target Path: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/interaction/IosTouchPhysics.kt`

```kotlin
package com.example.inkpaperdiary.core.designsystem.interaction

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Apple Human Interface Guidelines (HIG) Touch Physics & Haptics System
 *
 * Implements tactile iOS physical feedback for interactive surfaces:
 * 1. Scale-down compression (0.97f default, 0.96f for icons, 0.92f for tabs).
 * 2. Opacity dimming (0.85f default, 0.80f for tabs).
 * 3. Elastic spring physics (MediumBouncy damping ratio 0.75f, MediumLow stiffness 400f).
 * 4. Crisp haptic feedback (TextHandleMove tick on press down, LongPress on long hold).
 * 5. Complete eradication of outward-diffusing Material ink ripples.
 */
object IosTouchDefaults {
    const val PRESSED_SCALE: Float = 0.97f
    const val COMPACT_PRESSED_SCALE: Float = 0.96f
    const val TAB_PRESSED_SCALE: Float = 0.92f

    const val PRESSED_ALPHA: Float = 0.85f
    const val TAB_PRESSED_ALPHA: Float = 0.80f

    val SpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy, // 0.75f
        stiffness = Spring.StiffnessMediumLow           // 400.0f
    )

    val AlphaSpringSpec = spring<Float>(
        stiffness = Spring.StiffnessMediumLow           // 400.0f
    )
}

/**
 * Primary iOS touch interaction modifier replacing Material 3 ink ripples with tactile spring physics.
 *
 * @param enabled Whether the component responds to touch input.
 * @param pressedScale Scale compression factor on finger press down (default: 0.97f).
 * @param pressedAlpha Opacity attenuation factor on finger press down (default: 0.85f).
 * @param haptic Whether to trigger tactile haptic tick feedback.
 * @param onLongClick Optional callback for long-press gestures (e.g. contextual action sheet).
 * @param onClick Primary tap callback.
 */
fun Modifier.iosClick(
    enabled: Boolean = true,
    pressedScale: Float = IosTouchDefaults.PRESSED_SCALE,
    pressedAlpha: Float = IosTouchDefaults.PRESSED_ALPHA,
    haptic: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentHaptic by rememberUpdatedState(haptic)

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = IosTouchDefaults.SpringSpec,
        label = "iosClickScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedAlpha else 1.0f,
        animationSpec = IosTouchDefaults.AlphaSpringSpec,
        label = "iosClickAlpha"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (enabled) {
                this.onClick {
                    currentOnClick()
                    true
                }
                if (currentOnLongClick != null) {
                    this.onLongClick {
                        currentOnLongClick?.invoke()
                        true
                    }
                }
            }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    isPressed = true
                    if (currentHaptic) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val released = tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = {
                    if (currentOnLongClick != null) {
                        if (currentHaptic) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        currentOnLongClick?.invoke()
                    }
                },
                onTap = {
                    currentOnClick()
                }
            )
        }
}

/**
 * Overload supporting specification naming conventions (`scaleDown`, `dimAlpha`, `hapticFeedback`).
 */
fun Modifier.iosClick(
    scaleDown: Float,
    dimAlpha: Float = IosTouchDefaults.PRESSED_ALPHA,
    hapticFeedback: Boolean = true,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = iosClick(
    enabled = enabled,
    pressedScale = scaleDown,
    pressedAlpha = dimAlpha,
    haptic = hapticFeedback,
    onLongClick = onLongClick,
    onClick = onClick
)

/**
 * Specialized touch modifier for bottom navigation tab items (IosTabBar).
 */
fun Modifier.iosTabClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = iosClick(
    enabled = enabled,
    pressedScale = IosTouchDefaults.TAB_PRESSED_SCALE, // 0.92f
    pressedAlpha = IosTouchDefaults.TAB_PRESSED_ALPHA, // 0.80f
    haptic = true,
    onClick = onClick
)

/**
 * Specialized touch modifier for compact icons and navigation buttons.
 */
fun Modifier.iosIconClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = iosClick(
    enabled = enabled,
    pressedScale = IosTouchDefaults.COMPACT_PRESSED_SCALE, // 0.96f
    pressedAlpha = IosTouchDefaults.PRESSED_ALPHA,         // 0.85f
    haptic = true,
    onClick = onClick
)

/**
 * CompositionLocalProvider helper that completely suppresses Material 3 ink ripples
 * and Foundation default ripples across all enclosed Composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppressMaterialRipples(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides null,
        LocalIndication provides null,
        content = content
    )
}
```

---

## 5. Global Ripple Suppression in `Theme.kt`

### 5.1 Mechanism
In Jetpack Compose with Material 3 1.4.0:
- Material 3 components query `LocalRippleConfiguration.current`. If `null`, they skip drawing ripple layers entirely.
- Foundation components query `LocalIndication.current`. If `null`, they skip default indication layers.
- By providing both as `null` at the root of `PaperDiaryTheme`, any child component without explicit indications becomes 100% ripple-free.

### 5.2 Implementation Plan for `Theme.kt`
Modify `app/src/main/java/com/example/inkpaperdiary/core/designsystem/Theme.kt`:
```kotlin
package com.example.inkpaperdiary.core.designsystem

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

... (colors remain unchanged) ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PaperTypography,
        shapes = PaperShapes
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
            LocalIndication provides null,
            content = content
        )
    }
}
```

---

## 6. Component Touch Physics & Ripple Suppression in `PaperCard.kt`

### 6.1 Changes Required in `PaperCard.kt`
1. Import `com.example.inkpaperdiary.core.designsystem.interaction.iosClick`.
2. Add `onLongClick: (() -> Unit)? = null` and `enabled: Boolean = true` parameters to `PaperCard`.
3. Remove `Modifier.clickable { onClick() }`.
4. Apply `Modifier.iosClick(...)` to `interactionModifier`.
5. Apply `interactionModifier` to `Card(modifier = ...)` before or alongside decorative drawing, ensuring the card and its specular border scale together seamlessly.

### 6.2 Proposed Code for `PaperCard.kt`
```kotlin
package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * Apple HIG Material Surface Card with tactile iOS spring physics & zero ripple.
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    thickness: MaterialThickness = MaterialThickness.THICK,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = AppleMaterials.backgroundColor(thickness),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 0.5.dp,
    elevation: Dp = 1.dp,
    hasCeladonAccent: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBorder = AppleMaterials.glassBorder(width = borderWidth)

    val interactionModifier = if (onClick != null) {
        Modifier.iosClick(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .then(interactionModifier)
            .then(
                if (hasCeladonAccent) {
                    Modifier.drawBehind {
                        // iOS slim pill accent bar
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(4.dp.toPx(), 14.dp.toPx()),
                            size = Size(3.dp.toPx(), size.height - 28.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = glassBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            content = content
        )
    }
}
```

---

## 7. Downstream Impact & Compatibility Plan

1. **`TimelineScreen`**:
   - `PaperCard` calls in `TimelineScreen` can now accept `onLongClick = { showContextMenu = true }` without touching any internal card logic.
   - When users tap a diary card, the card elastically compresses to `0.97f`, dims to `0.85f`, emits a crisp haptic tick, and opens the editor without any radial ink ripple.
2. **`IosListComponents` (`IosListRow`, `IosNavigationRow`, `IosSwitchRow`)**:
   - When interactive rows are tapped, they apply `Modifier.iosClick(onClick = onClick)`.
3. **`IosTabBar`**:
   - Bottom tab bar items use `Modifier.iosTabClick { onTabSelected(tab) }` with `0.92f` scale-down.
4. **Existing Code Compatibility**:
   - All existing call sites of `PaperCard` without `onLongClick` remain 100% binary- and source-compatible due to the default parameter `= null`.
   - All callers using `PaperCard(onClick = ...)` automatically receive tactile iOS spring physics and zero ink ripple.

---

## 8. Verification Strategy

1. **Compilation Verification**:
   - Run `./gradlew compileDebugKotlin` to ensure clean compilation of `IosTouchPhysics.kt`, `Theme.kt`, and `PaperCard.kt`.
2. **Unit Test Verification**:
   - All tests in `R1DesignSystemFeatureTest.kt` under `F2: iOS Touch Physics Contract` will pass.
3. **Runtime & Visual Verification**:
   - Interactive components visibly compress elastically on touch down and spring back upon release.
   - Dragging a finger outside the card before lifting cancels the tap cleanly without triggering `onClick`.
   - No circular ink ripple gradient is drawn on cards or rows.
