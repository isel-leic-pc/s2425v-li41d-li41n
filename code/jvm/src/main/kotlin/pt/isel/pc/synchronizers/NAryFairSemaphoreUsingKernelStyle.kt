package pt.isel.pc.synchronizers

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

class NAryFairSemaphoreUsingKernelStyle(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val acquireRequestsQueue = mutableListOf<AcquireRequest>()

    data class AcquireRequest(
        val requestedUnits: Int,
        val condition: Condition,
        var isDone: Boolean,
    )

    @Throws(InterruptedException::class)
    fun acquire(
        requestedUnits: Int,
        timeout: Long,
        timeoutUnits: TimeUnit,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (units >= requestedUnits && acquireRequestsQueue.isEmpty()) {
                units -= requestedUnits
                return true
            }
            // wait-path
            var remainingNanos = timeoutUnits.toNanos(timeout)
            val selfRequest =
                AcquireRequest(
                    requestedUnits,
                    mutex.newCondition(),
                    false,
                )
            acquireRequestsQueue.addLast(selfRequest)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.isDone) {
                        // re-set the interrupt status to true
                        Thread.currentThread().interrupt()
                        return true
                    }
                    // give-up
                    acquireRequestsQueue.remove(selfRequest)
                    completeAllPossible()
                    throw ex
                }
                // check if already done
                if (selfRequest.isDone) {
                    return true
                }
                // is it a timeout
                if (remainingNanos <= 0) {
                    // give-up
                    acquireRequestsQueue.remove(selfRequest)
                    completeAllPossible()
                    return false
                }
            }
        }
    }

    fun release(releasedUnits: Int) =
        mutex.withLock {
            units += releasedUnits
            completeAllPossible()
        }

    private fun completeAllPossible() {
        while (true) {
            val firstRequest =
                acquireRequestsQueue.firstOrNull()
                    ?: break
            if (units < firstRequest.requestedUnits) {
                break
            }
            acquireRequestsQueue.removeFirst()
            units -= firstRequest.requestedUnits
            firstRequest.isDone = true
            firstRequest.condition.signal()
        }
    }
}
