# Architectural Report: Apple Materials, Vibrancy & Specular Glass Borders (M1)

**Target Artifact:** `app/src/main/java/com/example/inkpaperdiary/core/designsystem/AppleMaterial.kt`  
**Milestone:** M1 (iOS Design System & Interaction Primitives)  
**Author:** Explorer M1-1  
**Date:** 2026-09-06  

---

## 1. Executive Summary

In Milestone 1 of the iOS HIG refactoring for InkPaperDiary, the foundational visual substrate is provided by **Translucent Materials**, **Vibrancy Levels**, and **Specular Glass Borders**. Unlike Android Material Design's elevation-via-drop-shadows approach, Apple Human Interface Guidelines establish visual hierarchy through **optical depth**: layered translucent frosted glass sheets bordered by hairline specular edge highlights that catch simulated zenith light.

This report establishes the complete specification, code architecture, modifier extensions, downstream integration contracts, and preview/unit test strategies for `AppleMaterial.kt`.

---

## 2. Review & Critique of Existing `AppleMaterial.kt`

The repository already has an initial implementation of `AppleMaterial.kt` (lines 1–128). An architectural review reveals both solid foundations and critical areas for enhancement:

### 2.1 Strengths
1. **Core Semantics Present:** The 5-level `MaterialThickness` enum (`ULTRA_THIN`, `THIN`, `REGULAR`, `THICK`, `ULTRA_THICK`) and 4-tier `VibrancyLevel` enum (`PRIMARY`, `SECONDARY`, `TERTIARY`, `QUATERNARY`) are correctly named and aligned with Apple HIG.
2. **Accurate Calibrated Alpha Values:** The hex color values in `AppleMaterials.backgroundColor` match the exact opacity targets specified in Apple HIG:
   - `ULTRA_THIN`: 45% Light (`0x73FFFFFF`) / 40% Dark (`0x661C1C1E`)
   - `THIN`: 60% Light (`0x99FFFFFF`) / 55% Dark (`0x8C1C1C1E`)
   - `REGULAR`: 90% Light (`0xE6F2F2F7`) / 85% Dark (`0xD9161618`)
   - `THICK`: 96% Light (`0xF5FFFFFF`) / 95% Dark (`0xF21C1C1E`)
   - `ULTRA_THICK`: 99% Light (`0xFDFFFFFF`) / 98% Dark (`0xFA121214`)
3. **Specular Gradient Concept:** `glassBorder` uses a vertical linear gradient brush (`Brush.verticalGradient`) with top highlight and bottom shadow.

### 2.2 Critical Gaps & Enhancement Opportunities
1. **Composable-Only Lock-in:** 
   - `AppleMaterials.backgroundColor`, `glassBorder`, and `vibrancyColor` are currently annotated with `@Composable` because they invoke `isSystemInDarkTheme()`.
   - *Problem:* This prevents pure JVM unit tests from verifying colors without mocking Android/Compose runtime environments, and prevents non-composable helper functions or forced-theme previews from passing an explicit `isDark: Boolean`.
   - *Solution:* Provide pure Kotlin overload functions accepting `isDark: Boolean`, with `@Composable` zero-argument convenience delegates defaulting to `isSystemInDarkTheme()`.
2. **Missing Bar Background Token (93% Translucency):**
   - Both `IosTabBar` and the collapsed header in `IosLargeTitleScaffold` (M2) require standard iOS 93% frosted bar glass (`0xEEF2F2F7` light / `0xEE000000` dark).
   - Currently, components must reference `PaperColors.GlassLightBar` directly or approximate with `REGULAR`.
   - *Solution:* Add `AppleMaterials.barBackgroundColor(isDark: Boolean)` to provide a single authoritative API.
3. **Missing System Separator Color API:**
   - Inset list dividers and modal separators require Apple HIG hairline separator color: `0x1F000000` (12% black) in light mode and `0x2EFFFFFF` (18% white, matching Quaternary vibrancy) in dark mode.
   - *Solution:* Expose `AppleMaterials.separatorColor(isDark: Boolean)`.
4. **Missing Content Color & Text Vibrancy Integration:**
   - Compose components like `Text` and `Icon` default to `LocalContentColor.current`. Currently, developers must manually assign `color = AppleMaterials.vibrancyColor(...)` on every single text element.
   - *Solution:* Introduce `ProvideVibrancy(level, baseColor) { ... }` which binds `LocalContentColor`, plus a `Color.withVibrancy(level)` extension and `Modifier.vibrancyAlpha(level)`.
