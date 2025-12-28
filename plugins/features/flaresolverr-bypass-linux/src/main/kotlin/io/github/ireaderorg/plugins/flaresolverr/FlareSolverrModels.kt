package io.github.ireaderorg.plugins.flaresolverr

import kotlinx.serialization.Serializable

@Serializable
data class FlareSolverrRequest(
    val cmd: String,
    val url: String,
    val maxTimeout: Int = 60000,
    val postData: String? = null,
    val returnOnlyCookies: Boolean = false,
    val session: String? = null,
    val sessionTtlMinutes: Int? = null,
    val proxy: FlareSolverrProxy? = null
)

@Serializable
data class FlareSolverrProxy(
    val url: String,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class FlareSolverrResponse(
    val status: String,
    val message: String,
    val startTimestamp: Long = 0,
    val endTimestamp: Long = 0,
    val version: String = "",
    val solution: FlareSolverrSolution? = null
)

@Serializable
data class FlareSolverrSolution(
    val url: String,
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val response: String? = null,
    val cookies: List<FlareSolverrCookie> = emptyList(),
    val userAgent: String
)

@Serializable
data class FlareSolverrCookie(
    val name: String,
    val value: String,
    val domain: String? = null,
    val path: String? = null,
    val expires: Double? = null,
    val size: Int? = null,
    val httpOnly: Boolean? = null,
    val secure: Boolean? = null,
    val session: Boolean? = null,
    val sameSite: String? = null
)
