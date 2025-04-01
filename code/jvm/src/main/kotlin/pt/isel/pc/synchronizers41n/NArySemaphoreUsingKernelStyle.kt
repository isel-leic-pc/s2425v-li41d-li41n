package pt.isel.pc.synchronizers41n

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NArySemaphoreUsingKernelStyle(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val acquireRequests = mutableListOf<AcquireRequest>()

    private data class AcquireRequest(
        val unitsToAcquire: Int,
        val condition: Condition,
        var isDone: Boolean = false,
    )

    fun acquire(
        unitsToAcquire: Int,
        timeout: Long,
        timeoutUnits: TimeUnit,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (units >= unitsToAcquire && acquireRequests.isEmpty()) {
                units -= unitsToAcquire
                return true
            }
            // wait-path
            val selfRequest =
                AcquireRequest(
                    unitsToAcquire,
                    condition = mutex.newCondition(),
                )
            acquireRequests.addLast(selfRequest)
            var remainingNanos = timeoutUnits.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = selfRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (selfRequest.isDone) {
                        // too late to give up
                        // Re-set interrupt status to true
                        Thread.currentThread().interrupt()
                        return true
                    }
                    // give-up/cancel
                    acquireRequests.remove(selfRequest)
                    completeAllPossible()
                    throw ex
                }
                if (selfRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    // give-up/cancel
                    acquireRequests.remove(selfRequest)
                    completeAllPossible()
                    return false
                }
            }
        }
    }

    fun release(unitsToRelease: Int) {
        mutex.withLock {
            units += unitsToRelease
            completeAllPossible()
        }
    }

    private fun completeAllPossible() {
        while (true) {
            val first =
                acquireRequests.firstOrNull()
                    ?: break
            if (units < first.unitsToAcquire) {
                break
            }
            acquireRequests.removeFirst()
            units -= first.unitsToAcquire
            first.isDone = true
            first.condition.signal()
        }
    }
}
