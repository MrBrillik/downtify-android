package com.henriquesebastiao.downtify.core.player

import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.StreamQuality
import com.henriquesebastiao.downtify.core.model.Track

enum class RepeatMode { Off, All, One }

/** One queue row. */
data class QueueEntry(val index: Int, val trackId: String, val title: String, val artist: String)

/** A podcast episode playing (or loaded), as its media item describes it. */
data class PlayingEpisode(
    val episodeId: Long,
    val showId: Long,
    val title: String,
    val showName: String,
    /** The publisher's artwork: an address on their site, not the server. */
    val artworkUrl: String,
    /** The file's library path, for the activity report. */
    val file: String,
)

/** What the mini player and Now Playing show. */
data class PlayerState(
    /** The track playing, from the library (null when nothing is loaded, or an episode is). */
    val track: Track? = null,
    /** The podcast episode playing, when it isn't a song. */
    val episode: PlayingEpisode? = null,
    /** 1 for music; an episode can play faster or slower. */
    val speed: Float = 1f,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val context: PlaybackContext? = null,
    /** The quality the current track streams in, once it started. */
    val quality: StreamQuality? = null,
    /** The current track plays from its offline copy on the phone. */
    val fromPhone: Boolean = false,
    /** The last playback error, for a message; cleared by the next track. */
    val error: PlaybackError? = null,
) {
    val hasMedia: Boolean get() = track != null || episode != null
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

enum class PlaybackError { Network, Unauthorized, NotFound, Other }

/** Just what lists need to mark the playing row — changes far less often than [PlayerState]. */
data class NowPlayingRef(
    val trackId: String? = null,
    val episodeId: Long? = null,
    val isPlaying: Boolean = false,
    val context: PlaybackContext? = null,
)
