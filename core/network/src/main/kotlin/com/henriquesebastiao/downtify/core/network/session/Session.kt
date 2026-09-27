package com.henriquesebastiao.downtify.core.network.session

/** The server this phone is paired with, and its device token. */
data class Session(
    /** No trailing slash; may have a path (a reverse proxy). */
    val baseUrl: String,
    val serverId: String,
    val serverName: String,
    val deviceId: String,
    val token: String,
) {
    override fun toString(): String = "Session(baseUrl=$baseUrl, serverId=$serverId, deviceId=$deviceId, token=***)"
}

/** Why the app is back at the connect screen. */
enum class SignOutReason {
    /** The user unpaired or changed server. */
    User,

    /** The server answered 401 to our token, or closed the socket with 4401. */
    Revoked,
}
