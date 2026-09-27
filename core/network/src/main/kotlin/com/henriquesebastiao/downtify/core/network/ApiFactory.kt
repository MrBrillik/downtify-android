package com.henriquesebastiao.downtify.core.network

import com.henriquesebastiao.downtify.core.network.di.DefaultClient
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds (and keeps) a [DowntifyApi] for a server base URL. */
@Singleton
class ApiFactory @Inject constructor(@DefaultClient private val client: OkHttpClient) {
    private val converter = NetworkJson.asConverterFactory("application/json".toMediaType())

    @Volatile
    private var cached: Pair<String, DowntifyApi>? = null

    fun create(baseUrl: String): DowntifyApi {
        cached?.let { (url, api) -> if (url == baseUrl) return api }
        val api = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(converter)
            .build()
            .create(DowntifyApi::class.java)
        cached = baseUrl to api
        return api
    }
}
