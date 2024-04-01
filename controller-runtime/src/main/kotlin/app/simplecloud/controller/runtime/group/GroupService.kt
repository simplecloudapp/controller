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
        val group = groupRepository.find(request.name)
        if (group == null) {
            responseObserver.onError(Exception("Group not found"))
            return
        }

        val response = GetGroupByNameResponse.newBuilder()
            .setGroup(group.toDefinition())
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
        val group = groupRepository.find(request.name)
        if (group == null) {
            responseObserver.onNext(ApiResponse(status = "error").toDefinition())
            responseObserver.onCompleted()
            return
        }
        try {
            val successfullyDeleted = groupRepository.delete(group)
            responseObserver.onNext(ApiResponse(status = if (successfullyDeleted) "success" else "error").toDefinition())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
    }

}