5. **Modifier Chaining & Clipping Order:**
   - In `Modifier.appleMaterial`, clipping child content to the rounded shape must occur *inside* the border so that the outer half of the 0.5dp specular border is not clipped away by anti-aliasing masks.
   - *Solution:* Structure modifier pipeline: `border -> background -> clip`.
6. **Missing Standalone `Modifier.glassBorder`:**
   - Elements with their own custom backgrounds (e.g. photos, custom card surfaces, capsule buttons) need to apply the 0.5dp specular border directly without re-applying a material background.
   - *Solution:* Expose `fun Modifier.glassBorder(shape: Shape = RectangleShape, width: Dp = 0.5.dp): Modifier`.

---

## 3. Detailed Technical Specifications

### 3.1 5-Thickness Materials (`MaterialThickness`)

```kotlin
enum class MaterialThickness {
    ULTRA_THIN,   // 45% Light / 40% Dark: Layered backdrop scrims, search filters
    THIN,         // 60% Light / 55% Dark: Segmented control tracks, capsule chips
    REGULAR,      // 90% Light / 85% Dark: Navigation top bars, toolbars, search headers
    THICK,        // 96% Light / 95% Dark: Diary cards (PaperCard), Inset Grouped sections
    ULTRA_THICK   // 99% Light / 98% Dark: Modal alerts (IosModalDialog), Action sheets
}
```

#### Color & Alpha Matrix

| Thickness Level | Light Mode Hex | Light ARGB | Light Alpha | Dark Mode Hex | Dark ARGB | Dark Alpha | Recommended Usage |
|---|---|---|---|---|---|---|---|
| `ULTRA_THIN` | `#73FFFFFF` | `0x73FFFFFF` | 45.1% | `#661C1C1E` | `0x661C1C1E` | 40.0% | Layered scrims, backdrops |
| `THIN` | `#99FFFFFF` | `0x99FFFFFF` | 60.0% | `#8C1C1C1E` | `0x8C1C1C1E` | 54.9% | Filter tracks, pill buttons |
| `REGULAR` | `#E6F2F2F7` | `#E6F2F2F7` | 90.2% | `#D9161618` | `0xD9161618` | 85.1% | Top bar, search bar surface |
| `REGULAR_BAR` (Bar) | `#EEF2F2F7` | `0xEEF2F2F7` | 93.3% | `#EE000000` | `0xEE000000` | 93.3% | Tab bar, collapsed top header |
| `THICK` | `#F5FFFFFF` | `0xF5FFFFFF` | 96.1% | `#F21C1C1E` | `0xF21C1C1E` | 94.9% | Diary cards, Inset list sections |
| `ULTRA_THICK` | `#FDFFFFFF` | `0xFDFFFFFF` | 99.2% | `#FA121214` | `0xFA121214` | 98.0% | Action sheets, modal alerts, PIN pad |

### 3.2 4-Tier Vibrancy Hierarchy (`VibrancyLevel`)

Vibrancy adapts foreground elements (text, glyphs, dividers) to ensure high legibility without losing the optical connection to the translucent background.

```kotlin
enum class VibrancyLevel {
    PRIMARY,     // 100% Alpha: Headings, high-priority labels, active tab icons
    SECONDARY,   // 60% Alpha: Subtitles, timestamps, metadata, inactive tab icons
    TERTIARY,    // 30% Alpha: Placeholder text, disabled glyphs, subtle borders
    QUATERNARY   // 18% Alpha: Interior hairline list dividers, ultra-subtle indicators
}
```

#### Base Color Resolution Rules
- If `baseColor == Color.Unspecified`:
  - Light mode base = `Color(0xFF000000)` (`Color.Black`)
  - Dark mode base = `Color(0xFFFFFFFF)` (`Color.White`)
- If `baseColor` is specified (e.g. system accent, destructive red `0xFFFF3B30`):
  - Return `baseColor.copy(alpha = vibrancyAlpha)`

### 3.3 Specular Glass Border (`AppleMaterials.glassBorder`)

To reproduce the optical bevel found on real iOS physical glass panels:
1. **Light Directionality:** Overhead ambient light reflects off the top edge, while the bottom edge suffers ambient contact occlusion.
2. **Light Mode Gradient:**
   - Top Stop (0.0): `Color(0x99FFFFFF)` (60% white specular highlight)
   - Bottom Stop (1.0): `Color(0x1F000000)` (12% black contact shadow)
3. **Dark Mode Gradient:**
   - Top Stop (0.0): `Color(0x38FFFFFF)` (22% white highlight)
   - Bottom Stop (1.0): `Color(0x14FFFFFF)` (8% white diffuse bleed)
