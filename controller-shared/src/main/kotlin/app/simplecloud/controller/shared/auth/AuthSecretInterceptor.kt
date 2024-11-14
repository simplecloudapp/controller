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

    private val oAuthIntrospector = OAuthIntrospector(secretKey, "http://$authHost:$authPort")

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
            headers.put(MetadataKeys.SCOPES, "*")
            return Contexts.interceptCall(Context.current(), call, headers, next)
        }
        return runBlocking {
            val oAuthResult = oAuthIntrospector.introspect(secretKey)
            if (oAuthResult == null) {
                call.close(Status.UNAUTHENTICATED, headers)
                return@runBlocking object : ServerCall.Listener<ReqT>() {}
            }
            headers.put(MetadataKeys.SCOPES, oAuthResult.getClaim("scope").toString())
            return@runBlocking Contexts.interceptCall(Context.current(), call, headers, next)
        }

    }

}