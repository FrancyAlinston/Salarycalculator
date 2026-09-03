package com.example.salarycalculator.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.salarycalculator.domain.ThemeMode
import com.example.salarycalculator.domain.ThemePalette

private fun buildColorScheme(palette: ThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        ThemePalette.EMERALD -> if (isDark) {
            darkColorScheme(
                primary = Emerald80,
                onPrimary = Emerald40,
                primaryContainer = Emerald40,
                onPrimaryContainer = Emerald80,
                secondary = Teal80,
                onSecondary = Teal40,
                secondaryContainer = Teal40,
                onSecondaryContainer = Teal80,
                tertiary = Amber80,
                onTertiary = Amber40,
                background = Slate950,
                onBackground = Slate100,
                surface = Slate900,
                onSurface = Slate100,
                surfaceVariant = Slate800,
                onSurfaceVariant = Slate300,
                outline = Slate600,
                outlineVariant = Slate700,
                error = Rose80,
                onError = Rose40
            )
        } else {
            lightColorScheme(
                primary = Emerald60,
                onPrimary = Color.White,
                primaryContainer = EmeraldContainerLight,
                onPrimaryContainer = Emerald40,
                secondary = Teal60,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCFBF1),
                onSecondaryContainer = Teal40,
                tertiary = Amber60,
                onTertiary = Color.White,
                background = Slate50,
                onBackground = Slate900,
                surface = Color.White,
                onSurface = Slate900,
                surfaceVariant = Slate100,
                onSurfaceVariant = Slate700,
                outline = Slate300,
                outlineVariant = Slate200,
                error = Rose60,
                onError = Color.White
            )
        }

        ThemePalette.VIOLET -> if (isDark) {
            darkColorScheme(
                primary = Violet80,
                onPrimary = Violet40,
                primaryContainer = Violet40,
                onPrimaryContainer = Violet80,
                secondary = Indigo80,
                onSecondary = Indigo20,
                secondaryContainer = Indigo40,
                onSecondaryContainer = Indigo80,
                tertiary = Rose80,
                onTertiary = Rose40,
                background = Slate950,
                onBackground = Slate100,
                surface = Slate900,
                onSurface = Slate100,
                surfaceVariant = Slate800,
                onSurfaceVariant = Slate300,
                outline = Slate600,
                outlineVariant = Slate700,
                error = Rose80,
                onError = Rose40
            )
        } else {
            lightColorScheme(
                primary = Violet60,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3E8FF),
                onPrimaryContainer = Violet40,
                secondary = Indigo60,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0E7FF),
                onSecondaryContainer = Indigo40,
                tertiary = Rose60,
                onTertiary = Color.White,
                background = Slate50,
                onBackground = Slate900,
                surface = Color.White,
                onSurface = Slate900,
                surfaceVariant = Slate100,
                onSurfaceVariant = Slate700,
                outline = Slate300,
                outlineVariant = Slate200,
                error = Rose60,
                onError = Color.White
            )
        }

        ThemePalette.AMBER -> if (isDark) {
            darkColorScheme(
                primary = Amber80,
                onPrimary = Amber40,
                primaryContainer = Amber40,
                onPrimaryContainer = Amber80,
                secondary = Emerald80,
                onSecondary = Emerald40,
                secondaryContainer = Emerald40,
                onSecondaryContainer = Emerald80,
                tertiary = Teal80,
                onTertiary = Teal40,
                background = Slate950,
                onBackground = Slate100,
                surface = Slate900,
                onSurface = Slate100,
                surfaceVariant = Slate800,
                onSurfaceVariant = Slate300,
                outline = Slate600,
                outlineVariant = Slate700,
                error = Rose80,
                onError = Rose40
            )
        } else {
            lightColorScheme(
                primary = Amber60,
                onPrimary = Color.White,
                primaryContainer = AmberContainerLight,
                onPrimaryContainer = Amber40,
                secondary = Emerald60,
                onSecondary = Color.White,
                secondaryContainer = EmeraldContainerLight,
                onSecondaryContainer = Emerald40,
                tertiary = Teal60,
                onTertiary = Color.White,
                background = Slate50,
                onBackground = Slate900,
                surface = Color.White,
                onSurface = Slate900,
                surfaceVariant = Slate100,
                onSurfaceVariant = Slate700,
                outline = Slate300,
                outlineVariant = Slate200,
                error = Rose60,
                onError = Color.White
            )
        }

        ThemePalette.OCEAN -> if (isDark) {
            darkColorScheme(
                primary = Indigo80,
                onPrimary = Indigo20,
                primaryContainer = Indigo40,
                onPrimaryContainer = Indigo80,
                secondary = Teal80,
                onSecondary = Teal40,
                secondaryContainer = Teal40,
                onSecondaryContainer = Teal80,
                tertiary = Violet80,
                onTertiary = Violet40,
                tertiaryContainer = Violet40,
                onTertiaryContainer = Violet80,
                background = Slate950,
                onBackground = Slate100,
                surface = Slate900,
                onSurface = Slate100,
                surfaceVariant = Slate800,
                onSurfaceVariant = Slate300,
                outline = Slate600,
                outlineVariant = Slate700,
                error = Rose80,
                onError = Rose40,
                errorContainer = RoseContainerDark,
                onErrorContainer = Rose80
            )
        } else {
            lightColorScheme(
                primary = Indigo60,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0E7FF),
                onPrimaryContainer = Indigo40,
                secondary = Teal60,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCFBF1),
                onSecondaryContainer = Teal40,
                tertiary = Violet60,
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF3E8FF),
                onTertiaryContainer = Violet40,
                background = Slate50,
                onBackground = Slate900,
                surface = Color.White,
                onSurface = Slate900,
                surfaceVariant = Slate100,
                onSurfaceVariant = Slate700,
                outline = Slate300,
                outlineVariant = Slate200,
                error = Rose60,
                onError = Color.White,
                errorContainer = RoseContainerLight,
                onErrorContainer = Rose40
            )
        }
    }
}

@Composable
fun SalaryCalculatorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.OCEAN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SOLAR -> com.example.salarycalculator.domain.SolarThemeScheduler.isSolarNightTime()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildColorScheme(themePalette, isDark)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
