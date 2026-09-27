package com.henriquesebastiao.downtify.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.henriquesebastiao.downtify.core.designsystem.R

// Sora for display, headline and titleLarge; DM Sans for everything else.
// Google downloadable fonts: fetched by Play services and shared between apps.
// Without Play services (de-Googled phones) Compose falls back to the system font.

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun family(name: String, vararg weights: FontWeight) =
    FontFamily(weights.map { Font(googleFont = GoogleFont(name), fontProvider = provider, weight = it) })

val Sora = family("Sora", FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold)
val DmSans = family("DM Sans", FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private val base = Typography()

private fun TextStyle.sora(size: Int, line: Int, tracking: Double = 0.0) = copy(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun TextStyle.dmSans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = copy(
    fontFamily = DmSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

val DowntifyTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = Sora, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.sora(32, 40, -0.019),
    headlineMedium = base.headlineMedium.sora(28, 36, -0.014),
    headlineSmall = base.headlineSmall.sora(24, 32, -0.01),
    titleLarge = base.titleLarge.sora(22, 28, -0.005),
    titleMedium = base.titleMedium.dmSans(16, 24, FontWeight.SemiBold),
    titleSmall = base.titleSmall.dmSans(14, 20, FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.dmSans(16, 24),
    bodyMedium = base.bodyMedium.dmSans(14, 20),
    bodySmall = base.bodySmall.dmSans(12, 16),
    labelLarge = base.labelLarge.dmSans(14, 20, FontWeight.SemiBold),
    labelMedium = base.labelMedium.dmSans(12, 16, FontWeight.SemiBold),
    labelSmall = base.labelSmall.dmSans(11, 16, FontWeight.SemiBold),
)
