package app.simplecloud.controller.runtime.reconciler

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerNumericalIdRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpc
import io.grpc.ManagedChannel

class Reconciler(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val serverHostRepository: ServerHostRepository,
    private val numericalIdRepository: ServerNumericalIdRepository,
    managedChannel: ManagedChannel,
) {

    private val serverStub = ControllerServerServiceGrpc.newFutureStub(managedChannel)

    fun reconcile() {
        this.groupRepository.findAll().forEach { group ->
            GroupReconciler(serverRepository, serverHostRepository, numericalIdRepository, serverStub, group)
                .reconcile()
        }
    }

}