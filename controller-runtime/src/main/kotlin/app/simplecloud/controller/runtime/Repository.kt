package app.simplecloud.controller.runtime

import java.util.concurrent.CompletableFuture

abstract class Repository<T> : ArrayList<T>() {
    abstract fun load()
    abstract fun save(element: T)
    abstract fun delete(element: T): CompletableFuture<Boolean>
}