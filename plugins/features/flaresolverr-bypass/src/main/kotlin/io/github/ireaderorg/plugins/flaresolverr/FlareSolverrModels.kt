package io.github.ireaderorg.plugins.flaresolverr

import kotlinx.serialization.Serializable

@Serializable
data class FlareSolverrRequest(
    val cmd: String,
    val url: String,
    val maxTimeout: Int = 60000,
    val postData: String? = null,
    val returnOnlyCookies: Boolean = false
)

@Serializable
data class FlareSolverrResponse(
    val status: String,
    val message: String = "",
    val solution: FlareSolverrSolution? = null
)

@Serializable
data class FlareSolverrSolution(
    val url: String = "",
    val status: Int? = null,
    val response: String? = null,
    val cookies: List<FlareSolverrCookie>? = null,
    val userAgent: String? = null
)

@Serializable
data class FlareSolverrCookie(
    val name: String,
    val value: String,
    val domain: String = "",
    val path: String = "/",
    val expiry: Double? = null,
    val expires: Double? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false
)
