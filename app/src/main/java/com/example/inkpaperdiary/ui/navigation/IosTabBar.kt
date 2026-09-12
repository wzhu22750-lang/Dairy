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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.PaperColors
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
