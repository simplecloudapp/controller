package app.simplecloud.controller.runtime

interface LoadableRepository<E, I, L> : Repository<E, I> {
    fun load(): L
}