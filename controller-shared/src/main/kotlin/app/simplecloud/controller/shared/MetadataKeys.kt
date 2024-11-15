package app.simplecloud.controller.shared

import com.nimbusds.jwt.JWTClaimsSet
import io.grpc.Context
import io.grpc.Metadata

object MetadataKeys {

    val AUTH_SECRET_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    val CLAIMS = Context.key<JWTClaimsSet>("claims")

}