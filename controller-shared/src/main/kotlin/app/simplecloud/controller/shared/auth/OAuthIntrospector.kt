package app.simplecloud.controller.shared.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class OAuthIntrospector(private val issuer: String) {
    private val client = HttpClient()
    private val gson = Gson()

    /**
     * @return list of all scopes issued to this token, or null if the token does not exist
     */
    suspend fun introspect(token: String): List<String>? {
        try {
            val response = client.submitForm(
                url = "$issuer/oauth/introspect",
                formParameters = parameters {
                    append("token", token)
                }
            )
            val body = gson.fromJson(response.bodyAsText(), JsonObject::class.java)
            return if (!response.status.isSuccess() || !body["active"].asBoolean) {
                null
            } else {
                Scope.fromString(body["scope"].asString)
            }
        }catch (e: Exception) {
            return null
        }
    }
}