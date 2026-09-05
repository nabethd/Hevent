package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NaturalPrimaryDark,
    onPrimary = NaturalOnPrimaryDark,
    primaryContainer = NaturalPrimaryContainerDark,
    onPrimaryContainer = NaturalOnPrimaryContainerDark,
    secondary = NaturalSecondaryDark,
    onSecondary = NaturalOnSecondaryDark,
    secondaryContainer = NaturalSecondaryContainerDark,
    onSecondaryContainer = NaturalOnSecondaryContainerDark,
    tertiary = NaturalTertiaryDark,
    onTertiary = NaturalOnTertiaryDark,
    tertiaryContainer = NaturalTertiaryContainerDark,
    onTertiaryContainer = NaturalOnTertiaryContainerDark,
    background = NaturalBackgroundDark,
    onBackground = NaturalOnBackgroundDark,
    surface = NaturalSurfaceDark,
    onSurface = NaturalOnSurfaceDark,
    surfaceVariant = NaturalSurfaceVariantDark,
    onSurfaceVariant = NaturalOnSurfaceVariantDark,
    outline = NaturalOutlineDark,
    outlineVariant = NaturalOutlineVariantDark,
    error = NaturalErrorDark,
    onError = NaturalOnErrorDark,
    errorContainer = NaturalErrorContainerDark,
    onErrorContainer = NaturalOnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalPrimaryLight,
    onPrimary = NaturalOnPrimaryLight,
    primaryContainer = NaturalPrimaryContainerLight,
    onPrimaryContainer = NaturalOnPrimaryContainerLight,
    secondary = NaturalSecondaryLight,
    onSecondary = NaturalOnSecondaryLight,
    secondaryContainer = NaturalSecondaryContainerLight,
    onSecondaryContainer = NaturalOnSecondaryContainerLight,
    tertiary = NaturalTertiaryLight,
    onTertiary = NaturalOnTertiaryLight,
    tertiaryContainer = NaturalTertiaryContainerLight,
    onTertiaryContainer = NaturalOnTertiaryContainerLight,
    background = NaturalBackgroundLight,
    onBackground = NaturalOnBackgroundLight,
    surface = NaturalSurfaceLight,
    onSurface = NaturalOnSurfaceLight,
    surfaceVariant = NaturalSurfaceVariantLight,
    onSurfaceVariant = NaturalOnSurfaceVariantLight,
    outline = NaturalOutlineLight,
    outlineVariant = NaturalOutlineVariantLight,
    error = NaturalErrorLight,
    onError = NaturalOnErrorLight,
    errorContainer = NaturalErrorContainerLight,
    onErrorContainer = NaturalOnErrorContainerLight
)

/**
 * Whether the active scheme is the dark one. The category accent colours live outside the
 * Material scheme, so they need this to pick their variant.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MyApplicationTheme(
    // Follows the system setting. It used to be pinned to `false`, which left the whole dark
    // palette below unreachable and rendered a white app under a dark-mode status bar.
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // handcrafted palette, not the wallpaper's
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
