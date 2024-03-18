package app.simplecloud.controller.shared.future

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool


class ListenableFutureAdapter<T>(
    val listenableFuture: ListenableFuture<T>
) {

    val completableFuture: CompletableFuture<T> = object : CompletableFuture<T>() {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            val cancelled = listenableFuture.cancel(mayInterruptIfRunning)
            super.cancel(cancelled)
            return cancelled
        }
    }

    init {
        Futures.addCallback(listenableFuture, object : FutureCallback<T> {
            override fun onSuccess(result: T) {
                completableFuture.complete(result)
            }

            override fun onFailure(ex: Throwable) {
                completableFuture.completeExceptionally(ex)
            }
        }, ForkJoinPool.commonPool())
    }

    companion object {
        fun <T> toCompletable(listenableFuture: ListenableFuture<T>): CompletableFuture<T> {
            val listenableFutureAdapter: ListenableFutureAdapter<T> = ListenableFutureAdapter<T>(listenableFuture)
            return listenableFutureAdapter.completableFuture
        }
    }

}