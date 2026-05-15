package thread

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class RequestResolver {
    private val pool = ThreadPoolExecutor(
        8,
        8,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(1000),
        ThreadPoolExecutor.AbortPolicy()
    )
    fun execute(task: () -> Unit) {
        try {
            pool.execute(task)
        } catch (e: RejectedExecutionException) {
            throw IllegalStateException("Очередь запросов переполнена.", e)
        }
    }

    fun shutdown() {
        pool.shutdown()
    }

    fun shutdownNow() {
        pool.shutdownNow()
    }
}