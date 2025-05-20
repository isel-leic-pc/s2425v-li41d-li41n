package pt.isel.pc.sketches.coroutines

import kotlinx.coroutines.delay

suspend fun someSuspendFunction(input: Int): String {
    delay(100)
    return input.toString()
}
