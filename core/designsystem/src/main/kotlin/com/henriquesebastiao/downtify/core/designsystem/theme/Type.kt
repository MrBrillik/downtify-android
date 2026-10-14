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
import com.henriquesebastiao.downtify.core.model.AppFont

// Three choices in Settings › Appearance:
//  - Downtify: Sora for display, headline and titleLarge; DM Sans for everything else.
//  - System: the phone's own font (FontFamily.Default).
//  - Google Sans Flex: one family for everything.
// Google downloadable fonts are fetched by Play services and shared between apps. Without
// Play services (de-Googled phones) Compose falls back to the system font.

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun family(name: String, vararg weights: FontWeight) =
    FontFamily(weights.map { Font(googleFont = GoogleFont(name), fontProvider = provider, weight = it) })

val Sora = family("Sora", FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold)
val DmSans = family("DM Sans", FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
val GoogleSansFlex = family(
    "Google Sans Flex",
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

private val base = Typography()

/** The family of headlines and of everything else, and whether the headline letter spacing applies. */
private data class Faces(val heading: FontFamily, val body: FontFamily, val tightHeadings: Boolean)

private fun facesOf(font: AppFont) = when (font) {
    AppFont.Downtify -> Faces(Sora, DmSans, tightHeadings = true)
    AppFont.System -> Faces(FontFamily.Default, FontFamily.Default, tightHeadings = false)
    AppFont.GoogleSansFlex -> Faces(GoogleSansFlex, GoogleSansFlex, tightHeadings = false)
}

/** The design's type scale in [font]. Sizes and weights are the same for all three. */
fun downtifyTypography(font: AppFont): Typography {
    val faces = facesOf(font)

    fun TextStyle.heading(size: Int, line: Int, tracking: Double = 0.0) = copy(
        fontFamily = faces.heading,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = line.sp,
        // The tracking was drawn for Sora; other typefaces keep their own spacing.
        letterSpacing = if (faces.tightHeadings) tracking.em else 0.em,
    )

    fun TextStyle.body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = copy(
        fontFamily = faces.body,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
    )

    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = faces.heading, fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontFamily = faces.heading, fontWeight = FontWeight.SemiBold),
        displaySmall = base.displaySmall.copy(fontFamily = faces.heading, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.heading(32, 40, -0.019),
        headlineMedium = base.headlineMedium.heading(28, 36, -0.014),
        headlineSmall = base.headlineSmall.heading(24, 32, -0.01),
        titleLarge = base.titleLarge.heading(22, 28, -0.005),
        titleMedium = base.titleMedium.body(16, 24, FontWeight.SemiBold),
        titleSmall = base.titleSmall.body(14, 20, FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.body(16, 24),
        bodyMedium = base.bodyMedium.body(14, 20),
        bodySmall = base.bodySmall.body(12, 16),
        labelLarge = base.labelLarge.body(14, 20, FontWeight.SemiBold),
        labelMedium = base.labelMedium.body(12, 16, FontWeight.SemiBold),
        labelSmall = base.labelSmall.body(11, 16, FontWeight.SemiBold),
    )
}

/** Downtify's own typeface: what previews and tests use. */
val DowntifyTypography = downtifyTypography(AppFont.Downtify)
