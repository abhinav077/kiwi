package com.abhinavsirohi.kiwi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KiwiDarkColorScheme = darkColorScheme(
    primary = KiwiSage,
    onPrimary = KiwiCharcoal,
    secondary = KiwiBlush,
    onSecondary = KiwiCharcoal,
    tertiary = KiwiPeriwinkle,
    onTertiary = KiwiCharcoal,
    background = KiwiCharcoal,
    onBackground = KiwiPaper,
    surface = Color(0xFF3A3530),
    onSurface = KiwiPaper,
    surfaceVariant = Color(0xFF4B443D),
    onSurfaceVariant = KiwiWarmBeige,
    outline = KiwiWarmGray
)

private val KiwiLightColorScheme = lightColorScheme(
    primary = KiwiForest,
    onPrimary = KiwiWhite,
    secondary = KiwiCoralRose,
    onSecondary = KiwiWhite,
    tertiary = KiwiLavender,
    onTertiary = KiwiCharcoal,
    background = KiwiPaper,
    onBackground = KiwiCharcoal,
    surface = KiwiCream,
    onSurface = KiwiCharcoal,
    surfaceVariant = KiwiWarmBeige,
    onSurfaceVariant = KiwiWarmGray,
    outline = KiwiWarmGray
)

val KiwiShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(KiwiDimensions.standardCardRadius),
    large = androidx.compose.foundation.shape.RoundedCornerShape(KiwiDimensions.heroCardRadius)
)

@Composable
fun KiwiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) KiwiDarkColorScheme else KiwiLightColorScheme,
        typography = KiwiTypography,
        shapes = KiwiShapes,
        content = content
    )
}
