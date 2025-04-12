package pt.isel.pc.sketches.coroutines.leic41n

import org.slf4j.LoggerFactory
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("first")

private var savedContinuation: Continuation<Unit>? = null

suspend fun f(input: Int): String {
    logger.info("Inside f, before suspendCoroutine")
    suspendCoroutine<Unit> { continuation ->
        logger.info("Inside f, inside suspendCoroutine")
        savedContinuation = continuation
    }
    logger.info("Inside f, after suspendCoroutine")
    return input.toString()
}

val someContinuation =
    object : Continuation<String> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<String>) {
            logger.info("someContinuation called with '{}'", result)
        }
    }

fun main() {
    val g = ::f as (input: Int, Continuation<String>) -> Unit
    logger.info("Inside main, before calling g")
    g(42, someContinuation)
    logger.info("Inside main, after calling g")
    savedContinuation?.resumeWith(Result.success(Unit))
    logger.info("After calling savedContinuation")
}
/*
  "Inside main, before calling g"
  "Inside f, before suspendCoroutine"
  "Inside f, inside suspendCoroutine"
  "Inside main, after calling g"
  "Inside f, after suspendCoroutine"
  "someContinuation called with '{}'"
  "After calling savedContinuation"
 */
