package app.simplecloud.controller.shared.auth

import app.simplecloud.controller.shared.MetadataKeys
import io.grpc.*
import io.ktor.client.*
import kotlinx.coroutines.runBlocking

class AuthSecretInterceptor(
    private val secretKey: String,
    authHost: String,
    authPort: Int,
) : ServerInterceptor {

    private val issuer = "http://$authHost:$authPort"

    private val masterToken = JwtHandler(secretKey, issuer).generateJwt("internal", null, "*")

    private val oAuthIntrospector = OAuthIntrospector(secretKey, issuer)

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val secretKey = headers.get(MetadataKeys.AUTH_SECRET_KEY)
        if (secretKey == null) {
            call.close(Status.UNAUTHENTICATED, headers)
            return object : ServerCall.Listener<ReqT>() {}
        }

        if (this.secretKey == secretKey) {
            val forked = Context.current().withValue(MetadataKeys.CLAIMS, masterToken.jwtClaimsSet)
            return Contexts.interceptCall(forked, call, headers, next)
        }
        return runBlocking {
            val oAuthResult = oAuthIntrospector.introspect(secretKey)
            if (oAuthResult == null) {
                call.close(Status.UNAUTHENTICATED, headers)
                return@runBlocking object : ServerCall.Listener<ReqT>() {}
            }
            val forked = Context.current().withValue(MetadataKeys.CLAIMS, oAuthResult)
            return@runBlocking Contexts.interceptCall(forked, call, headers, next)
        }

    }

}