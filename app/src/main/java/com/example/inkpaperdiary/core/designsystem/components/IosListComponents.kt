package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.*
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick

/**
 * Apple HIG Inset Grouped Section Container (16dp squircle container)
 * 遵循 UICollectionLayoutListConfiguration.Appearance.insetGrouped 规范
 */
@Composable
fun IosListSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    header: String? = title,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val headerText = header ?: title
        if (!headerText.isNullOrBlank()) {
            Text(
                text = headerText.uppercase(),
                style = PaperTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 10.dp)
            )
        }
    }
}

/**
 * Apple HIG Inset Grouped Row (String-based overload)
 */
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
    IosListRow(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = PaperTypography.bodyLarge.copy(
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        subtitle = subtitle?.let { sub ->
            {
                Text(
                    text = sub,
                    style = PaperTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = icon,
        trailingContent = trailing,
        onClick = onClick,
        showDivider = showDivider
    )
}

/**
 * Apple HIG Inset Grouped Row Slot Overload
 */
@Composable
fun IosListRow(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    val rowModifier = if (onClick != null) {
        modifier.iosClick(onClick = onClick)
    } else {
        modifier
    }

    val isDark = isSystemInDarkTheme()
    val dividerColor = AppleMaterials.separatorColor(isDark)

    Column(modifier = rowModifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingIcon()
                    }
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    title()
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        subtitle()
                    }
                }
            }

            if (trailingContent != null) {
                Box(
                    modifier = Modifier.padding(start = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailingContent()
                }
            }
        }

        if (showDivider) {
            val indentStart = if (leadingIcon != null) 56.dp else 16.dp
            HorizontalDivider(
                modifier = Modifier.padding(start = indentStart),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

/**
 * Apple HIG Navigation Row with Chevron Disclosure
 */
@Composable
fun IosNavigationRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    IosListRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        showDivider = showDivider,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = PaperTypography.bodyLarge.copy(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    )
}

/**
 * Apple HIG Switch Row
 */
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
) {
    val haptics = LocalHapticFeedback.current
    val rowModifier = if (enabled) {
        modifier.iosClick(
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
        )
    } else {
        modifier
    }

    IosListRow(
        title = title,
        modifier = rowModifier,
        subtitle = subtitle,
        icon = icon,
        onClick = null,
        showDivider = showDivider,
        trailing = {
            IosSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * Apple HIG UISwitch
 *
 * 尺寸：51dp x 31dp
 * 轨道：选中为 Apple Green (#34C759)，未选中为淡灰
 * 滑块：27dp 纯白圆形带柔和阴影，弹簧物理位移 (2dp -> 22dp)
 * 零水波纹，触感反馈
 */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current

    // 选中轨道 = 墨色；Dairy 2.0 全应用无彩色（破坏性操作除外）
    val trackCheckedColor = MaterialTheme.colorScheme.onBackground
    val trackUncheckedColor = if (isDark) InkPalette.WashDark else InkPalette.WashLight

    val animatedTrackColor by animateColorAsState(
        targetValue = if (checked) trackCheckedColor else trackUncheckedColor,
        animationSpec = tween(durationMillis = 200),
        label = "IosSwitchTrackColor"
    )

    val thumbTargetOffset = if (checked) 22.dp else 2.dp
    val animatedThumbOffset by animateDpAsState(
        targetValue = thumbTargetOffset,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IosSwitchThumbOffset"
    )

    val clickModifier = if (enabled && onCheckedChange != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CapsuleShape)
            .background(if (enabled) animatedTrackColor else animatedTrackColor.copy(alpha = 0.5f))
            .then(clickModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = animatedThumbOffset)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, shape = CircleShape)
        )
    }
}

/**
 * 30dp 图标位：纸张化后不再绘制彩色方块，仅以墨色图标安静的占位。
 */
@Composable
fun IosSquircleIconBox(
    icon: ImageVector,
    contentDescription: String? = null,
    backgroundColor: Color = Color.Transparent,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
    }
}
