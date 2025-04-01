package pt.isel.pc.synchronizers

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class SimpleMessageQueue<T> {
    private data class PutRequest<T>(
        val message: T,
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private data class RemoveRequest<T>(
        var message: T? = null,
        val condition: Condition,
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
            val selfRequest =
                PutRequest<T>(
                    message = message,
                    condition = mutex.newCondition(),
                )
            putRequests.addLast(selfRequest)
            var remainingNanos = timeoutUnits.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: Exception) {
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

    @Throws(InterruptedException::class)
    fun remove(
        timeout: Long,
        timeoutUnits: TimeUnit,
    ): T? {
        mutex.withLock {
            // fast-path
            if (putRequests.isNotEmpty()) {
                val headPutRequest = putRequests.removeFirst()
                val message = headPutRequest.message
                headPutRequest.condition.signal()
                return message
            }
            // wait-path
            val selfRequest =
                RemoveRequest<T>(
                    condition = mutex.newCondition(),
                )
            removeRequests.addLast(selfRequest)
            var remainingNanos = timeoutUnits.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: Exception) {
                    if (selfRequest.message != null) {
                        Thread.currentThread().interrupt()
                        return selfRequest.message
                    }
                    removeRequests.remove(selfRequest)
                    throw ex
                }
                if (selfRequest.message != null) {
                    return selfRequest.message
                }
                if (remainingNanos <= 0) {
                    removeRequests.remove(selfRequest)
                    return null
                }
            }
        }
    }
}
