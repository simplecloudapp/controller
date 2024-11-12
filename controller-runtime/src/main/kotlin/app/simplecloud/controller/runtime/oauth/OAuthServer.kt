package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.logging.log4j.LogManager
import java.util.*

class OAuthServer(private val args: ControllerStartCommand, database: Database) {
    private val issuer = "http://${args.grpcHost}:${args.authorizationPort}"
    private val secret = args.authSecret
    private val jwtHandler = JwtHandler(secret, issuer)
    private val pkceHandler = PKCEHandler()
    private val clientRepository = AuthClientRepository(database)
    private val tokenRepository = AuthTokenRepository(database)

    //code to client_id, code_challenge and scope (this is in memory because it is only in use temporary)
    private val flowData = mutableMapOf<String, List<String>>()

    fun start() {
        embeddedServer(Netty, host = args.grpcHost, port = args.authorizationPort) {
            install(ContentNegotiation) {
                jackson {
                    enable(SerializationFeature.INDENT_OUTPUT)
                }
            }

            routing {
                // Register clients
                post("/register_client") {
                    val params = call.receiveParameters()
                    val providedMasterToken = params["master_token"]
                    if (providedMasterToken != secret) {
                        call.respond(HttpStatusCode.Forbidden, "Invalid master token")
                        return@post
                    }
                    val redirectUri = params["redirect_uri"]
                    val grantTypes = params["grant_types"]
                    if (grantTypes == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid grant_types")
                        return@post
                    }
                    val scope = params["scope"]
                    val clientId = "client-${UUID.randomUUID().toString().replace("-", "").substring(0, 8)}"
                    val clientSecret = "secret-${UUID.randomUUID().toString().replace("-", "")}"
                    val client = OAuthClient(clientId, clientSecret, redirectUri, grantTypes, scope)
                    clientRepository.save(client)
                    call.respond(mapOf("client_id" to clientId, "client_secret" to clientSecret))
                }

                // Authorization endpoint (simulating authorization code flow)
                get("/authorize") {
                    val clientId = call.request.queryParameters["client_id"]
                    val redirectUri = call.request.queryParameters["redirect_uri"]
                    val challengeMethod = call.request.queryParameters["code_challenge_method"]
                    val challenge = call.request.queryParameters["code_challenge"]
                    val scope = call.request.queryParameters["scope"]
                    if (clientId == null || redirectUri == null || scope == null || challenge == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "You have to provide redirect_uri, client_id, scope and challenge"
                        )
                        return@get
                    }
                    if (challengeMethod == null || challengeMethod != "S256") {
                        call.respond(HttpStatusCode.BadRequest, "Invalid challenge, S256 is supported.")
                        return@get
                    }
                    val client = clientRepository.find(clientId)
                    if (client == null) {
                        call.respond(HttpStatusCode.NotFound, "Client not found")
                        return@get
                    }
                    if (!client.grantTypes.contains("authorization_code")) {
                        call.respond(HttpStatusCode.BadRequest, "User authorization is not supported by the client")
                        return@get
                    }

                    if (!client.grantTypes.contains("pkce")) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "User authorization using PKCE is not supported by the client"
                        )
                        return@get
                    }

                    if (client.scope != null && !client.scope.contains(scope)) {
                        call.respond(HttpStatusCode.BadRequest, "This scope is not supported by the client")
                        return@get
                    }

                    val authorizationCode = UUID.randomUUID().toString().replace("-", "")
                    flowData[authorizationCode] = listOf(client.clientId, challenge, scope)
                    call.respondRedirect("$redirectUri?code=$authorizationCode")
                }

                // Token endpoint
                post("/token") {
                    val params = call.receiveParameters()
                    val clientId = params["client_id"]
                    val clientSecret = params["client_secret"]
                    val code = params["code"]
                    val codeVerifier = params["code_verifier"]

                    if (clientId == null) {
                        call.respond(HttpStatusCode.BadRequest, "You have to provide a client id")
                        return@post
                    }

                    if (clientSecret == null) {
                        call.respond(HttpStatusCode.BadRequest, "You have to provide a client secret")
                        return@post
                    }

                    val client = clientRepository.find(clientId)
                    if (client == null) {
                        call.respond(HttpStatusCode.BadRequest, "You have to provide a valid client id")
                        return@post
                    }

                    if (client.clientSecret != clientSecret) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid client secret")
                        return@post
                    }

                    if (client.grantTypes.contains("authorization_code") && client.grantTypes.contains("pkce")) {
                        if (codeVerifier == null || code == null) {
                            call.respond(HttpStatusCode.BadRequest, "You have to provide a code and a code verifier")
                            return@post
                        }
                        val reconstructedChallenge = pkceHandler.generateCodeChallenge(codeVerifier)
                        val originalChallenge = flowData[code]?.get(1)
                        //If we can reconstruct the challenge, the authorization context was made in a secure context
                        if (originalChallenge == reconstructedChallenge) {
                            val token = OAuthToken(
                                id = UUID.randomUUID().toString(),
                                clientId = clientId,
                                accessToken = jwtHandler.generateJwt(
                                    clientId,
                                    expiresIn = 3600,
                                    scope = flowData[code]?.get(2)!!
                                ),
                                expiresIn = 3600,
                                scope = flowData[code]?.get(2)!!
                            )
                            tokenRepository.save(token)
                            call.respond(mapOf(
                                "access_token" to token.accessToken,
                                "scope" to token.scope,
                                "exp" to (token.expiresIn ?: -1),
                            ))
                            return@post
                        }
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "The token request was not made in the same context as the authorization."
                        )
                        return@post
                    } else if (client.grantTypes.contains("client_credentials")) {
                        val token = OAuthToken(
                            id = UUID.randomUUID().toString(),
                            clientId = clientId,
                            accessToken = jwtHandler.generateJwt(clientId, scope = client.scope ?: "*"),
                            scope = client.scope ?: "*"
                        )
                        tokenRepository.save(token)
                        call.respond(mapOf(
                            "access_token" to token.accessToken,
                            "scope" to token.scope,
                            "exp" to (token.expiresIn ?: -1),
                        ))
                        return@post
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid client")
                        return@post
                    }
                }

                post("/introspect") {
                    val params = call.receiveParameters()
                    val token = params["token"]
                    if (token == null) {
                        call.respond(HttpStatusCode.BadRequest, "Token is missing")
                        return@post
                    }

                    val authToken = tokenRepository.findByAccessToken(token)
                    if (authToken == null) {
                        call.respond(HttpStatusCode.OK, mapOf("active" to false))
                        return@post
                    }

                    // If the token exists, respond with token details
                    call.respond(
                        mapOf(
                            "active" to ((authToken.expiresIn ?: 1) > 0),
                            "client_id" to authToken.clientId,
                            "scope" to authToken.scope,
                            "exp" to (authToken.expiresIn ?: -1),
                        )
                    )
                }
            }
        }.start(wait = true)
    }
}