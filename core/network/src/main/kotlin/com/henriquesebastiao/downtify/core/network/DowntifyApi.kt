package com.henriquesebastiao.downtify.core.network

import com.henriquesebastiao.downtify.core.network.dto.AuthStatusDto
import com.henriquesebastiao.downtify.core.network.dto.LibraryPageDto
import com.henriquesebastiao.downtify.core.network.dto.LikeRequest
import com.henriquesebastiao.downtify.core.network.dto.LikeResponse
import com.henriquesebastiao.downtify.core.network.dto.LikesDto
import com.henriquesebastiao.downtify.core.network.dto.ListenRequest
import com.henriquesebastiao.downtify.core.network.dto.LyricsDto
import com.henriquesebastiao.downtify.core.network.dto.MeResponse
import com.henriquesebastiao.downtify.core.network.dto.PairRequest
import com.henriquesebastiao.downtify.core.network.dto.PairResponse
import com.henriquesebastiao.downtify.core.network.dto.PlaybackActivityRequest
import com.henriquesebastiao.downtify.core.network.dto.PlaylistDto
import com.henriquesebastiao.downtify.core.network.dto.PreviewResponse
import com.henriquesebastiao.downtify.core.network.dto.ServerInfoDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The server routes this app uses — only those in the mobile client contract
 * (`~/git/downtify/docs/mobile-client-contract.md`). Paths are relative so a
 * base URL with a path (a reverse proxy) works.
 */
interface DowntifyApi {
    @GET("api/server/info")
    suspend fun serverInfo(): ServerInfoDto

    @GET("api/auth/status")
    suspend fun authStatus(): AuthStatusDto

    @POST("api/auth/pair")
    suspend fun pair(@Body body: PairRequest): PairResponse

    @GET("api/v1/library")
    suspend fun library(
        @Query("since") since: Long,
        @Query("refresh") refresh: Boolean? = null,
    ): Response<LibraryPageDto>

    @GET("api/v1/playlists")
    suspend fun playlists(): List<PlaylistDto>

    @GET("api/v1/likes")
    suspend fun likes(): LikesDto

    @PUT("api/v1/likes")
    suspend fun setLike(@Body body: LikeRequest): LikeResponse

    @GET("api/v1/tracks/{id}/lyrics")
    suspend fun lyrics(@Path("id") trackId: String): LyricsDto

    @GET("api/songs/search")
    suspend fun searchSongs(@Query("query") query: String): JsonArray

    /** Undocumented in the API reference, but in the server's code and allowed to devices. */
    @GET("api/albums/search")
    suspend fun searchAlbums(@Query("query") query: String, @Query("limit") limit: Int): JsonArray

    @GET("api/url/resolve")
    suspend fun resolve(@Query("url") url: String): JsonObject

    @GET("api/preview")
    suspend fun preview(
        @Query("artist") artist: String,
        @Query("title") title: String,
        @Query("duration") duration: Int?,
    ): PreviewResponse

    @POST("api/download/batch")
    suspend fun downloadBatch(@Body body: JsonObject): Response<Unit>

    @GET("api/queue")
    suspend fun queue(): JsonArray

    @GET("api/me")
    suspend fun me(): MeResponse

    @POST("api/activity/playback")
    suspend fun reportPlayback(@Body body: PlaybackActivityRequest): Response<Unit>

    @POST("api/discover/listens")
    suspend fun reportListen(@Body body: ListenRequest): Response<Unit>
}
