package app.simplecloud.controller.runtime.oauth

data class OAuthToken(
    val id: String,
    val clientId: String? = null,
    val accessToken: String,
    val scope: String,
    val expiresIn: Int? = null,
    val userId: String? = null
)