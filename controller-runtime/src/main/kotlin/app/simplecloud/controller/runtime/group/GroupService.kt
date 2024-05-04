package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
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
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("This group does not exist")
                        .asRuntimeException()
                )
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

    override fun updateGroup(request: GroupDefinition, responseObserver: StreamObserver<GroupDefinition>) {
        val group = Group.fromDefinition(request)
        try {
            groupRepository.save(group)
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Error whilst updating group")
                    .withCause(e)
                    .asRuntimeException()
            )
            return
        }

        responseObserver.onNext(group.toDefinition())
        responseObserver.onCompleted()
    }

    override fun createGroup(request: GroupDefinition, responseObserver: StreamObserver<GroupDefinition>) {
        val group = Group.fromDefinition(request)
        try {
            groupRepository.save(group)
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Error whilst creating group")
                    .withCause(e)
                    .asRuntimeException()
            )
            return
        }

        responseObserver.onNext(group.toDefinition())
        responseObserver.onCompleted()
    }

    override fun deleteGroupByName(request: GetGroupByNameRequest, responseObserver: StreamObserver<GroupDefinition>) {
        groupRepository.find(request.name).thenApply { group ->
            if (group == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("This group does not exist")
                        .asRuntimeException()
                )
                return@thenApply
            }
            groupRepository.delete(group).thenApply thenDelete@ { successfullyDeleted ->
                if(!successfullyDeleted) {
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription("Could not delete group")
                            .asRuntimeException()
                    )

                    return@thenDelete
                }
                responseObserver.onNext(group.toDefinition())
                responseObserver.onCompleted()
            }.exceptionally {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription("Could not delete group")
                        .withCause(it)
                        .asRuntimeException()
                )
            }
        }

    }

}