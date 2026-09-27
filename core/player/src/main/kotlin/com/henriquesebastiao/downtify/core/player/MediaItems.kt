package com.henriquesebastiao.downtify.core.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.henriquesebastiao.downtify.core.model.PlaybackContext
import com.henriquesebastiao.downtify.core.model.PlaybackContextType
import com.henriquesebastiao.downtify.core.model.Track
import com.henriquesebastiao.downtify.core.network.ServerUrls

/** Building Media3 items for library tracks. */
object MediaItems {
    /** Resolved to the real stream URL (and quality) only when the player opens it: see [StreamResolver]. */
    const val SCHEME = "downtify"
    private const val HOST = "track"
    private const val EXTRA_CONTEXT_TYPE = "downtify.context_type"
    private const val EXTRA_CONTEXT_ID = "downtify.context_id"

    fun uriFor(trackId: String): Uri = Uri.Builder().scheme(SCHEME).authority(HOST).appendPath(trackId).build()

    fun trackIdOf(uri: Uri): String? = if (uri.scheme == SCHEME && uri.authority == HOST) uri.lastPathSegment else null

    fun from(track: Track, baseUrl: String?): MediaItem = MediaItem.Builder()
        .setMediaId(track.id)
        .setUri(uriFor(track.id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.displayTitle)
                .setArtist(track.displayArtist)
                .setAlbumTitle(track.album)
                .setAlbumArtist(track.albumArtist)
                .setTrackNumber(track.trackNumber.takeIf { it > 0 })
                .setDurationMs((track.duration * 1000).toLong())
                .setArtworkUri(
                    if (track.hasCover && baseUrl != null) {
                        Uri.parse(ServerUrls.cover(baseUrl, track.id, ServerUrls.COVER_LARGE))
                    } else {
                        null
                    },
                )
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()

    fun contextMetadata(context: PlaybackContext): MediaMetadata = MediaMetadata.Builder()
        .setTitle(context.title)
        .setExtras(
            Bundle().apply {
                putString(EXTRA_CONTEXT_TYPE, context.type.name)
                putString(EXTRA_CONTEXT_ID, context.refId)
            },
        )
        .build()

    fun contextOf(metadata: MediaMetadata): PlaybackContext? {
        val extras = metadata.extras ?: return null
        val type =
            PlaybackContextType.entries.firstOrNull { it.name == extras.getString(EXTRA_CONTEXT_TYPE) } ?: return null
        return PlaybackContext(type, extras.getString(EXTRA_CONTEXT_ID).orEmpty(), metadata.title?.toString().orEmpty())
    }
}
