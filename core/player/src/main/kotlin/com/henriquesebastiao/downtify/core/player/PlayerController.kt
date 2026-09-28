package com.henriquesebastiao.downtify.core.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.henriquesebastiao.downtify.core.data.di.ApplicationScope
import com.henriquesebastiao.downtify.core.data.library.LibraryRepository
import com.henriquesebastiao.downtify.core.data.library.RecentsRepository
import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.Track
import com.henriquesebastiao.downtify.core.network.session.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The UI's handle on playback: a [MediaController] connected to
 * [PlaybackService], exposed as a [StateFlow]. The activity [connect]s it on
 * start and [release]s it on stop, so the service can stop once paused.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val recents: RecentsRepository,
    private val sessions: SessionStore,
    private val resolver: StreamResolver,
    @ApplicationScope scope: CoroutineScope,
) {
    private val main = CoroutineScope(scope.coroutineContext + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val mutableState = MutableStateFlow(PlayerState())

    val state: StateFlow<PlayerState> = mutableState.asStateFlow()

    /** The playing track and context, without position ticks. */
    val nowPlaying: StateFlow<NowPlayingRef> = mutableState
        .map { NowPlayingRef(it.track?.id, it.isPlaying, it.context) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, NowPlayingRef())

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }

        override fun onPlayerError(error: PlaybackException) {
            mutableState.update { it.copy(error = classify(error)) }
        }
    }

    init {
        // Library changes (a sync) can fill in details of the playing track.
        main.launch { library.library.collect { refresh() } }
    }

    suspend fun connect(): MediaController = mutex.withLock {
        controller?.let { return it }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val built = withContext(Dispatchers.Main) { MediaController.Builder(context, token).buildAsync().await() }
        built.addListener(listener)
        controller = built
        refresh()
        built
    }

    fun release() {
        main.launch {
            mutex.withLock {
                ticker?.cancel()
                controller?.removeListener(listener)
                controller?.release()
                controller = null
            }
        }
    }

    /** Plays [tracks] from [startIndex] as the new queue, remembering where they came from. */
    fun play(tracks: List<Track>, startIndex: Int, from: PlaybackContext, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        main.launch {
            val player = connect()
            val baseUrl = sessions.current?.baseUrl
            val start = if (shuffle && startIndex == 0) tracks.indices.random() else startIndex.coerceIn(tracks.indices)
            player.setMediaItems(tracks.map { MediaItems.from(it, baseUrl) }, start, C.TIME_UNSET)
            player.shuffleModeEnabled = shuffle
            player.playlistMetadata = MediaItems.contextMetadata(from)
            player.prepare()
            player.play()
            mutableState.update { it.copy(error = null) }
            recents.record(from, tracks[start].takeIf { it.hasCover }?.id ?: tracks.firstOrNull { it.hasCover }?.id)
        }
    }

    fun togglePlayPause() = command {
        if (it.isPlaying) {
            it.pause()
        } else {
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition()
            it.play()
        }
    }

    fun next() = command { it.seekToNext() }

    fun previous() = command { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = command {
        it.seekTo(positionMs)
        mutableState.update { s -> s.copy(positionMs = positionMs) }
    }

    fun skipTo(index: Int) = command { it.seekToDefaultPosition(index) }

    fun toggleShuffle() = command { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = command {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun dismissError() = mutableState.update { it.copy(error = null) }

    private fun command(block: (MediaController) -> Unit) {
        main.launch { block(connect()) }
    }

    private fun refresh() {
        val player = controller ?: return
        val snapshot = library.library.value
        val item = player.currentMediaItem
        val track = item?.mediaId?.let { snapshot?.byId?.get(it) }
        val queue = (0 until player.mediaItemCount).map { i ->
            val entry = player.getMediaItemAt(i)
            QueueEntry(
                index = i,
                trackId = entry.mediaId,
                title = entry.mediaMetadata.title?.toString().orEmpty(),
                artist = entry.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
        mutableState.update { old ->
            old.copy(
                track = track,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.takeIf { it != C.TIME_UNSET }
                    ?: ((track?.duration ?: 0.0) * 1000).toLong(),
                shuffle = player.shuffleModeEnabled,
                repeat = when (player.repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.All
                    Player.REPEAT_MODE_ONE -> RepeatMode.One
                    else -> RepeatMode.Off
                },
                queue = queue,
                currentIndex = player.currentMediaItemIndex,
                context = MediaItems.contextOf(player.playlistMetadata),
                quality = item?.mediaId?.let(resolver::qualityOf),
                fromPhone = item?.mediaId?.let(resolver::isLocal) == true,
                error = if (old.track?.id != track?.id) null else old.error,
            )
        }
        if (player.isPlaying) startTicker() else ticker?.cancel()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = main.launch {
            while (isActive) {
                val player = controller ?: break
                mutableState.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                delay(TICK_MS)
            }
        }
    }

    private fun classify(error: PlaybackException): PlaybackError {
        val cause = error.cause
        return when {
            cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 401 ->
                PlaybackError.Unauthorized

            cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404 ->
                PlaybackError.NotFound

            error.errorCode in NETWORK_ERRORS -> PlaybackError.Network

            else -> PlaybackError.Other
        }
    }

    private companion object {
        const val TICK_MS = 500L
        val NETWORK_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )
    }
}
