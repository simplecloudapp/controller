package app.simplecloud.controller.runtime.oauth

data class OAuthClient(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String? = null,
    val grantTypes: String,
    val scope: String? = null,
)