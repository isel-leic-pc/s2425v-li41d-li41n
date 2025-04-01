package pt.isel.pc.synchronizers

import java.util.concurrent.TimeUnit

/**
 * Using the semaphore object has the lock and single condition.
 * Note the `@Synchronized` method annotation and the `this.wait`.
 * This avoids using an extra object as the lock, however it exposes the lock to external code.
 */
class SemaphoreUsingImplicitMonitors2(
    initialUnits: Int,
) : Object() {
    private var units = initialUnits

    @Synchronized
    fun release() {
        units += 1
        this.notify()
    }

    @Synchronized
    fun acquire(
        timeout: Long,
        timeoutUnit: TimeUnit,
    ): Boolean {
        if (units > 0) {
            units -= 1
        }
        val deadline = System.nanoTime() + timeoutUnit.toNanos(timeout)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) {
                return false
            }
            this.wait(remaining / 1_000_000, (remaining % 1_000_000).toInt())
            if (units > 0) {
                units -= 1
                return true
            }
        }
    }
}
