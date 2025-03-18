package pt.isel.pc.synchronizers41n

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * Unary semaphore using Kernel Style.
 *
 * Safety properties:
 *  - units >= 0 always - invariant
 *  - An acquire with success decrements units
 *  - An acquire without success does not decrement units
 *  - A release increments units
 *  Liveliness properties:
 *  - If units > 0 we cannot have threads indefinitely waiting for units
 */
class MyFirstFairSemaphoreUsingKernelStyle(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    /**
     * Represents a request for unit acquisition
     */
    private class AcquireRequest(
        val condition: Condition,
        var isDone: Boolean = false,
    )

    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val queue = mutableListOf<AcquireRequest>()

    /**
     * Decrements the available units.
     */
    @Throws(InterruptedException::class)
    fun tryAcquire(
        timeout: Long,
        timeoutUnit: TimeUnit,
    ): Boolean {
        mutex.withLock {
            // fast-path
            if (units > 0) {
                units -= 1
                return true
            }
            // wait-path
            // unlock the mutex and make the thread not-ready
            // we know that units == 0
            val myRequest =
                AcquireRequest(
                    mutex.newCondition(),
                )
            queue.addLast(myRequest)
            var remainingNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = myRequest.condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    if (myRequest.isDone) {
                        // Too late to cancel
                        // Re-set the interrupt status
                        Thread.currentThread().interrupt()
                        return true
                    }
                    queue.remove(myRequest)
                    throw ex
                }
                if (myRequest.isDone) {
                    return true
                }
                if (remainingNanos <= 0) {
                    queue.remove(myRequest)
                    return false
                }
            }
        }
    }

    /**
     * Increments the available units
     */
    fun release() =
        mutex.withLock {
            if (queue.isNotEmpty()) {
                val first = queue.removeFirst()
                first.isDone = true
                first.condition.signal()
            } else {
                units += 1
            }
        }
}
