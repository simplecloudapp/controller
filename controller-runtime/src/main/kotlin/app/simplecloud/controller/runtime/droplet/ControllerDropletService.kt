package app.simplecloud.controller.runtime.droplet

import build.buf.gen.simplecloud.controller.v1.ControllerDropletServiceGrpcKt

class ControllerDropletService(private val dropletRepository: DropletRepository) :
    ControllerDropletServiceGrpcKt.ControllerDropletServiceCoroutineImplBase() {
}