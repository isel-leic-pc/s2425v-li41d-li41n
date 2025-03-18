package pt.isel.pc.synchronizers41n

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * First unary semaphore version, developed in the lectures.
 * Supports cancellation by timeout and interruption.
 * Does not provide any fairness guarantees.
 *
 * Safety properties:
 *  - units >= 0 always - invariant
 *  - An acquire with success decrements units
 *  - An acquire without success does not decrement units
 *  - A release increments units
 *  Liveliness properties:
 *  - If units > 0 we cannot have threads indefinitely waiting for units
 */
class MyFirstSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private val mutex = ReentrantLock()
    private val condition = mutex.newCondition()
    private var units = initialUnits

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
            var remainingNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    remainingNanos = condition.awaitNanos(remainingNanos)
                } catch (ex: InterruptedException) {
                    // https://docs.oracle.com/javase/specs/jls/se23/html/jls-17.html#jls-17.2.4
                    if (units > 0) {
                        condition.signal()
                    }
                    throw ex
                }
                if (units > 0) {
                    // units > 0? Yes!
                    units -= 1
                    return true
                }
                if (remainingNanos <= 0) {
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
            condition.signal()
        }
}
