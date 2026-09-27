package com.henriquesebastiao.downtify.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.downtify.core.data.library.LibraryRepository
import com.henriquesebastiao.downtify.core.data.library.LibrarySnapshot
import com.henriquesebastiao.downtify.core.model.Album
import com.henriquesebastiao.downtify.core.model.Artist
import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.PlaybackContextType
import com.henriquesebastiao.downtify.core.model.Playlist
import com.henriquesebastiao.downtify.core.model.TextSearch
import com.henriquesebastiao.downtify.core.model.Track
import com.henriquesebastiao.downtify.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

enum class SearchFilter { All, Songs, Albums, Artists, Playlists }

data class SearchResults(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Track> = emptyList(),
) {
    val isEmpty: Boolean get() = albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && songs.isEmpty()
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val results: SearchResults = SearchResults(),
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
)

/** Searches the synced library on the phone (server search comes in a later phase). */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    library: LibraryRepository,
    private val player: PlayerController,
) : ViewModel() {
    private val query = savedState.getStateFlow(KEY_QUERY, "")
    private val filter = savedState.getStateFlow(KEY_FILTER, SearchFilter.All.name)

    val uiState: StateFlow<SearchUiState> = combine(
        query.debounce(DEBOUNCE_MS),
        filter,
        library.library,
        library.playlists,
        player.nowPlaying,
    ) { q, f, snapshot, playlists, playing ->
        val selected = SearchFilter.entries.firstOrNull { it.name == f } ?: SearchFilter.All
        SearchUiState(
            query = q,
            filter = selected,
            results = if (q.isBlank() ||
                snapshot == null
            ) {
                SearchResults()
            } else {
                search(q, selected, snapshot, playlists)
            },
            currentTrackId = playing.trackId,
            isPlaying = playing.isPlaying,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState())

    /** The text field reads this directly (not debounced) so typing stays instant. */
    val queryText: StateFlow<String> = query

    fun onQueryChange(value: String) {
        savedState[KEY_QUERY] = value
    }

    fun onFilterChange(value: SearchFilter) {
        savedState[KEY_FILTER] = value.name
    }

    fun playSong(index: Int) {
        player.play(uiState.value.results.songs, index, PlaybackContext(PlaybackContextType.Songs, "", ""))
    }

    private fun search(
        q: String,
        filter: SearchFilter,
        snapshot: LibrarySnapshot,
        playlists: List<Playlist>,
    ): SearchResults {
        fun limit(kind: SearchFilter, all: Int) = when (filter) {
            SearchFilter.All -> all
            kind -> MAX_RESULTS
            else -> 0
        }
        val albums = snapshot.albums.asSequence().filter { TextSearch.matches(q, it.title, it.artist) }
            .take(limit(SearchFilter.Albums, ALL_SECTION)).toList()
        val artists = snapshot.artists.asSequence().filter { TextSearch.matches(q, it.name) }
            .take(limit(SearchFilter.Artists, ALL_SECTION)).toList()
        val lists = playlists.asSequence().filter { !it.liked && TextSearch.matches(q, it.name) }
            .take(limit(SearchFilter.Playlists, ALL_SECTION)).toList()
        val songs = snapshot.tracks.asSequence().filter { TextSearch.matches(q, it.displayTitle, it.artist, it.album) }
            .take(limit(SearchFilter.Songs, ALL_SONGS)).toList()
        return SearchResults(albums, artists, lists, songs)
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
        const val DEBOUNCE_MS = 150L
        const val STOP_TIMEOUT_MS = 5_000L
        const val ALL_SECTION = 3
        const val ALL_SONGS = 30
        const val MAX_RESULTS = 200
    }
}
