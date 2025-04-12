package pt.isel.pc.sketches.coroutines.leic41n

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("second")

private var savedContinuation: Continuation<Unit>? = null

private suspend fun f1() {
    logger.info("f1 started")
    f2()
    logger.info("f1 ending")
}

private suspend fun f2() {
    logger.info("f2 started")
    f3()
    logger.info("f2 after f3")
    suspendCoroutine<Unit> { continuation ->
        savedContinuation = continuation
    }
    logger.info("f2 ending")
}

private suspend fun f3() {
    logger.info("f3 started")
    suspendCoroutine<Unit> { continuation ->
        savedContinuation = continuation
    }
    logger.info("f3 ending")
}

private val nop =
    object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            logger.info("Final continuation called")
        }
    }

fun main() {
    val g = ::f1 as (Continuation<Unit>) -> Unit
    logger.info("before call to f1")
    g(nop)
    logger.info("after call to f1")

    logger.info("before call to saved continuation")
    savedContinuation?.resumeWith(Result.success(Unit))
    logger.info("after call to saved continuation")

    logger.info("before call to saved continuation")
    savedContinuation?.resumeWith(Result.success(Unit))
    logger.info("after call to saved continuation")
}
