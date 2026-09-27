package com.henriquesebastiao.downtify.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.component.EmptyState
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.core.network.ServerUrls
import com.henriquesebastiao.downtify.ui.common.CoverRow
import com.henriquesebastiao.downtify.ui.common.LocalCoverUrls
import com.henriquesebastiao.downtify.ui.common.PreviewData
import com.henriquesebastiao.downtify.ui.common.TrackRow
import com.henriquesebastiao.downtify.ui.common.albumSubtitle
import com.henriquesebastiao.downtify.ui.common.artistSubtitle
import com.henriquesebastiao.downtify.ui.common.songsCount

data class SearchNavigation(
    val onAlbum: (String) -> Unit = {},
    val onArtist: (String) -> Unit = {},
    val onPlaylist: (String) -> Unit = {},
)

@Composable
fun SearchRoute(
    navigation: SearchNavigation,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    SearchScreen(
        query = query,
        state = state,
        navigation = navigation,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onPlaySong = viewModel::playSong,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    state: SearchUiState,
    navigation: SearchNavigation,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onPlaySong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(painterResource(DowntifyIcons.Search), contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        painterResource(DowntifyIcons.Close),
                                        contentDescription = stringResource(R.string.search_clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            ) {}
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(SearchFilter.entries) { filter ->
                    val selected = filter == state.filter
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChange(filter) },
                        label = { Text(stringResource(filter.label)) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    painterResource(DowntifyIcons.Check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
            Results(query, state, navigation, onPlaySong)
        }
    }
}

@Composable
private fun Results(query: String, state: SearchUiState, navigation: SearchNavigation, onPlaySong: (Int) -> Unit) {
    val covers = LocalCoverUrls.current
    val results = state.results
    when {
        query.isBlank() -> EmptyState(icon = DowntifyIcons.Search, title = stringResource(R.string.search_prompt))

        results.isEmpty && state.query == query -> EmptyState(
            icon = DowntifyIcons.Search,
            title = stringResource(R.string.search_no_results, query),
        )

        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
            item {
                Text(
                    stringResource(R.string.search_in_library),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm).semantics {
                        heading()
                    },
                )
            }
            items(results.albums, key = { "album:${it.id}" }) { album ->
                CoverRow(
                    title = album.title,
                    subtitle = stringResource(
                        R.string.album_subtitle,
                        stringResource(R.string.search_kind_album),
                        albumSubtitle(album),
                    ),
                    coverUrl = covers.track(album.coverTrackId, ServerUrls.COVER_SMALL),
                    onClick = { navigation.onAlbum(album.id) },
                )
            }
            items(results.artists, key = { "artist:${it.id}" }) { artist ->
                CoverRow(
                    title = artist.name,
                    subtitle = stringResource(
                        R.string.album_subtitle,
                        stringResource(R.string.search_kind_artist),
                        artistSubtitle(artist),
                    ),
                    coverUrl = covers.track(artist.coverTrackId, ServerUrls.COVER_SMALL),
                    onClick = { navigation.onArtist(artist.id) },
                    round = true,
                    placeholderIcon = DowntifyIcons.Artist,
                )
            }
            items(results.playlists, key = { "playlist:${it.name}" }) { playlist ->
                CoverRow(
                    title = playlist.name,
                    subtitle = stringResource(
                        R.string.album_subtitle,
                        stringResource(R.string.search_kind_playlist),
                        songsCount(playlist.trackIds.size),
                    ),
                    coverUrl = covers.playlist(playlist, playlist.trackIds.firstOrNull(), ServerUrls.COVER_SMALL),
                    onClick = { navigation.onPlaylist(playlist.name) },
                    placeholderIcon = DowntifyIcons.Playlist,
                )
            }
            itemsIndexed(results.songs, key = { _, t -> "song:${t.id}" }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = { onPlaySong(index) },
                    coverUrl = covers.track(track.id.takeIf { track.hasCover }, ServerUrls.COVER_SMALL),
                    isCurrent = track.id == state.currentTrackId,
                    isPlaying = state.isPlaying,
                )
            }
        }
    }
}

private val SearchFilter.label: Int
    get() = when (this) {
        SearchFilter.All -> R.string.search_all
        SearchFilter.Songs -> R.string.library_songs
        SearchFilter.Albums -> R.string.library_albums
        SearchFilter.Artists -> R.string.library_artists
        SearchFilter.Playlists -> R.string.library_playlists
    }

@PreviewLightDark
@Composable
private fun SearchPreview() {
    DowntifyTheme {
        SearchScreen(
            query = "harbor",
            state = SearchUiState(
                query = "harbor",
                results = SearchResults(
                    albums = PreviewData.albums.take(1),
                    artists = PreviewData.artists.take(1),
                    songs = PreviewData.tracks.take(3),
                ),
            ),
            navigation = SearchNavigation(),
            onQueryChange = {},
            onFilterChange = {},
            onPlaySong = {},
        )
    }
}
