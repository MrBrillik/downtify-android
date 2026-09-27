package com.henriquesebastiao.downtify.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Downtify's Material 3 theme. The brand scheme is the default; [dynamicColor]
 * (Material You wallpaper colors, Android 12+) is opt-in from Settings.
 */
@Composable
fun DowntifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DowntifyDarkColors

        else -> DowntifyLightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DowntifyTypography,
        shapes = DowntifyShapes,
        content = content,
    )
}
