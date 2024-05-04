package app.simplecloud.controller.shared.auth

import app.simplecloud.controller.shared.MetadataKeys
import io.grpc.*

class AuthSecretInterceptor(
    private val secretKey: String
) : ServerInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val secretKey = headers.get(MetadataKeys.AUTH_SECRET_KEY)
        if (this.secretKey != secretKey) {
            call.close(Status.UNAUTHENTICATED, headers)
            return object : ServerCall.Listener<ReqT>() {}
        }

        return Contexts.interceptCall(Context.current(), call, headers, next)
    }

}