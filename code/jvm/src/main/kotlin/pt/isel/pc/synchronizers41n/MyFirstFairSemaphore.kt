package pt.isel.pc.synchronizers41n

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * Fair semaphore with specific notification using a queue of [Condition].
 *
 * Safety properties:
 *  - units >= 0 always - invariant
 *  - An acquire with success decrements units
 *  - An acquire without success does not decrement units
 *  - A release increments units
 *  Liveliness properties:
 *  - If units > 0 we cannot have threads indefinitely waiting for units
 */
class MyFirstFairSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private val mutex = ReentrantLock()
    private var units = initialUnits
    private val queue = mutableListOf<Condition>()

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
            if (units > 0 && queue.isEmpty()) {
                units -= 1
                return true
            }
            // wait-path
            // unlock the mutex and make the thread not-ready
            val myCondition = mutex.newCondition()
            queue.addLast(myCondition)
            var remainingNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = myCondition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    queue.remove(myCondition)
                    signalNext()
                    throw ex
                }
                if (units > 0 && queue.first() == myCondition) {
                    queue.removeFirst()
                    signalNext()
                    // units > 0? Yes!
                    units -= 1
                    return true
                }
                if (remainingNanos <= 0) {
                    queue.remove(myCondition)
                    signalNext()
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
            units += 1
            if (queue.isNotEmpty()) {
                queue.first().signal()
            }
        }

    private fun signalNext() {
        if (units > 0 && queue.isNotEmpty()) {
            queue.first().signal()
        }
    }
}
