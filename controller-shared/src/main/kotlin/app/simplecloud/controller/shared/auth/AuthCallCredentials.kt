package app.simplecloud.controller.shared.auth

import app.simplecloud.controller.shared.MetadataKeys
import io.grpc.CallCredentials
import io.grpc.Metadata
import java.util.concurrent.Executor

class AuthCallCredentials(
    private val secretKey: String
): CallCredentials() {

    override fun applyRequestMetadata(
        requestInfo: RequestInfo,
        appExecutor: Executor,
        applier: MetadataApplier
    ) {
        appExecutor.execute {
            val headers = Metadata()
            headers.put(MetadataKeys.AUTH_SECRET_KEY, secretKey)
            applier.apply(headers)
        }
    }

}