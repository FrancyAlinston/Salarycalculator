package com.example.salarycalculator.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearTeal = Color(0xFF14B8A6)
val WearTealDark = Color(0xFF0F766E)
val WearAmber = Color(0xFFF59E0B)
val WearEmerald = Color(0xFF10B981)
val WearBlue = Color(0xFF3B82F6)
val WearPurple = Color(0xFFA855F7)
val WearRose = Color(0xFFF43F5E)
val WearCyan = Color(0xFF22D3EE)
val WearSurface = Color(0xFF1E1E24)
val WearSurfaceDark = Color(0xFF121216)
val WearTextPrimary = Color(0xFFFFFFFF)
val WearTextSecondary = Color(0xFFA1A1AA)

private val wearColorPalette = Colors(
    primary = WearTeal,
    primaryVariant = WearTealDark,
    secondary = WearAmber,
    secondaryVariant = Color(0xFFD97706),
    background = Color(0xFF000000),
    surface = WearSurface,
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = WearTextPrimary,
    onSurface = WearTextPrimary,
    onSurfaceVariant = WearTextSecondary,
    error = WearRose
)

@Composable
fun WearSalaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = wearColorPalette,
        content = content
    )
}
