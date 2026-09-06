package com.example.inkpaperdiary.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PaperColors.MonoBlack,
    onPrimary = PaperColors.MonoWhite,
    primaryContainer = PaperColors.MonoGray200,
    onPrimaryContainer = PaperColors.MonoBlack,
    secondary = PaperColors.MonoGray500,
    onSecondary = PaperColors.MonoWhite,
    secondaryContainer = PaperColors.MonoGray100,
    onSecondaryContainer = PaperColors.MonoBlack,
    tertiary = PaperColors.MonoBlack,
    onTertiary = PaperColors.MonoWhite,
    tertiaryContainer = PaperColors.MonoGray200,
    onTertiaryContainer = PaperColors.MonoBlack,
    background = PaperColors.MonoGray100,
    onBackground = PaperColors.MonoBlack,
    surface = PaperColors.MonoWhite,
    onSurface = PaperColors.MonoBlack,
    surfaceVariant = PaperColors.MonoWhite,
    onSurfaceVariant = PaperColors.MonoGray600,
    outline = PaperColors.MonoGray200,
    outlineVariant = PaperColors.MonoGray100
)

private val DarkColorScheme = darkColorScheme(
    primary = PaperColors.MonoWhite,
    onPrimary = PaperColors.MonoBlack,
    primaryContainer = PaperColors.MonoGray800,
    onPrimaryContainer = PaperColors.MonoWhite,
    secondary = PaperColors.MonoGray500,
    onSecondary = PaperColors.MonoBlack,
    secondaryContainer = PaperColors.MonoGray900,
    onSecondaryContainer = PaperColors.MonoWhite,
    tertiary = PaperColors.MonoWhite,
    onTertiary = PaperColors.MonoBlack,
    tertiaryContainer = PaperColors.MonoGray800,
    onTertiaryContainer = PaperColors.MonoWhite,
    background = PaperColors.MonoBlackBg,
    onBackground = PaperColors.MonoWhite,
    surface = PaperColors.MonoGray900,
    onSurface = PaperColors.MonoWhite,
    surfaceVariant = PaperColors.MonoGray900,
    onSurfaceVariant = PaperColors.MonoGray500,
    outline = PaperColors.MonoGray700,
    outlineVariant = PaperColors.MonoGray800
)

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
        shapes = PaperShapes,
        content = content
    )
}
