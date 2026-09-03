package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to light mode for crisp, clean presentation
    dynamicColor: Boolean = false, // Use our handcrafted palette for distinct Hebrew branding
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
