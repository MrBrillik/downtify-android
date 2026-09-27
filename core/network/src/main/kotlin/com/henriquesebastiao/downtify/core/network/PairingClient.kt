package com.henriquesebastiao.downtify.core.network

import com.henriquesebastiao.downtify.core.network.dto.PairRequest
import com.henriquesebastiao.downtify.core.network.session.Session
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** Trades a pairing code for a device token: `POST /api/auth/pair`. */
class PairingClient @Inject constructor(private val apis: ApiFactory) {
    sealed interface Result {
        data class Paired(val session: Session) : Result

        /** 401: the code is wrong, expired or already used. */
        data object WrongCode : Result

        /** 429: too many wrong codes from this address. */
        data class TooManyAttempts(val retryAfterSeconds: Long) : Result
        data object Unreachable : Result
        data class Failed(val status: Int) : Result
    }

    /** Pairs with [baseUrl]; the code is sent as typed (the server normalises it). */
    @Suppress("TooGenericExceptionCaught") // Network boundary: every failure maps to a Result.
    suspend fun pair(baseUrl: String, code: String, deviceName: String): Result = try {
        val response = apis.create(baseUrl).pair(PairRequest(code = code.trim(), deviceName = deviceName))
        Result.Paired(
            Session(
                baseUrl = baseUrl,
                serverId = response.server.serverId,
                serverName = response.server.name,
                deviceId = response.device.id,
                token = response.token,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        when (e.httpStatus) {
            null -> Result.Unreachable
            HTTP_UNAUTHORIZED -> Result.WrongCode
            HTTP_TOO_MANY -> Result.TooManyAttempts(e.retryAfterSeconds ?: DEFAULT_RETRY_AFTER)
            else -> Result.Failed(e.httpStatus ?: 0)
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_TOO_MANY = 429
        const val DEFAULT_RETRY_AFTER = 60L
    }
}
