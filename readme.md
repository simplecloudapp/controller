# SimpleCloud v3 Controller

Process that (automatically) manages minecraft server deployments (across multiple root-servers).
At least one [ServerHost](#serverhosts) is needed to actually start servers.
> Please visit [our documentation](https://docs.simplecloud.app/controller) to learn how exactly it works

## Features

- [x] Reconciler (auto-deploying for servers)
- [x] [API](#api-usage) using [gRPC](https://grpc.io/)
- [x] Server cache [SQL](https://en.wikipedia.org/wiki/SQL)-Database (any dialect)

## ServerHosts

ServerHosts are processes, that directly handle minecraft server deployments. Each root-server should have exactly one
ServerHost online. We provide a [default implementation](),
however, you can write your [own implementation](). You can have as many ServerHost instances as you like.

## API usage

> If you are searching for documentation, please visit our [official documentation](https://docs.simplecloud.app/api)

The SimpleCloud v3 Controller provides API for both server groups and actual servers.
The group API is used for [CRUD-Operations](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) of server
groups, whereas the server API is used to manage running servers or starting new ones.


