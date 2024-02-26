# SimpleCloud v3 Controller
Process that (automatically) manages minecraft server deployments (across multiple root-servers).
At least one [ServerHost](#serverhosts) is needed to actually start servers.
> You can take a look at the [controller structure](structure.png) to learn how exactly it works

## Features
- [x] Reconciler (auto-deploying for servers)
- [x] [API](#api-usage) using [gRPC](https://grpc.io/)
- [x] Server [SQL](https://en.wikipedia.org/wiki/SQL)-Database (any dialect)

## ServerHosts
ServerHosts are processes, that directly handle minecraft server deployments. Each root-server should have exactly one ServerHost online. We provide a [default implementation](),
however, you can write your [own implementation](). To enable the Controller to communicate with ServerHosts, you need to add the ServerHost to the controllers' configuration. You can have as many ServerHost instances as you like.

### Example hosts.yml
````yaml
items:
  # ServerHost on same machine
  - id: 'my-local-server-host-1'
    ip: 127.0.0.1
    port: 12345
  # ServerHost on different machine
  - id: 'my-external-server-host-1'
    ip: 123.23.12.1
    port: 12344
````

## API usage
The SimpleCloud v3 Controller provides API for both server groups and actual servers.
The group API is used for [CRUD-Operations](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) of server groups, whereas the server API is used to manage running servers or starting new ones.
````kotlin
//Getting the Server API
val serverApi = Controller.serverApi

//Getting the Group API
val groupApi = Controller.groupApi
````

### Group API functions
> Group update functionality is yet to be implemented
````kotlin
    /**
     * @param name the name of the group.
     * @return a [CompletableFuture] with the [Group].
     */
    fun getGroupByName(name: String): CompletableFuture<Group>

    /**
     * @param name the name of the group.
     * @return a status [ApiResponse] of the delete state.
     */
    fun deleteGroup(name: String): CompletableFuture<ApiResponse>

    /**
     * @param group the [Group] to create.
     * @return a status [ApiResponse] of the creation state.
     */
    fun createGroup(group: Group): CompletableFuture<ApiResponse>
````

### Server API functions
````kotlin
    /**
     * @param id the id of the server.
     * @return a [CompletableFuture] with the [Server].
     */
    fun getServerById(id: String): CompletableFuture<Server>

    /**
     * @param groupName the name of the server group.
     * @return a [CompletableFuture] with a [List] of [Server]s of that group.
     */
    fun getServersByGroup(groupName: String): CompletableFuture<List<Server>>

    /**
     * @param group The server group.
     * @return a [CompletableFuture] with a [List] of [Server]s of that group.
     */
    fun getServersByGroup(group: Group): CompletableFuture<List<Server>>

    /**
     * @param groupName the group name of the group the new server should be of.
     * @return a [CompletableFuture] with a [Server] or null.
     */
    fun startServer(groupName: String): CompletableFuture<Server?>

    /**
     * @param id the id of the server.
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun stopServer(id: String): CompletableFuture<ApiResponse>
````

