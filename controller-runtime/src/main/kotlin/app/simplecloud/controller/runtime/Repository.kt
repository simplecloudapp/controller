package app.simplecloud.controller.runtime

import java.util.concurrent.CompletableFuture

abstract class Repository<T> {
    open fun load() {}
    abstract fun save(element: T)
    abstract fun delete(element: T): CompletableFuture<Boolean>
}