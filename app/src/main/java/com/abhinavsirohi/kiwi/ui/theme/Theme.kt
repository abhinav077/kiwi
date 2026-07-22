package com.abhinavsirohi.kiwi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhinavsirohi.kiwi.data.local.KiwiFontPreference
import com.abhinavsirohi.kiwi.data.local.KiwiThemePreference

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

private val KiwiSunriseColorScheme = lightColorScheme(
    primary = Color(0xFF8B3F48),
    onPrimary = KiwiWhite,
    secondary = KiwiCoralRose,
    onSecondary = KiwiCharcoal,
    tertiary = KiwiButter,
    onTertiary = KiwiCharcoal,
    background = Color(0xFFFFF3E8),
    onBackground = KiwiCharcoal,
    surface = Color(0xFFFFFBF5),
    onSurface = KiwiCharcoal,
    surfaceVariant = KiwiPeach,
    onSurfaceVariant = KiwiCharcoal,
    outline = KiwiWarmGray,
)

private val KiwiGroveColorScheme = lightColorScheme(
    primary = KiwiForest,
    onPrimary = KiwiWhite,
    secondary = Color(0xFF668052),
    onSecondary = KiwiWhite,
    tertiary = KiwiPowderBlue,
    onTertiary = KiwiCharcoal,
    background = Color(0xFFF3F4E8),
    onBackground = KiwiCharcoal,
    surface = Color(0xFFFCFAF0),
    onSurface = KiwiCharcoal,
    surfaceVariant = KiwiPistachio,
    onSurfaceVariant = KiwiCharcoal,
    outline = KiwiWarmGray,
)

val KiwiShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(KiwiDimensions.standardCardRadius),
    large = androidx.compose.foundation.shape.RoundedCornerShape(KiwiDimensions.heroCardRadius)
)

@Composable
fun KiwiTheme(
    themePreference: KiwiThemePreference = KiwiThemePreference.SYSTEM,
    fontPreference: KiwiFontPreference = KiwiFontPreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreference) {
        KiwiThemePreference.SYSTEM -> if (isSystemInDarkTheme()) KiwiDarkColorScheme else KiwiLightColorScheme
        KiwiThemePreference.LIGHT -> KiwiLightColorScheme
        KiwiThemePreference.DARK -> KiwiDarkColorScheme
        KiwiThemePreference.SUNRISE -> KiwiSunriseColorScheme
        KiwiThemePreference.GROVE -> KiwiGroveColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = kiwiTypography(fontPreference),
        shapes = KiwiShapes,
        content = content
    )
}
