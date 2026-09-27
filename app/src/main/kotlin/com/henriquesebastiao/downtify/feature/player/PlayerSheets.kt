package com.henriquesebastiao.downtify.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.PlayingBars
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.core.model.LrcParser
import com.henriquesebastiao.downtify.core.player.PlayerState

/** Lyrics, synced to the playhead when the server has an LRC; tap a line to jump to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(lyrics: LyricsState, positionMs: Long, onSeek: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(stringResource(R.string.player_lyrics))
        when (lyrics) {
            LyricsState.Loading -> Box(
                Modifier.fillMaxWidth().heightIn(min = 200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is LyricsState.Loaded -> when {
                lyrics.lyrics.isEmpty -> Text(
                    stringResource(R.string.player_lyrics_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.xl),
                )

                lyrics.lyrics.isSynced -> SyncedLyrics(lyrics, positionMs, onSeek)

                else -> Text(
                    lyrics.lyrics.plain,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun SyncedLyrics(lyrics: LyricsState.Loaded, positionMs: Long, onSeek: (Long) -> Unit) {
    val lines = lyrics.lyrics.synced
    val active = LrcParser.activeIndex(lines, positionMs)
    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
    }
    LazyColumn(state = listState, modifier = Modifier.heightIn(max = 520.dp)) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == active
            Text(
                line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            )
        }
    }
}

/** The queue: tap a row to play it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: PlayerState, onSkipTo: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle(stringResource(R.string.player_queue))
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex.coerceAtLeast(0))
        LazyColumn(state = listState, modifier = Modifier.heightIn(max = 560.dp)) {
            itemsIndexed(state.queue, key = { index, entry -> "$index:${entry.trackId}" }) { index, entry ->
                val current = index == state.currentIndex
                ListItem(
                    headlineContent = {
                        Text(
                            entry.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (current) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        )
                    },
                    supportingContent = { Text(entry.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = if (current) {
                        { PlayingBars(animate = state.isPlaying) }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSkipTo(index) },
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm).semantics { heading() },
    )
}
