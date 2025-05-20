package pt.isel.pc.sketches.coroutines.leic41n

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class SimpleSemaphoreWithCancellation(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    private data class AcquireRequest(
        var continuation: Continuation<Unit>? = null,
        var isDone: Boolean = false,
    )

    private var units = initialUnits
    val mutex = Mutex()
    private val acquireRequests = mutableListOf<AcquireRequest>()

    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            logger.info("Unit already available, no need wait")
            units -= 1
            mutex.unlock()
            return
        }
        // wait-path
        logger.info("Unit not available, waiting...")
        val myRequest = AcquireRequest()
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                myRequest.continuation = continuation
                acquireRequests.addLast(myRequest)
                mutex.unlock()
            }
        } catch (ex: CancellationException) {
            logger.info("Handling CancellationException")
            withContext(NonCancellable) {
                mutex.withLock {
                    if (myRequest.isDone) {
                        logger.info("Too late to give-up, unit already granted")
                        return@withLock
                    }
                    logger.info("Cancellation with unit not granted, removing request from queue")
                    acquireRequests.remove(myRequest)
                    throw ex
                }
            }
        }
        // nothing more to do
    }

    suspend fun release() {
        val maybeAcquireRequest: AcquireRequest? =
            mutex.withLock {
                if (acquireRequests.isNotEmpty()) {
                    val headRequest = acquireRequests.removeFirst()
                    headRequest.isDone = true
                    headRequest // there is a continuation to be called after the mutual-exclusion
                } else {
                    units += 1
                    null // there is NOT a continuation to be called after the mutual-exclusion
                }
            }
        maybeAcquireRequest?.continuation?.resume(Unit)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleSemaphoreWithCancellation::class.java)
    }
}
