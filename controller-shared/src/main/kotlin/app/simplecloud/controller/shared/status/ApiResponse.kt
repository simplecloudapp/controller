package app.simplecloud.controller.shared.status

import app.simplecloud.controller.shared.proto.StatusResponse

data class ApiResponse(
        val status: String
) {
    fun toDefinition(): StatusResponse {
        return StatusResponse.newBuilder()
                .setStatus(status)
                .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(statusResponse: StatusResponse): ApiResponse {
            return ApiResponse(
                    statusResponse.status
            )
        }
    }
}