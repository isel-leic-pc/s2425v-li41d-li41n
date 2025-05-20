package pt.isel.pc.sketches.coroutines.leic41d

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SimpleSemaphore(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private val mutex = Mutex()
    private var units = initialUnits
    private val continuations = mutableListOf<Continuation<Unit>>()

    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            units -= 1
            mutex.unlock()
            return
        }
        // wait-path
        suspendCoroutine { continuation ->
            continuations.addLast(continuation)
            mutex.unlock()
        }
        // nothing else to do
    }

    suspend fun release() {
        val maybeContinuation: Continuation<Unit>? =
            mutex.withLock {
                if (continuations.isNotEmpty()) {
                    val headContinuation = continuations.removeFirst()
                    headContinuation
                } else {
                    units += 1
                    null
                }
            }
        maybeContinuation?.resume(Unit)
    }
}
