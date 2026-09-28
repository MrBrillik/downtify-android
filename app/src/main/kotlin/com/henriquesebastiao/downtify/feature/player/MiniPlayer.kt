package com.henriquesebastiao.downtify.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.CoverArt
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.component.rememberCoverColors
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.core.network.ServerUrls
import com.henriquesebastiao.downtify.core.player.PlayerState
import com.henriquesebastiao.downtify.ui.common.LocalCoverUrls
import com.henriquesebastiao.downtify.ui.common.PreviewData

/** Sits above the navigation bar while something is loaded; tapping it opens Now Playing. */
@Composable
fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onTogglePlay: () -> Unit, modifier: Modifier = Modifier) {
    val track = state.track
    val episode = state.episode
    if (track == null && episode == null) return
    val coverUrl = episode?.artworkUrl?.ifBlank { null }
        ?: track?.let { LocalCoverUrls.current.track(it.id.takeIf { _ -> it.hasCover }, ServerUrls.COVER_SMALL) }
    val colors = rememberCoverColors(coverUrl)
    Surface(
        color = colors.surface,
        contentColor = colors.onSurface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 6.dp,
        modifier = modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs).fillMaxWidth().height(64.dp),
    ) {
        Box {
            Row(
                Modifier.padding(start = Spacing.sm, end = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(
                            onClickLabel = stringResource(R.string.player_open),
                            role = Role.Button,
                            onClick = onOpen,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    CoverArt(url = coverUrl, contentDescription = null, modifier = Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track?.displayTitle ?: episode?.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track?.displayArtist ?: episode?.showName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        painterResource(if (state.isPlaying) DowntifyIcons.Pause else DowntifyIcons.Play),
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.player_pause else R.string.player_play,
                        ),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                color = colors.onSurface,
                trackColor = colors.onSurface.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.align(
                    Alignment.BottomCenter,
                ).padding(horizontal = Spacing.md).fillMaxWidth().height(2.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MiniPlayerPreview() {
    DowntifyTheme {
        Surface {
            MiniPlayer(
                state = PlayerState(
                    track = PreviewData.tracks[1],
                    isPlaying = true,
                    positionMs = 70_000,
                    durationMs = 239_000,
                ),
                onOpen = {},
                onTogglePlay = {},
            )
        }
    }
}
