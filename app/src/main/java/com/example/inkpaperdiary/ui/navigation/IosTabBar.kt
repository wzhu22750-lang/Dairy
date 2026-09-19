package com.example.inkpaperdiary.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.inkpaperdiary.core.designsystem.interaction.iosTabClick

/**
 * Dairy 2.0 — 底部索引条 (Flat Ink Tab Bar)
 *
 * 原 iOS 毛玻璃 TabBar 压平为一条安静的索引：
 * 97% 纸色底、0.5dp 发丝线上边、墨色激活 / 淡墨未激活、无强调色。
 */
object AppleTabDefaults {
    val BarHeight: Dp = 48.dp
    val HairlineBorderWidth: Dp = 0.5.dp
    val IconSize: Dp = 22.dp
    val LabelFontSize: TextUnit = 10.sp

    val ColorTransitionSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 200)
}

/**
 * 根导航 4 栏契约：
 * - JOURNAL: 书页, Outlined.Book -> Filled.Book
 * - CALENDAR: 目录, Outlined.CalendarMonth -> Filled.CalendarMonth
 * - MEMORIES: 回声, Outlined.History -> Filled.History
 * - SETTINGS: 设置, Outlined.Settings -> Filled.Settings
 */
enum class IosTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    JOURNAL(
        label = "书页",
        icon = Icons.Outlined.Book,
        selectedIcon = Icons.Filled.Book
    ),
    CALENDAR(
        label = "目录",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth
    ),
    MEMORIES(
        label = "回声",
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History
    ),
    SETTINGS(
        label = "设置",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}

/** 扁平墨色底部索引条。 */
@Composable
fun IosTabBar(
    selectedTab: IosTab,
    onTabSelected: (IosTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.onBackground
    // 未激活图标保持清晰可辨的墨灰（参考墨水屏阅读器的底部索引条）
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppleMaterials.barBackgroundColor())
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    ) {
        // 0.5dp 发丝线上边
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppleTabDefaults.HairlineBorderWidth)
                .background(AppleMaterials.separatorColor())
        )

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

@Composable
private fun IosTabItem(
    tab: IosTab,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = AppleTabDefaults.ColorTransitionSpec,
        label = "tabColor_${tab.name}"
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
            tint = contentColor,
            modifier = Modifier.size(AppleTabDefaults.IconSize)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = AppleTabDefaults.LabelFontSize,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.5.sp,
            color = contentColor,
            maxLines = 1
        )
    }
}