4. **Stroke Width:** `0.5.dp` (Apple hairline thickness).

---

## 4. Exact Kotlin Implementation Code

The complete proposed replacement code for `AppleMaterial.kt`:

```kotlin
package com.example.inkpaperdiary.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Human Interface Guidelines (HIG) - 材质系统 (Materials)
 * 官方文档：https://developer.apple.com/cn/design/human-interface-guidelines/materials
 *
 * 核心设计哲学：
 * 材质是指在前景和背景元素间创建景深感、分层感和层次感的半透明视觉效果。
 * 系统定义了 5 级物理材质厚度（Thickness Levels）与 4 级鲜明度（Vibrancy Levels）：
 * - ULTRA_THIN: 极薄，最高透光度，用于次要背景穿透与微弱覆盖层
 * - THIN: 薄，适度透光，用于轻量筛选条、分段控制滑轨、小胶囊
 * - REGULAR: 标准，经典平衡，用于导航顶栏 (TopAppBar)、搜索条、浮动输入面板
 * - THICK: 厚，低透光高对比，用于悬浮日记卡片 (PaperCard)、成组列表 (IosListSection)
 * - ULTRA_THICK: 极厚，用于锁屏密码盘、高对比模态弹窗 (IosModalDialog)、操作动作表 (IosActionSheet)
 */
enum class MaterialThickness {
    ULTRA_THIN,
    THIN,
    REGULAR,
    THICK,
    ULTRA_THICK
}

/**
 * Apple HIG Vibrancy (鲜明度/活力度)：
 * 动态调节前景文字、图标与微分割线的透明度，以保证在半透明材质之上的极佳对比度与通透感。
 */
enum class VibrancyLevel {
    PRIMARY,     // 100% 主标题/核心图标
    SECONDARY,   // 60% 次要文字/时间戳/标签
    TERTIARY,    // 30% 辅助提示/占位文字/禁用态
    QUATERNARY   // 18% 细发丝分界/微弱指示
}

object AppleMaterials {

    /**
     * 根据深浅色模式与材质厚度等级返回标准半透明底色（纯函数，支持无 Compose 环境与单元测试）
     */
    fun backgroundColor(thickness: MaterialThickness, isDark: Boolean): Color {
        return when (thickness) {
            MaterialThickness.ULTRA_THIN -> if (isDark) Color(0x661C1C1E) else Color(0x73FFFFFF)
            MaterialThickness.THIN -> if (isDark) Color(0x8C1C1C1E) else Color(0x99FFFFFF)
            MaterialThickness.REGULAR -> if (isDark) Color(0xD9161618) else Color(0xE6F2F2F7)
            MaterialThickness.THICK -> if (isDark) Color(0xF21C1C1E) else Color(0xF5FFFFFF)
            MaterialThickness.ULTRA_THICK -> if (isDark) Color(0xFA121214) else Color(0xFDFFFFFF)
        }
    }

    /**
     * Composable 便捷重载：自动读取当前系统深浅色模式
     */
    @Composable
    fun backgroundColor(thickness: MaterialThickness): Color =
        backgroundColor(thickness, isSystemInDarkTheme())

    /**
     * iOS 底部标签栏与顶部常驻收起栏的 93% 通透底色
     */
    fun barBackgroundColor(isDark: Boolean): Color =
        if (isDark) Color(0xEE000000) else Color(0xEEF2F2F7)

    @Composable
    fun barBackgroundColor(): Color =
        barBackgroundColor(isSystemInDarkTheme())

    /**
     * iOS 成组列表与卡片内部发丝线分割色 (56dp / 16dp 缩进分割线)
     */
    fun separatorColor(isDark: Boolean): Color =
        if (isDark) Color(0x2EFFFFFF) else Color(0x1F000000)

    @Composable
    fun separatorColor(): Color =
        separatorColor(isSystemInDarkTheme())

    /**
     * Apple HIG 玻璃边缘折射与发丝线高光边框 (Hairline Glass Border)
     * 浅色模式具有顶部微光高光与极淡环境阴影线；深色模式具有晶莹发丝反射线。
     */
    fun glassBorder(
        isDark: Boolean,
        width: Dp = 0.5.dp
    ): BorderStroke {
        val brush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x38FFFFFF), // 顶部边缘微反射 (22% white)
                    Color(0x14FFFFFF)  // 底部极淡透光 (8% white)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x99FFFFFF), // 顶部高光 (60% white specular)
                    Color(0x1F000000)  // 底部极细阴影 (12% black contact shadow)
                )
            )
        }
        return BorderStroke(width, brush)
    }

    @Composable
    fun glassBorder(
        width: Dp = 0.5.dp
    ): BorderStroke = glassBorder(isSystemInDarkTheme(), width)

    /**
     * Apple HIG Vibrancy (鲜明度/活力度)：动态调节前景文字/图标的透明度
     */
    fun vibrancyColor(
        level: VibrancyLevel,
        isDark: Boolean,
        baseColor: Color = Color.Unspecified
    ): Color {
        val defaultBase = if (isDark) Color.White else Color.Black
        val targetBase = if (baseColor != Color.Unspecified) baseColor else defaultBase

        val alpha = when (level) {
            VibrancyLevel.PRIMARY -> 1.0f
            VibrancyLevel.SECONDARY -> 0.60f
            VibrancyLevel.TERTIARY -> 0.30f
            VibrancyLevel.QUATERNARY -> 0.18f
        }
        return targetBase.copy(alpha = alpha)
    }

    @Composable
    fun vibrancyColor(
        level: VibrancyLevel,
        baseColor: Color = Color.Unspecified
    ): Color = vibrancyColor(level, isSystemInDarkTheme(), baseColor)
}

/**
 * 颜色扩展函数：按 Vibrancy 等级快速叠加透明度
 */
fun Color.withVibrancy(level: VibrancyLevel): Color {
    val alphaMultiplier = when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.60f
        VibrancyLevel.TERTIARY -> 0.30f
        VibrancyLevel.QUATERNARY -> 0.18f
    }
    return this.copy(alpha = this.alpha * alphaMultiplier)
}

/**
 * Compose 修饰符：快速为任何组件施加 Apple HIG 材质层级与发丝线微光边缘
 */
fun Modifier.appleMaterial(
    thickness: MaterialThickness = MaterialThickness.REGULAR,
    shape: Shape? = null,
    hasBorder: Boolean = true,
    borderWidth: Dp = 0.5.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val bgColor = AppleMaterials.backgroundColor(thickness, isDark)
    val resolvedShape = shape ?: RectangleShape

    var m = this
    if (hasBorder) {
        m = m.border(AppleMaterials.glassBorder(isDark, borderWidth), resolvedShape)
    }
    m = m.background(bgColor, shape = resolvedShape)
    if (shape != null) {
        m = m.clip(shape)
    }
    m
}

/**
 * Compose 修饰符：为已有自定义背景或透明组件单独应用 0.5dp 发丝线微光玻璃边框
 */
fun Modifier.glassBorder(
    shape: Shape = RectangleShape,
    width: Dp = 0.5.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    border(AppleMaterials.glassBorder(isDark, width), shape = shape)
}

/**
 * Compose 修饰符：快速调整组件整体 Vibrancy 透明度
 */
fun Modifier.vibrancyAlpha(level: VibrancyLevel): Modifier = this.alpha(
    when (level) {
        VibrancyLevel.PRIMARY -> 1.0f
        VibrancyLevel.SECONDARY -> 0.60f
        VibrancyLevel.TERTIARY -> 0.30f
        VibrancyLevel.QUATERNARY -> 0.18f
    }
)

/**
 * 局部 Composable 容器：为子树中的 Text 和 Icon 自动注入 Vibrancy 内容色彩
 */
@Composable
fun ProvideVibrancy(
    level: VibrancyLevel,
    baseColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val vibrantColor = AppleMaterials.vibrancyColor(level, baseColor = baseColor)
    CompositionLocalProvider(
        LocalContentColor provides vibrantColor,
        content = content
    )
}
```

