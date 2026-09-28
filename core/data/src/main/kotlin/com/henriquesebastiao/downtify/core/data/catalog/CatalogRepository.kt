package com.henriquesebastiao.downtify.core.data.catalog

import android.util.Log
import com.henriquesebastiao.downtify.core.model.RemoteAlbum
import com.henriquesebastiao.downtify.core.model.RemoteSong
import com.henriquesebastiao.downtify.core.model.ResolvedLink
import com.henriquesebastiao.downtify.core.network.ApiFactory
import com.henriquesebastiao.downtify.core.network.CatalogJson
import com.henriquesebastiao.downtify.core.network.DowntifyApi
import com.henriquesebastiao.downtify.core.network.session.SessionStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** What the server's search found that isn't in the library. */
data class CatalogSearch(val songs: List<RemoteSong>, val albums: List<RemoteAlbum>)

sealed interface CatalogResult<out T> {
    data class Ok<T>(val value: T) : CatalogResult<T>

    /** The server can't be reached (offline, another network). */
    data object Unreachable : CatalogResult<Nothing>

    /** It answered with an error: [status] 400 for an unsupported link, 404 for nothing there, 502 upstream. */
    data class Failed(val status: Int?) : CatalogResult<Nothing>
}

/**
 * The server's own search (YouTube Music, pasted Spotify and YouTube links),
 * 30-second previews, and asking the server to download what it found.
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val sessions: SessionStore,
    private val apis: ApiFactory,
    private val queue: ServerQueueRepository,
) {
    private val requested = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    /** Albums (by id or link) this phone asked for, with their songs' ids — for "Downloading 4/9". */
    val requestedAlbums: StateFlow<Map<String, List<String>>> = requested.asStateFlow()

    suspend fun search(query: String): CatalogResult<CatalogSearch> = call { api ->
        coroutineScope {
            // Albums are extra: their search may be off for this user, or fail alone.
            val albums = async { runCatching { CatalogJson.albums(api.searchAlbums(query, ALBUM_LIMIT)) }.getOrNull() }
            val songs = CatalogJson.songs(api.searchSongs(query))
            CatalogSearch(songs, albums.await().orEmpty())
        }
    }

    suspend fun resolve(url: String): CatalogResult<ResolvedLink> = call { api ->
        CatalogJson.resolved(url, api.resolve(url))
    }

    /** A playable 30-second clip for [song], or null when there's none (or no way to ask). */
    suspend fun previewUrl(song: RemoteSong): String? {
        if (song.previewUrl.isNotBlank()) return song.previewUrl
        val artist = song.artists.firstOrNull() ?: return null
        return when (
            val result = call { api ->
                api.preview(artist, song.title, song.durationSeconds.takeIf { it > 0 }).previewUrl
            }
        ) {
            is CatalogResult.Ok -> result.value.ifBlank { null }
            else -> null
        }
    }

    /** Queues [songs] on the server; a [playlistUrl] keeps them together as that playlist. */
    suspend fun download(songs: List<RemoteSong>, playlistUrl: String? = null): CatalogResult<Unit> {
        if (songs.isEmpty()) return CatalogResult.Ok(Unit)
        val result = call { api ->
            val response = api.downloadBatch(CatalogJson.batch(songs, playlistUrl))
            if (!response.isSuccessful) throw HttpException(response)
        }
        if (result is CatalogResult.Ok) queue.refresh()
        return result
    }

    /** Resolves [album]'s tracks and queues them all. */
    suspend fun downloadAlbum(album: RemoteAlbum): CatalogResult<Unit> = when (val resolved = resolve(album.url)) {
        is CatalogResult.Ok -> {
            val tracks = resolved.value.tracks
            requested.update { it + (album.id to tracks.map { t -> t.id }) }
            download(tracks)
        }

        is CatalogResult.Failed -> CatalogResult.Failed(resolved.status)

        CatalogResult.Unreachable -> CatalogResult.Unreachable
    }

    private suspend fun <T> call(block: suspend (DowntifyApi) -> T): CatalogResult<T> {
        val session = sessions.current ?: return CatalogResult.Unreachable
        return try {
            CatalogResult.Ok(block(apis.create(session.baseUrl)))
        } catch (e: HttpException) {
            Log.w(TAG, "Server answered ${e.code()}")
            CatalogResult.Failed(e.code())
        } catch (e: SerializationException) {
            Log.w(TAG, "Unexpected answer", e)
            CatalogResult.Failed(null)
        } catch (e: IOException) {
            Log.d(TAG, "Server unreachable", e)
            CatalogResult.Unreachable
        }
    }

    private companion object {
        const val TAG = "Catalog"
        const val ALBUM_LIMIT = 10
    }
}
