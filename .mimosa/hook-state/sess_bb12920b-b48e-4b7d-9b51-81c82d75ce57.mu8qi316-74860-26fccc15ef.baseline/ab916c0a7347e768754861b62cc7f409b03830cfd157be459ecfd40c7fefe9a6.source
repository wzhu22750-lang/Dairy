package com.example.inkpaperdiary.core.designsystem.interaction

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.ui.node.DelegatableNode
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
 * Overload supporting specification naming conventions (scaleDown, dimAlpha, hapticFeedback).
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
 * A modern, non-deprecated no-op [IndicationNodeFactory] that draws nothing,
 * effectively suppressing all Foundation indications.
 */
object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {}
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is NoIndication
}

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
        LocalIndication provides NoIndication,
        content = content
    )
}
