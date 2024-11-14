package app.simplecloud.controller.shared.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class OAuthIntrospector(secret: String, private val issuer: String) {
    private val client = HttpClient()
    private val jwtHandler = JwtHandler(secret, issuer)
    private val gson = Gson()

    suspend fun introspect(token: String): JWTClaimsSet? {
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
                jwtHandler.decodeJwt(token).jwtClaimsSet
            }
        }catch (e: Exception) {
            return null
        }
    }
}