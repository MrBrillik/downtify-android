package com.henriquesebastiao.downtify.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.downtify.core.data.library.LibraryRepository
import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.PlaybackContextType
import com.henriquesebastiao.downtify.core.model.Playlist
import com.henriquesebastiao.downtify.core.model.Track
import com.henriquesebastiao.downtify.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CollectionKind { Album, Playlist, Liked }

data class CollectionUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val kind: CollectionKind = CollectionKind.Album,
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val year: String = "",
    val tracks: List<Track> = emptyList(),
    val coverTrackId: String? = null,
    /** For a playlist with its own cover on the server. */
    val playlist: Playlist? = null,
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    /** This album/playlist is what's loaded in the player. */
    val isCurrentContext: Boolean = false,
    val likedIds: Set<String> = emptySet(),
) {
    val durationSeconds: Double get() = tracks.sumOf { it.duration }
}

/** An album, a playlist or Liked songs: a header and its tracks. */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository,
    private val player: PlayerController,
) : ViewModel() {
    private val albumId: String? = savedState["albumId"]
    private val playlistName: String? = savedState["name"]
    private val kind = when {
        albumId != null -> CollectionKind.Album
        playlistName != null -> CollectionKind.Playlist
        else -> CollectionKind.Liked
    }

    val uiState: StateFlow<CollectionUiState> = combine(
        library.library,
        library.playlists,
        library.likedIds,
        player.nowPlaying,
    ) { snapshot, playlists, liked, playing ->
        if (snapshot == null) return@combine CollectionUiState(kind = kind)
        val base = CollectionUiState(
            loading = false,
            kind = kind,
            currentTrackId = playing.trackId,
            isPlaying = playing.isPlaying,
            likedIds = liked.toSet(),
        )
        val state = when (kind) {
            CollectionKind.Album -> snapshot.albumsById[albumId]?.let { album ->
                base.copy(
                    title = album.title,
                    artist = album.artist,
                    artistId = album.artistId,
                    year = album.year,
                    tracks = album.tracks,
                    coverTrackId = album.coverTrackId,
                )
            }

            CollectionKind.Playlist -> playlists.firstOrNull { it.name == playlistName }?.let { p ->
                val tracks = snapshot.tracks(p.trackIds)
                base.copy(
                    title = p.name,
                    tracks = tracks,
                    playlist = p,
                    coverTrackId = tracks.firstOrNull {
                        it.hasCover
                    }?.id,
                )
            }

            CollectionKind.Liked -> {
                val tracks = snapshot.tracks(liked)
                base.copy(tracks = tracks, coverTrackId = tracks.firstOrNull { it.hasCover }?.id)
            }
        } ?: base.copy(notFound = true)
        state.copy(isCurrentContext = playing.context == context(state.title))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CollectionUiState(kind = kind))

    fun play(index: Int, shuffle: Boolean = false) {
        val state = uiState.value
        player.play(state.tracks, index, context(state.title), shuffle)
    }

    /** The big play button: pause/resume when this is what's playing, else play from the top. */
    fun playOrPause() {
        if (uiState.value.isCurrentContext) player.togglePlayPause() else play(0)
    }

    fun shuffle() = play(0, shuffle = true)

    fun toggleLike(track: Track) {
        viewModelScope.launch { library.setLiked(track.id, track.id !in uiState.value.likedIds) }
    }

    private fun context(title: String) = when (kind) {
        CollectionKind.Album -> PlaybackContext(PlaybackContextType.Album, albumId.orEmpty(), title)
        CollectionKind.Playlist -> PlaybackContext(PlaybackContextType.Playlist, playlistName.orEmpty(), title)
        CollectionKind.Liked -> PlaybackContext(PlaybackContextType.Liked, "", title)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
