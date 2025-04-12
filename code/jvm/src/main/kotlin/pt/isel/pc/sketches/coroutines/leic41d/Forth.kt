package pt.isel.pc.sketches.coroutines.leic41d

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("second")

private val continuations = LinkedBlockingQueue<Continuation<Unit>>()

private val scheduleExecutor = Executors.newSingleThreadScheduledExecutor()

class ExampleInterceptorContextElement :
    AbstractCoroutineContextElement(ExampleInterceptorContextElement),
    ContinuationInterceptor {
    companion object : CoroutineContext.Key<ExampleInterceptorContextElement> {
        // can be empty
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return object : Continuation<T> {
            override val context = continuation.context

            override fun resumeWith(result: Result<T>) {
                logger.info("begin calling continuation")
                continuation.resumeWith(result)
                logger.info("end calling continuation")
            }
        }
    }
}

private suspend fun sleep(durationInMs: Long) {
    suspendCoroutine<Unit> { continuation ->
        scheduleExecutor.schedule({
            logger.info("scheduled runnable")
            continuations.put(continuation)
            // continuation.resumeWith(Result.success(Unit))
        }, durationInMs, TimeUnit.MILLISECONDS)
    }
}

private suspend fun f1() {
    val strings = listOf("Hello", "World")
    strings.forEach {
        logger.info(it)
        sleep(2000)
    }
}

private suspend fun f2() {
    val strings = listOf("Olá", "Mundo")
    strings.forEach {
        logger.info(it)
        sleep(2000)
    }
}

private suspend fun f3() {
    val strings = listOf("Buna", "Lume")
    strings.forEach {
        logger.info(it)
        sleep(2000)
    }
}

fun main() {
    val nop =
        object : Continuation<Unit> {
            override val context = ExampleInterceptorContextElement()

            override fun resumeWith(result: Result<Unit>) {
                logger.info("nop called with {}", result)
            }
        }
    ::f1.startCoroutine(nop)
    ::f2.startCoroutine(nop)
    ::f3.startCoroutine(nop)
    while (true) {
        logger.info("waiting for the next continuation")
        val nextContinuation = continuations.take()
        logger.info("running the next continuation")
        nextContinuation.resumeWith(Result.success(Unit))
    }
}
