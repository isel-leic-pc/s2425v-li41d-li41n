package pt.isel.pc.synchronizers

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * First unary semaphore version, developed in the lectures.
 * Supports cancellation by timeout and interruption.
 * Does not provide any fairness guarantees.
 */
class OurFirstSemaphore(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private val condition: Condition = mutex.newCondition()
    private var units = initialUnits

    fun acquire() =
        mutex.withLock {
            // fast-path (no waiting is needed)
            if (units > 0) {
                units -= 1
                return
            }
            // wait-path
            // implicitly releases the lock
            // at this point we know for sure that units == 0
            // correct even with spurious wake-ups!
            while (units == 0) {
                try {
                    condition.await()
                } catch (ex: InterruptedException) {
                    // Not really needed due to
                    // https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.2.4
                    if (units > 0) {
                        condition.signal()
                    }
                    throw ex
                }
            }
            units -= 1
        }

    @Throws(InterruptedException::class)
    fun tryAcquire(
        timeout: Long,
        timeoutUnit: TimeUnit,
    ): Boolean {
        mutex.withLock {
            // fast-path (no waiting is needed)
            if (units > 0) {
                units -= 1
                return true
            }
            // wait-path
            // implicitly releases the lock
            // at this point we know for sure that units == 0
            // correct even with spurious wake-ups!
            var timeoutNanos = timeoutUnit.toNanos(timeout)
            while (true) {
                try {
                    timeoutNanos = condition.awaitNanos(timeoutNanos)
                } catch (ex: InterruptedException) {
                    // Not really needed due to
                    // https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.2.4
                    if (units > 0) {
                        condition.signal()
                    }
                    throw ex
                }
                // First check for success
                // I.e. condition to complete the function with success
                if (units > 0) {
                    units -= 1
                    return true
                }
                // Only then, check if timeout
                if (timeoutNanos <= 0) {
                    return false
                }
            }
        }
    }

    fun release() =
        mutex.withLock {
            units += 1
            // we need a way to resume one of the suspended threads
            condition.signal()
        }
}
