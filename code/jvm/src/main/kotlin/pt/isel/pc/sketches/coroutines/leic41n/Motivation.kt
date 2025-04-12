package pt.isel.pc.sketches.coroutines.leic41n

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("motivation")

private suspend fun f1(strings: List<String>) {
    strings.forEach {
        logger.info(it)
        delay(2000)
        // Thread.sleep(2000)
    }
}

fun main() {
    runBlocking(Dispatchers.Default) {
        logger.info("Before launch of both coroutines")
        launch {
            logger.info("before while(true)")
            while (true) {}
        }
        launch {
            f1(listOf("Hello", "World", "!"))
        }
        launch {
            f1(listOf("Olá", "Mundo", "?"))
        }
        logger.info("After launch of both coroutines")
    }
    logger.info("After runBlocking")
}
