package com.henriquesebastiao.downtify.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.henriquesebastiao.downtify.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The preview playing now: which song, how far (0–1), and whether it's still loading. */
data class PreviewState(val key: String, val loading: Boolean, val progress: Float)

/**
 * 30-second previews of songs that aren't in the library (Spotify's or
 * Deezer's clips). A small player of its own, outside the media session, so
 * a preview never lands in the queue, the notification or the play counts.
 * The music playing is paused for it and resumed after.
 *
 * Clips come from Spotify's and Deezer's CDNs: the auth interceptor only adds
 * the device token for the paired server, so it never reaches them.
 */
@OptIn(UnstableApi::class)
@Singleton
class PreviewPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpDataSource.Factory,
    private val controller: PlayerController,
    @ApplicationScope scope: CoroutineScope,
) {
    private val main = CoroutineScope(scope.coroutineContext + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var ticker: Job? = null
    private var pausedMusic = false
    private val mutableState = MutableStateFlow<PreviewState?>(null)

    val state: StateFlow<PreviewState?> = mutableState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> mutableState.value = mutableState.value?.copy(loading = false)
                Player.STATE_ENDED -> stop()
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stop()
        }
    }

    /** Plays [url] as the preview of [key] (a song id), replacing any other preview. */
    fun play(key: String, url: String) = main.launch {
        if (!pausedMusic && controller.state.value.isPlaying) {
            controller.pause()
            pausedMusic = true
        }
        val p = player ?: build().also { player = it }
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.play()
        mutableState.value = PreviewState(key, loading = true, progress = 0f)
        ticker?.cancel()
        ticker = main.launch {
            while (isActive) {
                val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: PREVIEW_MS
                mutableState.value =
                    mutableState.value?.copy(progress = (p.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
                delay(TICK_MS)
            }
        }
    }

    /** Stops the preview and gives the music back. */
    fun stop() {
        main.launch {
            ticker?.cancel()
            player?.stop()
            mutableState.value = null
            if (pausedMusic) {
                pausedMusic = false
                controller.resume()
            }
        }
    }

    private fun build(): ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)))
        // The music player was paused by hand; taking audio focus would stop it for good.
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            false,
        )
        .build()
        .apply { addListener(listener) }

    private companion object {
        const val TICK_MS = 200L
        const val PREVIEW_MS = 30_000L
    }
}
