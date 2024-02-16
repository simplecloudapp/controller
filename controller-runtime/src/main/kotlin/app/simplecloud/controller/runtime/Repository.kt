package app.simplecloud.controller.runtime

abstract class Repository<T> : ArrayList<T>() {
    abstract fun load()
}