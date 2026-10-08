package com.henriquesebastiao.downtify.core.data.similar

import com.henriquesebastiao.downtify.core.data.ServerResult
import com.henriquesebastiao.downtify.core.data.callServer
import com.henriquesebastiao.downtify.core.model.RemoteSong
import com.henriquesebastiao.downtify.core.network.ApiFactory
import com.henriquesebastiao.downtify.core.network.CatalogJson
import com.henriquesebastiao.downtify.core.network.session.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks that sound like one track (`GET /api/similar/tracks`), from
 * YouTube Music's radio mix. Needs no key; a blank artist or title asks
 * nothing and answers nothing.
 */
@Singleton
class SimilarRepository @Inject constructor(private val sessions: SessionStore, private val apis: ApiFactory) {
    suspend fun similar(artist: String, track: String): ServerResult<List<RemoteSong>> {
        if (artist.isBlank() || track.isBlank()) return ServerResult.Ok(emptyList())
        return callServer(sessions, apis, patient = true) { api ->
            CatalogJson.similarTracks(api.similarTracks(artist.trim(), track.trim(), SIMILAR_LIMIT))
        }
    }

    private companion object {
        const val SIMILAR_LIMIT = 20
    }
}
