package pt.isel.pc.sketches.coroutines.leic41d

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.coroutines.leic41n.SimpleSemaphoreWithCancellation
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class SimpleSemaphoreWithCancellation(
    initialUnits: Int,
) {
    init {
        require(initialUnits >= 0)
    }

    val mutex = Mutex()
    private var units = initialUnits
    private val acquireRequests = mutableListOf<AcquireRequest>()

    private data class AcquireRequest(
        val continuation: Continuation<Unit>,
        var isDone: Boolean = false,
    )

    suspend fun acquire() {
        mutex.lock()
        // fast-path
        if (units > 0) {
            units -= 1
            mutex.unlock()
            return
        }
        // wait-path
        // may throw CancellationException
        var myRequest: AcquireRequest? = null
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = AcquireRequest(continuation)
                myRequest = request
                acquireRequests.addLast(request)
                mutex.unlock()
            }
        } catch (ex: CancellationException) {
            // check if the request is done or not
            logger.info("suspend ended with exception")
            val observedRequest =
                myRequest
                    ?: throw IllegalStateException("Should not happen")
            withContext(NonCancellable) {
                mutex.withLock {
                    if (observedRequest.isDone) {
                        logger.info("too late to cancel, returning success")
                        return@withLock
                    } else {
                        logger.info("removing request and rethrowing")
                        acquireRequests.remove(observedRequest)
                        throw ex
                    }
                }
            }
        }
    }

    suspend fun release() {
        val maybeContinuation: Continuation<Unit>? =
            mutex.withLock {
                if (acquireRequests.isNotEmpty()) {
                    val headRequest = acquireRequests.removeFirst()
                    headRequest.isDone = true
                    headRequest.continuation
                } else {
                    units += 1
                    null
                }
            }
        maybeContinuation?.resume(Unit)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleSemaphoreWithCancellation::class.java)
    }
}
