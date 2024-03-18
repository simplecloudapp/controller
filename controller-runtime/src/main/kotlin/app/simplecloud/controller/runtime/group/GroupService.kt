package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.status.ApiResponse
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

    override fun createGroup(request: GroupDefinition, responseObserver: StreamObserver<StatusResponse>) {
        val group = Group.fromDefinition(request)
        try {
            groupRepository.save(group)
            responseObserver.onNext(ApiResponse(status = "success").toDefinition())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
    }

    override fun deleteGroupByName(request: GetGroupByNameRequest, responseObserver: StreamObserver<StatusResponse>) {
        val groupDefinition = groupRepository.findGroupByName(request.name)
        if (groupDefinition == null) {
            responseObserver.onNext(ApiResponse(status = "error").toDefinition())
            responseObserver.onCompleted()
            return
        }
        val group = Group.fromDefinition(groupDefinition)
        try {
            groupRepository.delete(group).thenApply {
                responseObserver.onNext(ApiResponse(status = if (it) "success" else "error").toDefinition())
                responseObserver.onCompleted()
            }
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
    }

}