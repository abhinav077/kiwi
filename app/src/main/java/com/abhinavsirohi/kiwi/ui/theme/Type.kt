package com.abhinavsirohi.kiwi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhinavsirohi.kiwi.R
import com.abhinavsirohi.kiwi.data.local.KiwiFontPreference

private val KiwiBodyFont = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
)

private val KiwiDisplayFont = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal),
)

val KiwiTypography = kiwiTypography(KiwiFontPreference.SYSTEM)

fun kiwiTypography(fontPreference: KiwiFontPreference): Typography {
    // Keep the parameter for compatibility with the existing settings flow. The
    // approved visual direction now uses these two families consistently.
    @Suppress("UNUSED_VARIABLE")
    val selectedPreference = fontPreference
    return Typography(
        displayLarge = TextStyle(
            fontFamily = KiwiDisplayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 38.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = KiwiDisplayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = KiwiBodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = KiwiBodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = KiwiBodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = KiwiBodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
    )
}
