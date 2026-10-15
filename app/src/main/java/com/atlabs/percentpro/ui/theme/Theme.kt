package com.atlabs.percentpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColorScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = NeutralWhite,
    primaryContainer = Lavender,
    onPrimaryContainer = DeepPurple,
    secondary = MainPurple,
    onSecondary = NeutralWhite,
    secondaryContainer = Lavender,
    onSecondaryContainer = DeepPurple,
    tertiary = MainPurple,
    onTertiary = NeutralWhite,
    tertiaryContainer = Lavender,
    onTertiaryContainer = DeepPurple,
    background = NeutralSurface,
    onBackground = NeutralOnSurface,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    surfaceContainerLowest = NeutralWhite,
    surfaceContainerLow = NeutralSurface,
    surfaceContainer = NeutralSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DeepPurpleDark,
    onPrimary = Color0B0A14,
    primaryContainer = DeepPurple,
    onPrimaryContainer = DeepPurpleDark,
    secondary = MainPurpleDark,
    onSecondary = Color0B0A14,
    secondaryContainer = MainPurple,
    onSecondaryContainer = DeepPurpleDark,
    tertiary = MainPurpleDark,
    onTertiary = Color0B0A14,
    tertiaryContainer = MainPurple,
    onTertiaryContainer = DeepPurpleDark,
    background = NeutralSurfaceDark,
    onBackground = NeutralOnSurfaceDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    surfaceContainerLowest = Color0B0A14,
    surfaceContainerLow = NeutralSurfaceDark,
    surfaceContainer = NeutralSurfaceVariantDark,
    outline = NeutralOnSurfaceVariantDark,
    outlineVariant = NeutralOutlineStrong
)

@Composable
fun PercentProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = PercentProTypography,
        shapes = PercentProShapes,
        content = content
    )
}