package pt.isel.pc.synchronizers

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Unary semaphore providing fairness on unit acquisition via a FIFO queue.
 * Developed in the lectures.
 */
class FairSemaphore(
    initialUnits: Int,
) {
    private val mutex = ReentrantLock()
    private val queue = mutableListOf<Condition>()
    private var units = initialUnits

    fun acquire() =
        mutex.withLock {
            // fast-path (no waiting is needed)
            if (units > 0 && queue.isEmpty()) {
                units -= 1
                return
            }
            // wait-path
            val condition = mutex.newCondition()
            queue.addLast(condition)
            while (!(units > 0 && condition == queue.first())) {
                condition.await()
            }
            // It is true that: units > 0 && first_in_queue
            queue.removeFirst()
            units -= 1
            if (units > 0 && queue.isNotEmpty()) {
                queue.first().signal()
            }
        }

    fun release() =
        mutex.withLock {
            units += 1
            val condition = queue.firstOrNull()
            if (condition != null) {
                condition.signal()
            }
        }
}
