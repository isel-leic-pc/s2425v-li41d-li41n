package pt.isel.pc.sketches.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

var sharedContinuation: Continuation<Unit>? = null
var ended: Boolean = false

suspend fun someFunction(msg: String) {
    repeat(3) {
        suspendCoroutine { continuation ->
            sharedContinuation = continuation
            println("$msg, iteration $it")
        }
    }
}

fun main() {
    @Suppress("UNCHECKED_CAST")
    val someFunctionCps = ::someFunction as (String, Continuation<Unit>) -> Any?
    someFunctionCps(
        "example",
        object : Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                ended = true
            }
        },
    )
    println("call to someFunctionCps returned")
    require(sharedContinuation != null)
    require(!ended)
    println("calling continuation")
    sharedContinuation?.resume(Unit)
}
