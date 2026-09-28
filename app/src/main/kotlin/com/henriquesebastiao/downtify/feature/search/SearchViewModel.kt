package com.henriquesebastiao.downtify.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.downtify.core.data.catalog.CatalogRepository
import com.henriquesebastiao.downtify.core.data.catalog.CatalogResult
import com.henriquesebastiao.downtify.core.data.catalog.ServerQueueRepository
import com.henriquesebastiao.downtify.core.data.library.LibraryRepository
import com.henriquesebastiao.downtify.core.data.library.LibrarySnapshot
import com.henriquesebastiao.downtify.core.model.Album
import com.henriquesebastiao.downtify.core.model.Artist
import com.henriquesebastiao.downtify.core.model.CatalogLinks
import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.PlaybackContextType
import com.henriquesebastiao.downtify.core.model.Playlist
import com.henriquesebastiao.downtify.core.model.RemoteAlbum
import com.henriquesebastiao.downtify.core.model.RemoteSong
import com.henriquesebastiao.downtify.core.model.ResolvedLink
import com.henriquesebastiao.downtify.core.model.ServerJob
import com.henriquesebastiao.downtify.core.model.TextSearch
import com.henriquesebastiao.downtify.core.model.Track
import com.henriquesebastiao.downtify.core.player.PlayerController
import com.henriquesebastiao.downtify.core.player.PreviewPlayer
import com.henriquesebastiao.downtify.core.player.PreviewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchFilter { All, Songs, Albums, Artists, Playlists }

data class SearchResults(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Track> = emptyList(),
) {
    val isEmpty: Boolean get() = albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && songs.isEmpty()
}

/** The server's side of the search: its catalog, or what a pasted link points at. */
sealed interface ServerResults {
    /** Nothing asked yet (a blank or one-letter query). */
    data object Idle : ServerResults
    data object Loading : ServerResults
    data object Unreachable : ServerResults

    /** [status]: 400 an unsupported link, 404 nothing there, 502 the upstream failed. */
    data class Failed(val status: Int?) : ServerResults
    data class Found(val songs: List<RemoteSong>, val albums: List<RemoteAlbum>) : ServerResults
    data class Link(val link: ResolvedLink) : ServerResults
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.All,
    val results: SearchResults = SearchResults(),
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val isLink: Boolean = false,
    val server: ServerResults = ServerResults.Idle,
    /** The server's download queue, by song id. */
    val jobs: Map<String, ServerJob> = emptyMap(),
    /** Albums asked for from here: their songs' ids. */
    val requestedAlbums: Map<String, List<String>> = emptyMap(),
    val preview: PreviewState? = null,
    /** A preview whose clip is being looked up. */
    val previewLookup: String? = null,
)

/** One-off messages for the snackbar. */
sealed interface SearchMessage {
    data class Queued(val title: String, val count: Int) : SearchMessage
    data object NoPreview : SearchMessage
    data object RequestFailed : SearchMessage
    data object Unreachable : SearchMessage
}

