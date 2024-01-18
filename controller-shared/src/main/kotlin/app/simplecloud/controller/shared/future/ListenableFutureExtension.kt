package app.simplecloud.controller.shared.future

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CompletableFuture

fun <T> ListenableFuture<T>.toCompletable(): CompletableFuture<T> {
    return ListenableFutureAdapter.toCompletable(this)
}