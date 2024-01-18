package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.proto.ControllerGroupServiceGrpc
import app.simplecloud.controller.shared.proto.GetGroupByNameRequest
import app.simplecloud.controller.shared.proto.GetGroupByNameResponse
import io.grpc.stub.StreamObserver

class GroupService(
    private val groupRepository: GroupRepository
) : ControllerGroupServiceGrpc.ControllerGroupServiceImplBase() {

    override fun getGroupByName(
        request: GetGroupByNameRequest,
        responseObserver: StreamObserver<GetGroupByNameResponse>
    ) {
        val group = groupRepository.findGroupByName(request.name)
        val response = GetGroupByNameResponse.newBuilder()
            .setGroup(group)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}