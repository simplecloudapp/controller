package app.simplecloud.controller.runtime

import java.util.concurrent.CompletableFuture


interface Repository<E, I> {
    fun delete(element: E): CompletableFuture<Boolean>
    fun save(element: E)
    fun find(identifier: I): CompletableFuture<E?>
    fun getAll(): CompletableFuture<List<E>>
}