package pt.isel.pc.sketches.coroutines.leic41n

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

private val logger = LoggerFactory.getLogger("second")
private val continuations = LinkedBlockingQueue<Continuation<Unit>>()

/*
 *  interface Executor
 *      fun execute(r: Runnable): Unit
 *  interface ExecutorService : Executor
 *      ...
 *      fun shutdown(...)
 *      fun awaitTermination
 *
 *  interface ScheduledExecutorService
 *      fun schedule(r: Runnable, delay: Long, ...)
 *
 *  class Executors
 *      static methods for the creation of executor services
 */
private val scheduledExecutor = Executors.newScheduledThreadPool(2)

private suspend fun sleep(duration: Duration) {
    suspendCoroutine { continuation ->
        scheduledExecutor.schedule({
            continuations.put(continuation)
        }, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }
}

private suspend fun f1() {
    val strings = listOf("Hello", "World", "!")
    strings.forEach {
        logger.info("{}", it)
        // sleep(2.seconds)
        Thread.sleep(2000)
    }
}

private suspend fun f2() {
    val strings = listOf("Olá", "Mundo", "?")
    strings.forEach {
        logger.info("{}", it)
        // sleep(2.seconds)
        Thread.sleep(2000)
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
    while (true) {
        val headContinuation = continuations.take()
        headContinuation.resumeWith(Result.success(Unit))
    }
    logger.info("main ending")
}
