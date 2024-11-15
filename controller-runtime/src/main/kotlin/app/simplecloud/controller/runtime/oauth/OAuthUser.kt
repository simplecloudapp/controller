package app.simplecloud.controller.runtime.oauth

data class OAuthUser(
    val groups: List<OAuthGroup> = emptyList(),
    val scopes: List<String> = emptyList(),
    val userId: String,
    val username: String,
    val hashedPassword: String,
    val token: OAuthToken? = null,
)