---

## 5. Downstream Component Integration Strategy

| Component | Target File | Material Thickness | Border Spec | Vibrancy Level | Usage Context |
|---|---|---|---|---|---|
| `PaperCard` | `core/designsystem/components/PaperCard.kt` | `THICK` | 0.5dp `glassBorder` (16dp squircle) | Title `PRIMARY`, Subtitle `SECONDARY` | Diary cards in `TimelineScreen` |
| `IosListSection` | `core/designsystem/components/IosListComponents.kt` | `THICK` | 0.5dp `glassBorder` (16dp squircle) | Header/Footer `SECONDARY` | Grouped cards in `SettingsScreen` |
| `IosListRow` | `core/designsystem/components/IosListComponents.kt` | Inherited from section | 0.5dp `separatorColor` (56dp indent) | Title `PRIMARY`, Subtitle `SECONDARY` | Rows inside Inset Grouped lists |
| `IosSegmentedControl` | `core/designsystem/components/IosSegmentedControl.kt` | `THIN` (track) | 0.5dp `glassBorder` (7dp squircle thumb) | Selected `PRIMARY`, Unselected `SECONDARY` | Filter tabs in `TimelineScreen` |
| `IosTabBar` | `ui/navigation/IosTabBar.kt` | `barBackgroundColor` (93%) | 0.5dp top hairline `glassBorder` | Active `PRIMARY`, Inactive `SECONDARY` | Bottom 4-tab navigation |
| `IosLargeTitleScaffold` | `core/designsystem/scaffold/IosLargeTitleScaffold.kt` | `barBackgroundColor` (93%) | 0.5dp bottom hairline `glassBorder` | Large title `PRIMARY`, Subtitle `SECONDARY` | Collapsible scrolling top bar |
| `IosActionSheet` | `core/designsystem/components/IosActionSheet.kt` | `ULTRA_THICK` | 0.5dp `glassBorder` (14dp squircle) | Actions `PRIMARY`, Destructive `0xFFFF3B30` | Contextual diary actions |
| `IosModalDialog` | `core/designsystem/components/IosModalDialog.kt` | `ULTRA_THICK` | 0.5dp `glassBorder` (14dp squircle) | Title `PRIMARY`, Message `SECONDARY` | PIN and Supabase credential alerts |
| `EditorScreen` Toolbar | `ui/editor/EditorScreen.kt` | `REGULAR` | 0.5dp `glassBorder` | Action glyphs `PRIMARY` | Bottom markdown styling bar |

