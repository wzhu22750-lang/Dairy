package com.example.inkpaperdiary.core.designsystem

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.inkpaperdiary.core.designsystem.interaction.NoIndication

private val LightColorScheme = lightColorScheme(
    primary = InkPalette.InkLight,
    onPrimary = InkPalette.PaperLight,
    primaryContainer = InkPalette.WashLight,
    onPrimaryContainer = InkPalette.InkLight,
    secondary = InkPalette.InkSoftLight,
    onSecondary = InkPalette.PaperLight,
    secondaryContainer = InkPalette.WashLight,
    onSecondaryContainer = InkPalette.InkLight,
    tertiary = InkPalette.InkSoftLight,
    onTertiary = InkPalette.PaperLight,
    background = InkPalette.PaperLight,
    onBackground = InkPalette.InkLight,
    surface = InkPalette.PaperLight,
    onSurface = InkPalette.InkLight,
    surfaceVariant = InkPalette.WashLight,
    onSurfaceVariant = InkPalette.InkSoftLight,
    outline = InkPalette.HairlineLight,
    outlineVariant = InkPalette.HairlineLight,
    error = InkPalette.Cinnabar
)

private val DarkColorScheme = darkColorScheme(
    primary = InkPalette.InkDark,
    onPrimary = InkPalette.PaperDark,
    primaryContainer = InkPalette.WashDark,
    onPrimaryContainer = InkPalette.InkDark,
    secondary = InkPalette.InkSoftDark,
    onSecondary = InkPalette.PaperDark,
    secondaryContainer = InkPalette.WashDark,
    onSecondaryContainer = InkPalette.InkDark,
    tertiary = InkPalette.InkSoftDark,
    onTertiary = InkPalette.PaperDark,
    background = InkPalette.PaperDark,
    onBackground = InkPalette.InkDark,
    surface = InkPalette.PaperDark,
    onSurface = InkPalette.InkDark,
    surfaceVariant = InkPalette.WashDark,
    onSurfaceVariant = InkPalette.InkSoftDark,
    outline = InkPalette.HairlineDark,
    outlineVariant = InkPalette.HairlineDark,
    error = InkPalette.Cinnabar
)

/**
 * Dairy Design System 2.0 主题。
 *
 * @param darkTheme 是否使用夜间纸面；由 ThemeMode（跟随系统/浅色/夜间）在入口处解析后传入。
 * @param reading 阅读排版设置（字体/字号/行距/页宽），注入 LocalReadingSettings。
 */
@Composable
fun PaperDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reading: ReadingSettings = ReadingSettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
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
            LocalReadingSettings provides reading,
            LocalRippleConfiguration provides null,
            LocalIndication provides NoIndication,
            content = content
        )
    }
}
