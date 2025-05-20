package pt.isel.pc.sketches.coroutinesx.leic41n

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger("first")
private val executor = Executors.newSingleThreadScheduledExecutor()

private fun main() {
    runBlocking(Dispatchers.Unconfined) {
        launch {
            logger.info("hello")
            delay(2000)
            logger.info("world")
        }
        launch {
            logger.info("olá")
            delay(1000)
            logger.info("mundo")
        }
        launch {
            suspendCoroutine { cont ->
                executor.schedule({
                    logger.info("On executor callback")
                    cont.resume(Unit)
                }, 5000, TimeUnit.MILLISECONDS)
            }
            logger.info("After suspendCoroutine")
        }
    }
    logger.info("after runBlocking")
}
