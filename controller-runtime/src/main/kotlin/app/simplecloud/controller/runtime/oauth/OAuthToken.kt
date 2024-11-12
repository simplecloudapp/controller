package app.simplecloud.controller.runtime.oauth

data class OAuthToken(
    val id: String,
    val clientId: String,
    val accessToken: String,
    val scope: String,
    val expiresIn: Int? = null
)