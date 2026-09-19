package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.PaperTypography

/**
 * Apple HIG Sliding Pill Segmented Control
 *
 * 遵循 iOS UISegmentedControl 设计规范：
 * 1. 32dp 高度连续圆角滑轨 (9dp squircle)
 * 2. 2dp 内边距与 28dp 高度平滑悬浮药丸滑块 (7dp squircle，2dp 柔和投影)
 * 3. 弹簧动力学无震荡过渡 (Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
 * 4. 0.5dp 细发丝微分割线：在滑块移动至相邻项时平滑消隐
 * 5. 零 Material 水波纹，触感反馈 (TextHandleMove)
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() },
    itemIcon: (@Composable (T) -> Unit)? = null
) {
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    IosSegmentedControl(
        items = items.map(itemLabel),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            if (index in items.indices) {
                onItemSelected(items[index])
            }
        },
        modifier = modifier
    )
}

/**
 * String-based index overload of IosSegmentedControl
 */
@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current

    val trackBgColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val thumbBgColor = if (isDark) Color(0xFF636366) else Color.White
    val selectedTextColor = if (isDark) Color.White else Color.Black
    val unselectedTextColor = PaperColors.MonoGray500
    val separatorColor = if (isDark) Color(0x38FFFFFF) else Color(0x2E000000)

    val validIndex = selectedIndex.coerceIn(0, items.size - 1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(trackBgColor)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color(0x2EFFFFFF) else Color(0x14000000),
                shape = RoundedCornerShape(9.dp)
            )
            .padding(2.dp)
    ) {
        val totalWidth = maxWidth
        val segmentWidth = totalWidth / items.size
        val thumbOffset = segmentWidth * validIndex

        val animatedThumbOffset by animateDpAsState(
            targetValue = thumbOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "SegmentedControlThumbOffset"
        )

        // Floating Pill Thumb
        Box(
            modifier = Modifier
                .offset(x = animatedThumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(7.dp), clip = false)
                .background(thumbBgColor, shape = RoundedCornerShape(7.dp))
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
                    shape = RoundedCornerShape(7.dp)
                )
        )

        // Dynamic 0.5dp Hairline Dividers between unselected segments
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until items.size - 1) {
                Spacer(modifier = Modifier.weight(1f))
                val isDividerHidden = (i == validIndex || i + 1 == validIndex)
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(if (isDividerHidden) Color.Transparent else separatorColor)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Segment Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == validIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                    animationSpec = tween(durationMillis = 150),
                    label = "SegmentedControlTextColor_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (index != validIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectedIndexChange(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = PaperTypography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
