package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.*
import app.simplecloud.controller.shared.status.ApiResponse
import io.grpc.stub.StreamObserver

class GroupService(
    private val groupRepository: GroupRepository
) : ControllerGroupServiceGrpc.ControllerGroupServiceImplBase() {

    override fun getGroupByName(
        request: GetGroupByNameRequest,
        responseObserver: StreamObserver<GetGroupByNameResponse>
    ) {
        groupRepository.find(request.name).thenApply { group ->
            if (group == null) {
                responseObserver.onError(Exception("Group not found"))
                return@thenApply
            }

            val response = GetGroupByNameResponse.newBuilder()
                .setGroup(group.toDefinition())
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

    }

    override fun getAllGroups(request: GetAllGroupsRequest, responseObserver: StreamObserver<GetAllGroupsResponse>) {
        groupRepository.getAll().thenApply { groups ->
            val response = GetAllGroupsResponse.newBuilder()
                .addAllGroups(groups.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

    }

    override fun getGroupsByType(
        request: GetGroupsByTypeRequest,
        responseObserver: StreamObserver<GetGroupsByTypeResponse>
    ) {
        val type = request.type
        groupRepository.getAll().thenApply { groups ->
            val response = GetGroupsByTypeResponse.newBuilder()
                .addAllGroups(groups.filter { it.type == type }.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

    }

    override fun updateGroup(request: GroupDefinition, responseObserver: StreamObserver<StatusResponse>) {
        groupRepository.save(Group.fromDefinition(request))
        responseObserver.onNext(ApiResponse("success").toDefinition())
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
        groupRepository.find(request.name).thenApply { group ->
            if (group == null) {
                responseObserver.onNext(ApiResponse(status = "error").toDefinition())
                responseObserver.onCompleted()
                return@thenApply
            }
            groupRepository.delete(group).thenApply { successfullyDeleted ->
                responseObserver.onNext(ApiResponse(status = if (successfullyDeleted) "success" else "error").toDefinition())
                responseObserver.onCompleted()
            }.exceptionally {
                responseObserver.onError(it)
            }
        }

    }

}