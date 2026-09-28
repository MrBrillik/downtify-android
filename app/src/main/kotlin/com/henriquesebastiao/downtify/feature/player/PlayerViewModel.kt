package com.henriquesebastiao.downtify.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henriquesebastiao.downtify.core.data.library.LibraryRepository
import com.henriquesebastiao.downtify.core.data.session.ServerRepository
import com.henriquesebastiao.downtify.core.model.Lyrics
import com.henriquesebastiao.downtify.core.model.PlaybackSpeeds
import com.henriquesebastiao.downtify.core.player.PlayerController
import com.henriquesebastiao.downtify.core.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lyrics of the current track, loaded when the lyrics sheet opens. */
sealed interface LyricsState {
    data object Loading : LyricsState
    data class Loaded(val trackId: String, val lyrics: Lyrics) : LyricsState
}

/** Shared by the mini player and Now Playing (activity scope). */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: PlayerController,
    private val library: LibraryRepository,
    server: ServerRepository,
) : ViewModel() {
    val state: StateFlow<PlayerState> = player.state

    val isLiked: StateFlow<Boolean> = combine(player.state, library.likedIds) { s, liked ->
        s.track?.id?.let { it in liked } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val serverName: StateFlow<String> = server.session.map { it?.serverName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    private val lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyrics: StateFlow<LyricsState> = lyricsState.asStateFlow()

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun skipBack() = player.skipBack()
    fun skipForward() = player.skipForward()
    fun cycleSpeed() = player.setSpeed(PlaybackSpeeds.next(state.value.speed))
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun skipTo(index: Int) = player.skipTo(index)
    fun dismissError() = player.dismissError()

    fun toggleLike() {
        val id = state.value.track?.id ?: return
        val liked = isLiked.value
        viewModelScope.launch { library.setLiked(id, !liked) }
    }

    /** Loads the lyrics of the current track (cached by the repository). */
    fun loadLyrics() {
        val id = state.value.track?.id ?: return
        val current = lyricsState.value
        if (current is LyricsState.Loaded && current.trackId == id) return
        lyricsState.value = LyricsState.Loading
        viewModelScope.launch { lyricsState.value = LyricsState.Loaded(id, library.lyrics(id)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
