package com.henriquesebastiao.downtify.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * small 8 (chips, list covers) · medium 12 (tiles) · large 16 (cards, mini player)
 * · extraLarge 28 (big covers, sheets). Buttons and the search bar are fully rounded.
 */
val DowntifyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Shapes the M3 scale doesn't name. */
object DowntifyShape {
    /** "large-increased": the FAB-like play button. */
    val Fab = RoundedCornerShape(20.dp)
}

/** Spacing on the 4 dp grid. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Screen margins. */
    val screen = 16.dp
}
