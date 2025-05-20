package pt.isel.pc.sketches.coroutines.li41d

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.sketches.coroutines.leic41d.SimpleSemaphoreWithCancellation
import kotlin.test.Test

class SimpleSemaphoreWithCancellationTests {
    @Test
    fun first() {
        runBlocking {
            val semaphore = SimpleSemaphoreWithCancellation(0)
            val acquirer =
                launch {
                    logger.info("Acquiring unit..")
                    semaphore.acquire()
                    logger.info("Unit acquired")
                }
            delay(100)
            semaphore.release()
            acquirer.cancel()
            semaphore.mutex.lock()
            delay(100)
            semaphore.mutex.unlock()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleSemaphoreWithCancellationTests::class.java)
    }
}
