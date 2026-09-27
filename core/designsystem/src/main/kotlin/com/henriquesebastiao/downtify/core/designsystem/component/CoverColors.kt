package com.henriquesebastiao.downtify.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Surface and content colors taken from a cover. */
@Immutable
data class CoverColors(val surface: Color, val onSurface: Color, val onSurfaceVariant: Color)

/**
 * The colors Now Playing, the album header and the widget take from a cover:
 * the Palette API's dark muted swatch (light muted in the light theme), falling
 * back to the theme's surface. The play button keeps `primary`. Animated when
 * the cover changes.
 */
@Composable
fun rememberCoverColors(
    coverUrl: String?,
    dark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
): CoverColors {
    val scheme = MaterialTheme.colorScheme
    val fallback = CoverColors(scheme.surface, scheme.onSurface, scheme.onSurfaceVariant)
    var target by remember { mutableStateOf<CoverColors?>(null) }
    val context = LocalContext.current

    LaunchedEffect(coverUrl, dark) {
        target = if (coverUrl == null) {
            null
        } else {
            val request = ImageRequest.Builder(context).data(coverUrl).size(PALETTE_SIZE).allowHardware(false).build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
            val bitmap = result?.image?.toBitmap()
            bitmap?.let {
                withContext(Dispatchers.Default) {
                    val palette = Palette.from(it).generate()
                    val swatch = if (dark) {
                        palette.darkMutedSwatch ?: palette.mutedSwatch
                    } else {
                        palette.lightMutedSwatch
                            ?: palette.mutedSwatch
                    }
                    swatch?.let { s -> colorsFor(Color(s.rgb), dark) }
                }
            }
        }
    }

    val colors = target ?: fallback
    val surface by animateColorAsState(colors.surface, tween(ANIMATION_MS), label = "coverSurface")
    val onSurface by animateColorAsState(colors.onSurface, tween(ANIMATION_MS), label = "coverOnSurface")
    val onVariant by animateColorAsState(colors.onSurfaceVariant, tween(ANIMATION_MS), label = "coverOnVariant")
    return CoverColors(surface, onSurface, onVariant)
}

/** Keeps the swatch dark (or light) enough for text, and picks text colors with contrast. */
private fun colorsFor(swatch: Color, dark: Boolean): CoverColors {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(swatch.toArgb(), hsl)
    hsl[2] = if (dark) hsl[2].coerceIn(0.12f, 0.24f) else hsl[2].coerceIn(0.82f, 0.9f)
    val surface = Color(ColorUtils.HSLToColor(hsl))
    val text = if (dark) Color(0xFFEFF4F3) else Color(0xFF141715)
    return CoverColors(surface = surface, onSurface = text, onSurfaceVariant = text.copy(alpha = 0.72f))
}

private const val PALETTE_SIZE = 128
private const val ANIMATION_MS = 450
