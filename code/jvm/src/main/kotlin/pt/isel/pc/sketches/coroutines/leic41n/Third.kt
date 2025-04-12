package pt.isel.pc.sketches.coroutines.leic41n

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("second")
private val continuations = mutableListOf<Continuation<Unit>>()

private suspend fun yield() {
    suspendCoroutine { continuation ->
        continuations.addFirst(continuation)
    }
}

private suspend fun f1() {
    val strings = listOf("Hello", "World", "!")
    strings.forEach {
        logger.info("{}", it)
        yield()
    }
}

private suspend fun f2() {
    val strings = listOf("Olá", "Mundo", "!")
    strings.forEach {
        logger.info("{}", it)
        yield()
    }
}

private val nop =
    object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            logger.info("Final continuation called")
        }
    }

fun main() {
    logger.info("main started")
    ::f1.startCoroutine(nop)
    ::f2.startCoroutine(nop)
    while (continuations.isNotEmpty()) {
        val headContinuation = continuations.removeFirst()
        headContinuation.resumeWith(Result.success(Unit))
    }
    logger.info("main ending")
}
