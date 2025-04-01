package pt.isel.pc.synchronizers41n

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class SimpleMessageQueue<T> {
    private data class PutRequest<T>(
        val condition: Condition,
        // input
        val message: T,
        var isDone: Boolean = false,
    )

    private data class RemoveRequest<T>(
        val condition: Condition,
        // output
        var message: T? = null,
    )

    private val mutex = ReentrantLock()
    private val putRequests = mutableListOf<PutRequest<T>>()
    private val removeRequests = mutableListOf<RemoveRequest<T>>()

    @Throws(InterruptedException::class)
    fun put(
        message: T,
        timeout: Long,
        timeoutUnits: TimeUnit,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (removeRequests.isNotEmpty()) {
                val headRemoveRequest = removeRequests.removeFirst()
                headRemoveRequest.message = message
                headRemoveRequest.condition.signal()
                return true
            }
            // wait-path
            val selfRequest = PutRequest(mutex.newCondition(), message)
            var remainingNanos = timeoutUnits.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    putRequests.remove(selfRequest)
                    throw ex
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    putRequests.remove(selfRequest)
                    return false
                }
            }
        }
    }
}