/**
 * Searches the library on the phone and, alongside, the server's own search
 * (YouTube Music) — or resolves a pasted Spotify / YouTube Music link — for
 * music to preview or have the server download.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    library: LibraryRepository,
    private val player: PlayerController,
    private val catalog: CatalogRepository,
    queue: ServerQueueRepository,
    private val previews: PreviewPlayer,
) : ViewModel() {
    private val query = savedState.getStateFlow(KEY_QUERY, "")
    private val filter = savedState.getStateFlow(KEY_FILTER, SearchFilter.All.name)
    private val retries = MutableStateFlow(0)
    private val previewLookup = MutableStateFlow<String?>(null)
    private val messageChannel = Channel<SearchMessage>(Channel.BUFFERED)

    val messages: Flow<SearchMessage> = messageChannel.receiveAsFlow()

    private val server: Flow<ServerResults> = combine(
        query.debounce(SERVER_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
        retries,
    ) { q, _ -> q }
        .flatMapLatest { q -> serverResults(q) }

    private val local = combine(
        query.debounce(DEBOUNCE_MS),
        filter,
        library.library,
        library.playlists,
        player.nowPlaying,
    ) { q, f, snapshot, playlists, playing ->
        val selected = SearchFilter.entries.firstOrNull { it.name == f } ?: SearchFilter.All
        val isLink = CatalogLinks.isLink(q)
        SearchUiState(
            query = q,
            filter = selected,
            results = if (q.isBlank() || isLink || snapshot == null) {
                SearchResults()
            } else {
                search(q, selected, snapshot, playlists)
            },
            currentTrackId = playing.trackId,
            isPlaying = playing.isPlaying,
            isLink = isLink,
        )
    }.flowOn(Dispatchers.Default)

    private val remote = combine(
        queue.jobs,
        catalog.requestedAlbums,
        previews.state,
        previewLookup,
    ) { jobs, albums, preview, lookup -> RemoteState(jobs, albums, preview, lookup) }

    val uiState: StateFlow<SearchUiState> = combine(local, server, remote) { l, s, r ->
        l.copy(
            server = s,
            jobs = r.jobs,
            requestedAlbums = r.albums,
            preview = r.preview,
            previewLookup = r.lookup,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState())

    private data class RemoteState(
        val jobs: Map<String, ServerJob>,
        val albums: Map<String, List<String>>,
        val preview: PreviewState?,
        val lookup: String?,
    )

    /** The text field reads this directly (not debounced) so typing stays instant. */
    val queryText: StateFlow<String> = query

    fun onQueryChange(value: String) {
        savedState[KEY_QUERY] = value
    }

    fun onFilterChange(value: SearchFilter) {
        savedState[KEY_FILTER] = value.name
    }

    fun retry() {
        retries.update { it + 1 }
    }

    fun playSong(index: Int) {
        previews.stop()
        player.play(uiState.value.results.songs, index, PlaybackContext(PlaybackContextType.Songs, "", ""))
    }

    /** Plays [song]'s 30-second clip, or stops it when it's the one playing. */
    fun togglePreview(song: RemoteSong) {
        if (uiState.value.preview?.key == song.id) {
            previews.stop()
            return
        }
        viewModelScope.launch {
            previewLookup.value = song.id
            val url = catalog.previewUrl(song)
            previewLookup.value = null
            if (url == null) messageChannel.send(SearchMessage.NoPreview) else previews.play(song.id, url)
        }
    }

    fun download(song: RemoteSong) = request(song.title, 1) { catalog.download(listOf(song)) }

    fun downloadAlbum(album: RemoteAlbum) = request(album.title, 0) { catalog.downloadAlbum(album) }

    /** Everything a pasted link points at; a playlist stays a playlist on the server. */
    fun downloadLink(link: ResolvedLink) = request(link.name, link.tracks.size) {
        catalog.download(link.tracks, playlistUrl = link.url.takeIf(CatalogLinks::isPlaylist))
    }

    private fun request(title: String, count: Int, block: suspend () -> CatalogResult<Unit>) {
        viewModelScope.launch {
            val message = when (val result = block()) {
                is CatalogResult.Ok -> SearchMessage.Queued(title, count)
                CatalogResult.Unreachable -> SearchMessage.Unreachable
                is CatalogResult.Failed -> SearchMessage.RequestFailed
            }
            messageChannel.send(message)
        }
    }

    private fun serverResults(q: String): Flow<ServerResults> = flow {
        val isLink = CatalogLinks.isLink(q)
        if (!isLink && q.length < MIN_SERVER_QUERY) {
            emit(ServerResults.Idle)
            return@flow
        }
        emit(ServerResults.Loading)
        val result = if (isLink) {
            catalog.resolve(q).map(ServerResults::Link)
        } else {
            catalog.search(q).map { ServerResults.Found(it.songs, it.albums) }
        }
        emit(result)
    }

    private inline fun <T> CatalogResult<T>.map(transform: (T) -> ServerResults): ServerResults = when (this) {
        is CatalogResult.Ok -> transform(value)
        CatalogResult.Unreachable -> ServerResults.Unreachable
        is CatalogResult.Failed -> ServerResults.Failed(status)
    }

    override fun onCleared() {
        previews.stop()
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

        /** The server's search goes out to YouTube Music: wait for a pause in the typing. */
        const val SERVER_DEBOUNCE_MS = 600L
        const val MIN_SERVER_QUERY = 2
        const val STOP_TIMEOUT_MS = 5_000L
        const val ALL_SECTION = 3
        const val ALL_SONGS = 30
        const val MAX_RESULTS = 200
    }
}
