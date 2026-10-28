package com.altankoc.nidado.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepOcean,
    onPrimary = White,
    primaryContainer = FrostBlue,
    onPrimaryContainer = DeepOcean,

    secondary = VibrantTeal,
    onSecondary = White,
    secondaryContainer = FrostBlue,
    onSecondaryContainer = VibrantTeal,

    tertiary = SoftTeal,
    onTertiary = White,
    tertiaryContainer = FrostBlue,
    onTertiaryContainer = DeepOcean,

    background = OffWhite,
    onBackground = Black,

    surface = OffWhite,
    onSurface = Black,
    surfaceVariant = FrostBlue,
    onSurfaceVariant = DeepOcean,

    error = ErrorRed,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftTealDark,
    onPrimary = SurfaceDark,
    primaryContainer = DeepOceanDark,
    onPrimaryContainer = FrostBlue,

    secondary = VibrantTealDark,
    onSecondary = SurfaceDark,
    secondaryContainer = DeepOceanDark,
    onSecondaryContainer = SoftTealDark,

    tertiary = SoftTeal,
    onTertiary = SurfaceDark,
    tertiaryContainer = DeepOceanDark,
    onTertiaryContainer = SoftTealDark,

    background = SurfaceDark,
    onBackground = OffWhite,

    surface = SurfaceDark,
    onSurface = OffWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SoftTealDark,

    error = ErrorRedDark,
    onError = Black
)

@Composable
fun NidaDoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}