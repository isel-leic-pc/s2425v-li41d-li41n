package pt.isel.pc.sketches.coroutines.li41n

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.coroutines.leic41n.SimpleSemaphoreWithCancellation
import kotlin.test.Test

class SimpleSemaphoreWithCancellationTests {
    @Test
    fun first() {
        logger.info("Test starting")
        val semaphore = SimpleSemaphoreWithCancellation(0)
        runBlocking {
            val consumer =
                launch {
                    semaphore.acquire()
                    logger.info("Consumer coroutine after acquire")
                }
            delay(100)
            semaphore.release()
            consumer.cancel()
            semaphore.mutex.lock()
            delay(100)
            semaphore.mutex.unlock()
            logger.info("Provider coroutine after release")
        }
        logger.info("Test finishing")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleSemaphoreWithCancellationTests::class.java)
    }
}