---

## 6. Visual Preview & Test Strategy

### 6.1 Compose Previews

To visually validate all materials, vibrancy levels, and glass borders across both light and dark themes, include dedicated `@Preview` composables at the bottom of `AppleMaterial.kt`:

```kotlin
@androidx.compose.ui.tooling.preview.Preview(name = "Apple Materials - Light", showBackground = true, backgroundColor = 0xFFF2F2F7)
@androidx.compose.ui.tooling.preview.Preview(name = "Apple Materials - Dark", showBackground = true, backgroundColor = 0xFF000000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppleMaterialsMatrixPreview() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.androidx.compose.foundation.layout.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        MaterialThickness.entries.forEach { thickness ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .androidx.compose.foundation.layout.fillMaxWidth()
                    .androidx.compose.foundation.layout.height(56.dp)
                    .appleMaterial(
                        thickness = thickness,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        hasBorder = true
                    )
                    .androidx.compose.foundation.layout.padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            ) {
                androidx.compose.material3.Text(
                    text = "Thickness: ${thickness.name}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Vibrancy Levels Preview", showBackground = true)
@Composable
private fun VibrancyLevelsPreview() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .appleMaterial(thickness = MaterialThickness.THICK, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .androidx.compose.foundation.layout.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        VibrancyLevel.entries.forEach { level ->
            ProvideVibrancy(level = level) {
                androidx.compose.material3.Text(
                    text = "Vibrancy ${level.name} Text Sample",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
```

### 6.2 Pure JVM Unit Test Suite (`AppleMaterialTest.kt`)

Create `app/src/test/java/com/example/inkpaperdiary/core/designsystem/AppleMaterialTest.kt` to run with `./gradlew testDebugUnitTest`:

```kotlin
package com.example.inkpaperdiary.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMaterialTest {

    @Test
    fun testMaterialThicknessEnumCount() {
        assertEquals("MaterialThickness must have exactly 5 levels", 5, MaterialThickness.entries.size)
    }

    @Test
    fun testVibrancyLevelEnumCount() {
        assertEquals("VibrancyLevel must have exactly 4 tiers", 4, VibrancyLevel.entries.size)
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
        assertEquals(1.0f, darkPrimary.alpha, 0.01f)
        assertEquals(0.60f, darkSecondary.alpha, 0.01f)
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
}
```

### 6.3 Command Verification
Execute unit testing:
```bash
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.core.designsystem.AppleMaterialTest"
```
Execute full build:
```bash
./gradlew assembleDebug
```
Both commands must finish with 0 errors.

---

## 7. Conclusion & Next Steps

This investigation delivers the complete specification and ready-to-integrate Kotlin architecture for Apple Materials, Vibrancy, and Specular Glass Borders. The proposed enhancements decouple Compose UI logic from pure Kotlin color and stroke definitions, making testing effortless and equipping subsequent milestones (M1 list components, M2 tab bars and collapsible large title scaffolds, and M3–M5 screens) with a resilient, authentic HIG foundation.
