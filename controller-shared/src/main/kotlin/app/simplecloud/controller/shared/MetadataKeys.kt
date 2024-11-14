package app.simplecloud.controller.shared

import io.grpc.Metadata

object MetadataKeys {

    val AUTH_SECRET_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    val SCOPES = Metadata.Key.of("Scopes", Metadata.ASCII_STRING_MARSHALLER)

}