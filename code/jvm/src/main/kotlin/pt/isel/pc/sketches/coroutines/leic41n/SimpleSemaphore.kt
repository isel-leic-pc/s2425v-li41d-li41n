package pt.isel.pc.sketches.coroutines.leic41n

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SimpleSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits > 0)
    }

    private var units = initialUnits
    private val mutex = Mutex()
    private val acquireContinuations = mutableListOf<Continuation<Unit>>()

    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            units -= 1
            mutex.unlock()
            return
        }
        // wait-path
        suspendCoroutine<Unit> { continuation ->
            acquireContinuations.addLast(continuation)
            mutex.unlock()
        }
        // nothing more to do
    }

    suspend fun release() {
        val maybeContinuation: Continuation<Unit>? =
            mutex.withLock {
                if (acquireContinuations.isNotEmpty()) {
                    val headContinuation = acquireContinuations.removeFirst()
                    headContinuation // there is a continuation to be called after the mutual-exclusion
                } else {
                    units += 1
                    null // there is NOT a continuation to be called after the mutual-exclusion
                }
            }
        maybeContinuation?.resume(Unit)
    }
}
