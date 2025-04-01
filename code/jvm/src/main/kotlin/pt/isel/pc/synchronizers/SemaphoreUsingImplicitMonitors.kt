package pt.isel.pc.synchronizers

import java.util.concurrent.TimeUnit

/**
 * Using an internal [Object] as the lock *and* the single condition.
 */
class SemaphoreUsingImplicitMonitors(
    initialUnits: Int,
) {
    private val mutex = Object()
    private var units = initialUnits

    fun release() {
        synchronized(mutex) {
            units += 1
            mutex.notify()
        }
    }

    fun acquire(
        timeout: Long,
        timeoutUnit: TimeUnit,
    ): Boolean {
        synchronized(mutex) {
            if (units > 0) {
                units -= 1
            }
            val deadline = System.nanoTime() + timeoutUnit.toNanos(timeout)
            while (true) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) {
                    return false
                }
                mutex.wait(remaining / 1_000_000, (remaining % 1_000_000).toInt())
                if (units > 0) {
                    units -= 1
                    return true
                }
            }
        }
    }
}
