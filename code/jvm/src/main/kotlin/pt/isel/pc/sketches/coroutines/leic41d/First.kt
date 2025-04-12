package pt.isel.pc.sketches.coroutines.leic41d

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("first")

suspend fun f(input: Int): String {
    logger.info("Inside f, before suspendCoroutine")
    suspendCoroutine<Unit> { continuation ->
        logger.info("Inside f, inside suspendCoroutine block")
        savedContinuation = continuation
        continuation.resumeWith(Result.success(Unit))
        //
    }
    logger.info("Inside f, after suspendCoroutine")
    // throw Exception("Ooops")
    return input.toString()
}

val someContinuation =
    object : Continuation<String> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<String>) {
            logger.info("someContinuation called with {}", result)
        }
    }

var savedContinuation: Continuation<Unit>? = null

fun main2() {
    val g = ::f as (Int, Continuation<String>) -> Unit
    logger.info("Inside main, before call to g")
    g(42, someContinuation)
    logger.info("Inside main, after call to g")
    savedContinuation?.resumeWith(Result.success(Unit))
    logger.info("Inside main, after savedContinuation?.resumeWith")
}

fun main() {
    logger.info("Inside main, before call to f")
    suspend { f(42) }.startCoroutine(someContinuation)
    logger.info("Inside main, after call to f")
    savedContinuation?.resumeWith(Result.success(Unit))
    logger.info("Inside main, after savedContinuation?.resumeWith")
}

/*
  "Inside main, before call to g"
  "Inside f, before suspendCoroutine"
  "Inside f, inside suspendCoroutine block"
  "Inside main, after call to g"
  "Inside f, after suspendCoroutine"
  "someContinuation called with 42"
  "Inside main, after savedContinuation?.resumeWith"
 */
