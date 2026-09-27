package com.henriquesebastiao.downtify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme

/**
 * A cover from the server (or anything Coil loads), clipped to [shape], with a
 * music-note placeholder while it loads, on error, or when there's no cover.
 */
@Composable
fun CoverArt(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    placeholderIcon: Int = DowntifyIcons.Song,
) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        if (url == null) {
            CoverPlaceholder(placeholderIcon)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { CoverPlaceholder(placeholderIcon) },
                error = { CoverPlaceholder(placeholderIcon) },
            )
        }
    }
}

@Composable
private fun CoverPlaceholder(icon: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.4f),
        )
    }
}

@PreviewLightDark
@Composable
private fun CoverArtPreview() {
    DowntifyTheme {
        Box(Modifier.padding(16.dp).fillMaxWidth()) {
            CoverArt(url = null, contentDescription = null, modifier = Modifier.size(96.dp))
        }
    }
}
