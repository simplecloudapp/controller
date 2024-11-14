package app.simplecloud.controller.runtime.oauth

import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import app.simplecloud.controller.shared.auth.JwtHandler
import app.simplecloud.controller.shared.auth.OAuthIntrospector
import com.fasterxml.jackson.databind.SerializationFeature
import com.nimbusds.jwt.JWTClaimsSet
import io.ktor.client.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class OAuthServer(private val args: ControllerStartCommand, database: Database) {
    private val issuer = "http://${args.grpcHost}:${args.authorizationPort}"
    private val secret = args.authSecret
    private val jwtHandler = JwtHandler(secret, issuer)
    private val pkceHandler = PKCEHandler()
    private val clientRepository = AuthClientRepository(database)
    private val tokenRepository = AuthTokenRepository(database)

    //code to client_id, code_challenge and scope (this is in memory because it is only in use temporary)
    private val flowData = mutableMapOf<String, List<String>>()

    private val authorizationHandler =
        AuthorizationHandler(secret, clientRepository, tokenRepository, pkceHandler, jwtHandler, flowData)

    private val introspector = OAuthIntrospector(secret, issuer)


    fun start() {
        embeddedServer(Netty, host = args.grpcHost, port = args.authorizationPort) {
            install(ContentNegotiation) {
                jackson {
                    enable(SerializationFeature.INDENT_OUTPUT)
                }
            }

            install(Authentication) {
                bearer {
                    authenticate { credential -> introspector.introspect(credential.token) }
                }
            }

            routing {

                // AUTHORIZATION

                // Client registration endpoint
                post("/oauth/register_client") {
                    authorizationHandler.registerClient(call)
                }
                // Authorization endpoint (simulating authorization code flow)
                post("/oauth/authorize") {
                    authorizationHandler.authorizeRequest(call)
                }
                // Token endpoint
                post("/oauth/token") {
                    authorizationHandler.tokenRequest(call)
                }
                // Revocation endpoint
                post("/oauth/revoke") {
                    authorizationHandler.revokeRequest(call)
                }
                // Introspection endpoint
                post("/oauth/introspect") {
                    authorizationHandler.introspectRequest(call)
                }

                // AUTHENTICATION

                authenticate {
                    get("/test_protection") {
                        call.respond(call.principal<JWTClaimsSet>() ?: "Claims not found")
                    }
                }
            }
        }.start(wait = true)
    }
}