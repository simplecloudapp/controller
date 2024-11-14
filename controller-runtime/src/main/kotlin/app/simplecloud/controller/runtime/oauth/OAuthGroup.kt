package app.simplecloud.controller.runtime.oauth

data class OAuthGroup(
    val scopes: List<String> = emptyList(),
    val name: String,
)